package at.ac.uibk.dps.dapr.execution.`object`

import io.dapr.Topic
import io.dapr.client.domain.CloudEvent
import java.io.File
import java.io.PrintWriter
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class EventSubscriber {
  val productionStartTime = AtomicLong(0L)

  val runId = System.getenv("RUN_ID") ?: "0"

  val metricsFiles: Array<File> =
    arrayOf(File("./metrics/ProductionTimes_run_${runId}.csv"), File("./metrics/arrival.csv"))

  val metricFileWriters: Array<PrintWriter> =
    arrayOf(PrintWriter(metricsFiles[0]), PrintWriter(metricsFiles[1]))

  val obsCount: Array<AtomicInteger> = arrayOf(AtomicInteger(0))

  val firstArrivalTime = AtomicLong(Long.MAX_VALUE)
  val lastArrivalTime = AtomicLong(Long.MIN_VALUE)
  val arrivalCount = AtomicInteger(0)

  @Topic(name = "eBeamInterruptedStart", pubsubName = "pubsub")
  @PostMapping("/eBeamInterruptedStart")
  fun eBeamInterruptedStart(): ResponseEntity<Unit> {
    val arrivalTime = System.nanoTime()

    firstArrivalTime.accumulateAndGet(arrivalTime) { current, value -> minOf(current, value) }

    lastArrivalTime.accumulateAndGet(arrivalTime) { current, value -> maxOf(current, value) }

    arrivalCount.incrementAndGet()

    return ResponseEntity.ok().build()
  }

  @Topic(name = "eProductionStarted", pubsubName = "pubsub")
  @PostMapping("/eProductionStarted")
  fun eProductionStarted(@RequestBody event: CloudEvent<Map<String, Any>>): ResponseEntity<Unit> {
    productionStartTime.set(event.data["emittedTime"].toString().toLong())

    logger.info("Production started at ${productionStartTime.get()}")

    return ResponseEntity.ok().build()
  }

  @Topic(name = "eJobDone", pubsubName = "pubsub")
  @PostMapping("/eJobDone")
  fun eJobDone(@RequestBody event: CloudEvent<Map<String, Any>>): ResponseEntity<Unit> {
    val emittedTime = event.data["emittedTime"].toString().toLong()

    val productionTime = (emittedTime - productionStartTime.get()).coerceAtLeast(0L)
    logger.info("Production time: ${productionTime / 1_000_000_000L} seconds")

    metricFileWriters[0].println(
      "index,start_time_ns,end_time_ns,completion_time_ns,completion_time_s"
    )
    metricFileWriters[0].println(
      "${obsCount[0].incrementAndGet()},${productionStartTime.get()},${emittedTime},$productionTime,${productionTime / 1_000_000_000L}"
    )
    metricFileWriters[0].flush()

    val count = arrivalCount.get()

    if (count > 1) {
      val firstArrival = firstArrivalTime.get()
      val lastArrival = lastArrivalTime.get()

      val arrivalDurationSeconds = (lastArrival - firstArrival) / 1_000_000_000.0

      val arrivalRate = (count - 1) / arrivalDurationSeconds

      logger.info("Observed arrival rate: $arrivalRate events/second")

      metricFileWriters[1].println(
        "index,count,first_arrival_time_ns,last_arrival_time_ns,arrival_duration_s,arrival_rate_per_s"
      )
      metricFileWriters[1].println(
        "$runId,$count,$firstArrival,$lastArrival,$arrivalDurationSeconds,$arrivalRate"
      )
      metricFileWriters[1].flush()
    }

    firstArrivalTime.set(Long.MAX_VALUE)
    lastArrivalTime.set(Long.MIN_VALUE)
    arrivalCount.set(0)

    return ResponseEntity.ok().build()
  }
}
