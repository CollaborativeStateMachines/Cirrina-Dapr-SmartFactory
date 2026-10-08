package ac.at.uibk.dps.dapr.smartfactory.actors.jobController

import ac.at.uibk.dps.dapr.smartfactory.utils.Utils
import io.dapr.actors.ActorId
import io.dapr.actors.runtime.AbstractActor
import io.dapr.actors.runtime.ActorRuntimeContext
import io.dapr.client.DaprClientBuilder

class JobControllerActorImpl(
  runtimeContext: ActorRuntimeContext<JobControllerActorImpl>,
  id: ActorId,
) : AbstractActor(runtimeContext, id), JobControllerActor {

  enum class State {
    STARTING,
    RUNNING,
    JOB_DONE,
  }

  private val totalProducts = 1
  private var state: State = State.STARTING

  private val daprClient = DaprClientBuilder().build()

  override fun initialize() {
    transition(State.STARTING)
  }

  override fun markProductCompleted() {
    if (state == State.RUNNING) {
      daprClient.getState("statestore", "productsCompleted", Int::class.java).block()?.value.let {
        daprClient.saveState("statestore", "productsCompleted", (it ?: 0) + 1).block()
      }
      this.checkJobDone()
    }
  }

  private fun checkJobDone() {
    val productsCompleted =
      daprClient.getState("statestore", "productsCompleted", Int::class.java).block()?.value ?: 0
    if (productsCompleted >= totalProducts) transition(State.JOB_DONE)
  }

  private fun transition(targetState: State) {
    if (state == State.JOB_DONE) // Terminal state
     return

    when (targetState) {
      State.STARTING -> {
        state = State.STARTING
        enterStarting()
      }
      State.RUNNING -> {
        if (state == State.STARTING) state = State.RUNNING
      }
      State.JOB_DONE -> {
        if (state == State.RUNNING) {
          state = State.JOB_DONE
          enterJobDone()
        }
      }
    }
  }

  private fun enterStarting() {
    // Emit event to Message Processor
    Utils.publishEvent(
        daprClient,
        "pubsub",
        "eProcessMessage",
        mutableMapOf("msg" to "Job started..."),
      )
      .subscribe()

    // Transition to running state
    transition(State.RUNNING)
  }

  private fun enterJobDone() {
    // Emit event to Message Processor
    Utils.publishEvent(
        daprClient,
        "pubsub",
        "eProcessMessage",
        mutableMapOf("msg" to "Job done..."),
      )
      .subscribe()

    // Updating job done status
    daprClient.saveState("statestore", "isJobDone", true).block()

    // Emit JobDone
    Utils.publishEvent(daprClient, "pubsub", "eJobDone", mutableMapOf()).subscribe()
  }
}
