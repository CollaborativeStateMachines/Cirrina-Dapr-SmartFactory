package ac.at.uibk.dps.dapr.smartfactory.actors.belt

import ac.at.uibk.dps.dapr.smartfactory.metrics
import ac.at.uibk.dps.dapr.smartfactory.utils.Utils
import com.codahale.metrics.Timer
import io.dapr.Topic
import io.dapr.actors.ActorId
import io.dapr.actors.client.ActorClient
import io.dapr.actors.client.ActorProxyBuilder
import io.dapr.client.domain.CloudEvent
import java.util.concurrent.TimeUnit
import kotlin.time.measureTime
import kotlin.time.toJavaDuration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@ConditionalOnProperty("role", havingValue = "belt")
class BeltSubscriber {

  private val actorId = System.getenv("ACTOR_ID") ?: "belt-0"
  private val proxy: BeltActor =
    ActorProxyBuilder(BeltActor::class.java, ActorClient()).build(ActorId(actorId))

  private val eventTimer: Timer = metrics.timer("event.latency")
  private val processEventTimer: Timer = metrics.timer("processEvent.time")

  @Topic(name = "eObjectValid", pubsubName = "pubsub")
  @PostMapping("/eObjectValid")
  fun handleObjectValid(@RequestBody event: CloudEvent<Map<String, Any?>>): ResponseEntity<Unit> {
    val delta = measureTime {
      // Logging event latency
      val eventEmitTime = event.data["emittedTime"]!! as Long
      val deltaTime: Long = (Utils.getCurrentTimeNs() - eventEmitTime).coerceAtLeast(0)
      eventTimer.update(deltaTime, TimeUnit.NANOSECONDS)

      proxy.markObjectValidity()
    }

    // Logging event processing time
    processEventTimer.update(delta.toJavaDuration())

    return ResponseEntity.ok().build()
  }

  @Topic(name = "eStartUnload", pubsubName = "pubsub")
  @PostMapping("/eStartUnload")
  fun handleStartUnload(@RequestBody event: CloudEvent<Map<String, Any?>>): ResponseEntity<Unit> {
    val delta = measureTime {
      // Logging event latency
      val eventEmitTime = event.data["emittedTime"]!! as Long
      val deltaTime: Long = (Utils.getCurrentTimeNs() - eventEmitTime).coerceAtLeast(0)
      eventTimer.update(deltaTime, TimeUnit.NANOSECONDS)

      proxy.startUnloading()
    }

    // Logging event processing time
    processEventTimer.update(delta.toJavaDuration())

    return ResponseEntity.ok().build()
  }

  @Topic(name = "ePickedUp", pubsubName = "pubsub")
  @PostMapping("/ePickedUp")
  fun handlePickedUp(@RequestBody event: CloudEvent<Map<String, Any?>>): ResponseEntity<Unit> {
    val delta = measureTime {
      // Logging event latency
      val eventEmitTime = event.data["emittedTime"]!! as Long
      val deltaTime: Long = (Utils.getCurrentTimeNs() - eventEmitTime).coerceAtLeast(0)
      eventTimer.update(deltaTime, TimeUnit.NANOSECONDS)

      proxy.markPickedUp()
    }

    // Logging event processing time
    processEventTimer.update(delta.toJavaDuration())

    return ResponseEntity.ok().build()
  }

  @Topic(name = "eJobDone", pubsubName = "pubsub")
  @PostMapping("/eJobDone")
  fun handleJobDone(@RequestBody event: CloudEvent<Map<String, Any?>>): ResponseEntity<Unit> {
    val delta = measureTime {
      // Logging event latency
      val eventEmitTime = event.data["emittedTime"]!! as Long
      val deltaTime: Long = (Utils.getCurrentTimeNs() - eventEmitTime).coerceAtLeast(0)
      eventTimer.update(deltaTime, TimeUnit.NANOSECONDS)

      proxy.markJobDone()
    }

    // Logging event processing time
    processEventTimer.update(delta.toJavaDuration())

    return ResponseEntity.ok().build()
  }
}
