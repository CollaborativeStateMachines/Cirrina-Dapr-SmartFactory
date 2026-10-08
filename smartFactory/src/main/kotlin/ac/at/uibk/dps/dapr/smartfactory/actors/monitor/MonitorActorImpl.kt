package ac.at.uibk.dps.dapr.smartfactory.actors.monitor

import ac.at.uibk.dps.dapr.smartfactory.api.StatisticsRequest
import ac.at.uibk.dps.dapr.smartfactory.services.Services
import io.dapr.actors.ActorId
import io.dapr.actors.runtime.AbstractActor
import io.dapr.actors.runtime.ActorRuntimeContext
import io.dapr.client.DaprClientBuilder

class MonitorActorImpl(runtimeContext: ActorRuntimeContext<MonitorActorImpl>, id: ActorId) :
  AbstractActor(runtimeContext, id), MonitorActor {

  enum class State {
    MONITORING,
    JOB_DONE,
  }

  private var state: State = State.MONITORING
  private var nScans = 0
  private var nAssemblies = 0

  private val daprClient = DaprClientBuilder().build()

  override fun markScanned() {
    if (state == State.MONITORING) {
      nScans += 1

      sendStatistics()
    }
  }

  override fun markAssembled() {
    if (state == State.MONITORING) {
      nAssemblies += 1

      sendStatistics()
    }
  }

  override fun markJobDone() {
    if (state == State.MONITORING) {
      state = State.JOB_DONE

      sendStatistics()
    }
  }

  private fun sendStatistics() {
    val jobDone =
      daprClient.getState("statestore", "isJobDone", Boolean::class.java).block()?.value ?: false
    val productsCompleted =
      daprClient.getState("statestore", "productsCompleted", Int::class.java).block()?.value ?: 0

    // Invoke SendStatistics service
    Services.sendStatistics(StatisticsRequest(nScans, nAssemblies, productsCompleted, jobDone))
      .subscribe()
  }
}
