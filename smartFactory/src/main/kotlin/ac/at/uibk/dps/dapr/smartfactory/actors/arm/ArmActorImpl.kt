package ac.at.uibk.dps.dapr.smartfactory.actors.arm

import ac.at.uibk.dps.dapr.smartfactory.services.Services
import ac.at.uibk.dps.dapr.smartfactory.utils.Utils
import io.dapr.actors.ActorId
import io.dapr.actors.runtime.AbstractActor
import io.dapr.actors.runtime.ActorRuntimeContext
import io.dapr.client.DaprClientBuilder
import java.time.Duration
import reactor.core.publisher.Mono

class ArmActorImpl(runtimeContext: ActorRuntimeContext<ArmActorImpl>, id: ActorId) :
  AbstractActor(runtimeContext, id), ArmActor {

  enum class State {
    IDLE,
    ERROR,
    PICKUP,
    ASSEMBLE,
    RETURN,
    JOB_DONE,
  }

  private val partsPerProduct = 1000
  private var state = State.IDLE
  private var pickupSuccess = true
  private var errorMsg = ""
  private var partsAssembled = 0

  private val daprClient = DaprClientBuilder().build()

  private fun transition(targetState: State, data: Any? = null) {
    when (targetState) {
      State.IDLE -> {
        if (state == State.RETURN) {
          // Transition Actions
          if (partsAssembled >= partsPerProduct) {
            partsAssembled = 0
            Utils.publishEvent(
                daprClient,
                "pubsub",
                "eProductComplete",
                mutableMapOf<String, Any?>(),
              )
              .subscribe()
          }

          state = targetState
          enterIdle()
        }
      }

      State.PICKUP -> {
        if (state == State.IDLE) {
          state = targetState
          enterPickup()
        }
      }

      State.ASSEMBLE -> {
        if (state == State.PICKUP || state == State.ERROR) {
          // Transition actions
          if (state == State.PICKUP) {
            // Raise ePickedUp
            Utils.publishEvent(daprClient, "pubsub", "ePickedUp", mutableMapOf<String, Any?>())
              .subscribe()
          }

          state = targetState
          enterAssemble()
        }
      }

      State.ERROR -> {
        if (state == State.PICKUP || state == State.ASSEMBLE) {
          // Transition actions
          if (state == State.PICKUP) errorMsg = "Pickup failed..."
          if (state == State.ASSEMBLE) errorMsg = "Assemble failed..."

          state = targetState
          enterError()
        }
      }

      State.RETURN -> {
        if (state == State.ASSEMBLE || state == State.ERROR) {
          // Transition Actions
          if (state == State.ASSEMBLE) {
            partsAssembled++
            Utils.publishEvent(
                daprClient,
                "pubsub",
                "eAssemblyComplete",
                mutableMapOf<String, Any?>(),
              )
              .subscribe()
          }

          state = targetState
          enterReturn()
        }
      }

      State.JOB_DONE -> {
        state = targetState
      }
    }
  }

  override fun initialize() {
    enterIdle()
  }

  override fun initiatePickup() {
    val isJobDone =
      daprClient.getState("statestore", "isJobDone", Boolean::class.java).block()?.value ?: false

    if (!isJobDone) transition(State.PICKUP)
  }

  override fun updatePickupStatus(pickupStatus: Boolean) {
    this.pickupSuccess = pickupStatus

    if (pickupStatus) transition(State.ASSEMBLE) else transition(State.ERROR)
  }

  override fun updateAssemblyStatus(assemblyStatus: Boolean) {
    if (assemblyStatus) transition(State.RETURN) else transition(State.ERROR)
  }

  override fun markJobDone() {
    if (state == State.IDLE) transition(State.JOB_DONE)
  }

  override fun markPickedUp() {
    this.pickupSuccess = true
  }

  override fun armReset() {
    transition(State.IDLE)
  }

  private fun enterIdle() {
    if (!pickupSuccess) transition(State.PICKUP)
  }

  private fun enterPickup() {
    // Invoke arm pickup
    Services.pickUp().subscribe()
  }

  private fun enterAssemble() {
    // Invoke Assemble
    Services.assemble().subscribe()
  }

  private fun enterError() {
    // Raise eProcessMessage
    Utils.publishEvent(
        daprClient,
        "pubsub",
        "eProcessMessage",
        mutableMapOf("msg" to "Fatal robotic arm failure: $errorMsg"),
      )
      .subscribe()

    // Starting timer eRetry
    registerActorTimer(
        "eRetryTimer",
        "retryTimeout",
        0,
        Duration.ofSeconds(2),
        Duration.ofMillis(-1),
      )
      .subscribe()
  }

  private fun enterReturn() {
    // Invoke return to start action
    Services.returnToStart().subscribe()
  }

  override fun retryTimeout(): Mono<Void> {
    if (state == State.ERROR) {
      if (pickupSuccess) transition(State.ASSEMBLE) else transition(State.RETURN)
    }

    return Mono.empty()
  }
}
