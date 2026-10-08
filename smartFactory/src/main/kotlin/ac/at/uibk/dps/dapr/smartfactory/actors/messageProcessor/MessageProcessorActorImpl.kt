package ac.at.uibk.dps.dapr.smartfactory.actors.messageProcessor

import ac.at.uibk.dps.dapr.smartfactory.api.MessageProcessingRequest
import ac.at.uibk.dps.dapr.smartfactory.services.Services
import io.dapr.actors.ActorId
import io.dapr.actors.runtime.AbstractActor
import io.dapr.actors.runtime.ActorRuntimeContext
import io.dapr.client.DaprClientBuilder

class MessageProcessorActorImpl(
  runtimeContext: ActorRuntimeContext<MessageProcessorActorImpl>,
  actorId: ActorId,
) : AbstractActor(runtimeContext, actorId), MessageProcessorActor {

  enum class State {
    IDLE,
    PROCESS,
    JOB_DONE,
  }

  enum class ProcessorType {
    EMAIL,
    SMS,
    LOG,
  }

  private var state: State = State.IDLE
  private val processorType: ProcessorType =
    (System.getenv("MP_TYPE") ?: "0").let {
      when (it) {
        "0" -> ProcessorType.EMAIL
        "1" -> ProcessorType.SMS
        else -> ProcessorType.LOG
      }
    }

  private val daprClient = DaprClientBuilder().build()

  private fun transition(targetState: State, data: Any? = null) {
    if (state == State.JOB_DONE) return

    when (targetState) {
      State.IDLE -> {
        state = State.IDLE
      }
      State.PROCESS -> {
        if (state == State.IDLE) {
          state = State.PROCESS
          enterProcess(data as String)
        }
      }
      State.JOB_DONE -> {
        if (state == State.IDLE) state = State.JOB_DONE
      }
    }
  }

  override fun processMessage(message: String) {
    if (state == State.IDLE) transition(State.PROCESS, message)
  }

  override fun markJobDone() {
    if (state == State.IDLE) transition(State.JOB_DONE)
  }

  private fun enterProcess(msg: String) {
    handleMessage(msg)

    transition(State.IDLE)
  }

  private fun handleMessage(msg: String) {
    when (processorType) {
      ProcessorType.EMAIL -> {
        Services.processEmail(MessageProcessingRequest(msg)).subscribe()
      }
      ProcessorType.SMS -> {
        Services.processSms(MessageProcessingRequest(msg)).subscribe()
      }
      ProcessorType.LOG -> {
        val logs: MutableList<String> =
          daprClient.getState("statestore", "logs", MutableList::class.java).block()?.value
            as MutableList<String>? ?: mutableListOf<String>()
        logs.add(msg)
        daprClient.saveState("statestore", "logs", logs).block()
      }
    }
  }
}
