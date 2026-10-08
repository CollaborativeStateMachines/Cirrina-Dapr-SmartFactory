package ac.at.uibk.dps.dapr.smartfactory.actors.arm

import io.dapr.actors.ActorType
import reactor.core.publisher.Mono

/** Robotic arm actor interface. */
@ActorType(name = "ArmActor")
interface ArmActor {

  fun initialize()

  fun initiatePickup()

  fun updatePickupStatus(pickupStatus: Boolean)

  fun updateAssemblyStatus(assemblyStatus: Boolean)

  fun markJobDone()

  fun retryTimeout(): Mono<Void>

  fun markPickedUp()

  fun armReset()
}
