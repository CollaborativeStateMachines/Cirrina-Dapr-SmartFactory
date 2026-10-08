package ac.at.uibk.dps.dapr.smartfactory.actors.belt

import io.dapr.actors.ActorType
import reactor.core.publisher.Mono

/** Belt actor interface. */
@ActorType(name = "BeltActor")
interface BeltActor {

  fun markObjectValidity()

  fun startUnloading()

  fun markJobDone()

  fun markPickedUp()

  fun armPickupTimeout(): Mono<Void>
}
