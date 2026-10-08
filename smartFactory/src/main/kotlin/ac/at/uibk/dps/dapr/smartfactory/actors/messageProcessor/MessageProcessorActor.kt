package ac.at.uibk.dps.dapr.smartfactory.actors.messageProcessor

import io.dapr.actors.ActorType

/** Message processor actor interface. */
@ActorType(name = "MessageProcessorActor")
interface MessageProcessorActor {

  fun processMessage(message: String)

  fun markJobDone()
}
