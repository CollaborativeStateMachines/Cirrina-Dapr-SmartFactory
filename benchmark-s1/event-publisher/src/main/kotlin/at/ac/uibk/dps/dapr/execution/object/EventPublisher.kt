package at.ac.uibk.dps.dapr.execution.`object`

import com.sun.net.httpserver.HttpServer
import io.dapr.client.DaprClient
import io.dapr.client.DaprClientBuilder
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong
import kotlin.time.Clock
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val executorService: ScheduledExecutorService = Executors.newScheduledThreadPool(8)

val daprClient: DaprClient = DaprClientBuilder().build()

const val startBeamInterruptionTopic = "eBeamInterruptedStart"

const val DAPR_APP_PORT = 6000

val PUBLISH_START_DELAY_MS: Long = System.getenv("PUBLISH_START_DELAY")?.toLong() ?: 0L

val PART_ARRIVAL_RATE_PER_SEC: Double =
  System.getenv("PART_ARRIVAl_RATE_PER_SEC")?.toDouble() ?: 1.0

val PUBLISH_MODE: Int = System.getenv("PUBLISH_MODE")?.toInt() ?: 0
// 0 = Poisson arrivals
// 1 = deterministic arrivals

val logger: Logger =
  LoggerFactory.getLogger("at.ac.uibk.dps.dapr.execution.object.EventPublisherKt")

val latch = CountDownLatch(1)

fun main() {
  logger.info("Part arrival rate = $PART_ARRIVAL_RATE_PER_SEC/sec")

  val subscriptionServer = startSubscriptionServer()

  val startTime = System.nanoTime() + (PUBLISH_START_DELAY_MS * 1_000_000L)

  scheduleNextArrival(startTime.toDouble())

  Runtime.getRuntime()
    .addShutdownHook(
      Thread {
        try {
          subscriptionServer.stop(0)

          executorService.shutdown()

          if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) executorService.shutdownNow()
        } catch (exe: Exception) {
          logger.error("Failed to shutdown executor service", exe)

          executorService.shutdownNow()
        }

        try {
          daprClient.close()
        } catch (exe: Exception) {
          logger.error("Failed to shutdown Dapr client", exe)
        }
      }
    )

  latch.await()

  executorService.shutdownNow()
  subscriptionServer.stop(0)
  daprClient.close()
}

fun startSubscriptionServer(): HttpServer {
  val server = HttpServer.create(InetSocketAddress(DAPR_APP_PORT), 0)

  server.createContext("/dapr/subscribe") { exchange ->
    if (exchange.requestMethod == "GET") {
      val response =
        """
        [
            {
                "pubsubname": "pubsub",
                "topic": "eJobDone",
                "routes": {
                    "default": "/jobdone"
                }
            }
        ]
        """
          .trimIndent()

      val responseBytes = response.toByteArray()

      exchange.responseHeaders.add("Content-Type", "application/json")

      exchange.sendResponseHeaders(200, responseBytes.size.toLong())

      exchange.responseBody.use { it.write(responseBytes) }
    } else {
      exchange.sendResponseHeaders(405, -1)
    }
  }

  server.createContext("/jobdone") { exchange ->
    if (exchange.requestMethod == "POST") {
      exchange.requestBody.use { it.readBytes() }

      logger.info("Received eJobDone. Stopping publish")

      val response =
        """
        {
            "status": "SUCCESS"
        }
        """
          .trimIndent()

      val responseBytes = response.toByteArray()

      exchange.responseHeaders.add("Content-Type", "application/json")

      exchange.sendResponseHeaders(200, responseBytes.size.toLong())

      exchange.responseBody.use { it.write(responseBytes) }

      exchange.close()
      latch.countDown()
    } else {
      exchange.sendResponseHeaders(405, -1)
    }
  }

  server.start()

  logger.info("Dapr subscription server started on port $DAPR_APP_PORT")

  return server
}

fun emitStartBeam() {
  try {
    daprClient
      .publishEvent(
        "pubsub",
        startBeamInterruptionTopic,
        mapOf<String, Any>("emittedTime" to getCurrentTimeNs()),
      )
      .subscribe()
  } catch (exe: Exception) {
    logger.error(exe.message, exe)
  }
}

fun scheduleNextArrival(prevArrivalTime: Double) {
  val newArrivalTime: Double =
    prevArrivalTime + getNextArrivalInterval(PART_ARRIVAL_RATE_PER_SEC) * 1_000_000_000.0

  val delay = maxOf(0L, newArrivalTime.roundToLong() - System.nanoTime())

  executorService.schedule(
    {
      emitStartBeam()
      scheduleNextArrival(newArrivalTime)
    },
    delay,
    TimeUnit.NANOSECONDS,
  )
}

fun getNextArrivalInterval(arrivalRate: Double): Double {
  if (PUBLISH_MODE == 1) return 1.0 / arrivalRate

  val expo = ThreadLocalRandom.current().nextExponential()

  return expo / arrivalRate
}

fun getCurrentTimeNs(): Long {
  val now = Clock.System.now()

  return now.epochSeconds * 1_000_000_000L + now.nanosecondsOfSecond
}
