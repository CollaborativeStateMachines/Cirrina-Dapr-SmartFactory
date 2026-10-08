package ac.at.uibk.dps.dapr.smartfactory.actors.belt

import ac.at.uibk.dps.dapr.smartfactory.services.Services
import ac.at.uibk.dps.dapr.smartfactory.utils.Utils
import io.dapr.actors.ActorId
import io.dapr.actors.runtime.AbstractActor
import io.dapr.actors.runtime.ActorRuntimeContext
import io.dapr.client.DaprClientBuilder
import java.time.Duration
import reactor.core.publisher.Mono

class BeltActorImpl(runtimeContext: ActorRuntimeContext<BeltActorImpl>, id: ActorId) :
  AbstractActor(runtimeContext, id), BeltActor {

  enum class State {
    LOADING,
    TRANSPORTING,
    UNLOADING,
    JOB_DONE,
  }

  private var state: State = State.LOADING

  private val daprClient = DaprClientBuilder().build()

  private fun transition(targetState: State, data: Any? = null) {
    when (targetState) {
      State.LOADING -> {
        if (state == State.UNLOADING) {
          // Exit actions
          unregisterTimer("armPickupTimeout-${id}").subscribe()

          state = targetState
        }
      }

      State.TRANSPORTING -> {
        if (state == State.LOADING) {
          state = State.TRANSPORTING
          enterTransporting()
        }
      }

      State.UNLOADING -> {
        if (state == State.TRANSPORTING) {
          // Exit actions
          Services.stopBelt().subscribe()

          state = State.UNLOADING
          enterUnloading()
        }
      }

      State.JOB_DONE -> {
        // Exit actions
        if (state == State.UNLOADING) unregisterTimer("armPickupTimeout-${id}").subscribe()

        state = State.JOB_DONE
      }
    }
  }

  private fun enterTransporting() {
    // Invoke MoveBelt Action
    Services.moveBelt().subscribe()
  }

  private fun enterUnloading() {
    // Starting timer for eArmPickup
    registerActorTimer(
        "armPickupTimeout-${id}",
        "armPickupTimeout",
        0,
        Duration.ofSeconds(0),
        Duration.ofSeconds(1),
      )
      .subscribe()
  }

  override fun markObjectValidity() {
    transition(State.TRANSPORTING)
  }

  override fun startUnloading() {
    transition(State.UNLOADING)
  }

  override fun markJobDone() {
    transition(State.JOB_DONE)
  }

  override fun markPickedUp() {
    transition(State.LOADING)
  }

  override fun armPickupTimeout(): Mono<Void> {
    if (state == State.UNLOADING) {
      // Raising eArmPickup
      Utils.publishEvent(daprClient, "pubsub", "eArmPickup", mutableMapOf<String, Any?>())
        .subscribe()
    }

    return Mono.empty()
  }
}
