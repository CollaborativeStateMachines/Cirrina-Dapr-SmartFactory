package ac.at.uibk.dps.dapr.smartfactory.actors.monitor

import io.dapr.actors.ActorType

/** Monitor actor interface. */
@ActorType(name = "MonitorActor")
interface MonitorActor {

  fun markScanned()

  fun markAssembled()

  fun markJobDone()
}
