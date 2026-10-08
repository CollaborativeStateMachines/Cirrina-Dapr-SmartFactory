package ac.at.uibk.dps.dapr.smartfactory.actors.assemblyController

import io.dapr.actors.ActorType

/** Assembly controller actor interface. */
@ActorType(name = "AssemblyControllerActor")
interface AssemblyControllerActor {

  fun initialize()

  fun detectedAtStart()

  fun processCapturedPhoto(photoData: ByteArray)

  fun processPhotoScan(scanStatus: Boolean)

  fun objectDiscarded()

  fun detectedAtEnd()

  fun processPickup()

  fun markJobDone()
}
