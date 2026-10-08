package ac.at.uibk.dps.dapr.smartfactory.utils

import io.dapr.client.DaprClient
import kotlin.time.Clock
import reactor.core.publisher.Mono

object Utils {
  fun publishEvent(
    daprClient: DaprClient,
    pubsubName: String = "pubsub",
    eventTopic: String,
    payload: MutableMap<String, Any?>,
  ): Mono<Void> {
    val emitTime = getCurrentTimeNs()
    payload["emittedTime"] = emitTime

    return daprClient.publishEvent(pubsubName, eventTopic, payload)
  }

  fun getCurrentTimeNs(): Long {
    val now = Clock.System.now()

    return now.epochSeconds * 1_000_000_000L + now.nanosecondsOfSecond
  }
}
