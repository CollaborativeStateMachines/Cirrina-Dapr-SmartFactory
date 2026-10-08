package ac.at.uibk.dps.dapr.smartfactory

import ac.at.uibk.dps.dapr.smartfactory.actors.arm.ArmActorImpl
import ac.at.uibk.dps.dapr.smartfactory.actors.assemblyController.AssemblyControllerActorImpl
import ac.at.uibk.dps.dapr.smartfactory.actors.belt.BeltActorImpl
import ac.at.uibk.dps.dapr.smartfactory.actors.jobController.JobControllerActorImpl
import ac.at.uibk.dps.dapr.smartfactory.actors.messageProcessor.MessageProcessorActorImpl
import ac.at.uibk.dps.dapr.smartfactory.actors.monitor.MonitorActorImpl
import com.codahale.metrics.CsvReporter
import com.codahale.metrics.MetricRegistry
import io.dapr.actors.runtime.ActorRuntime
import io.dapr.client.DaprClientBuilder
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.dropwizard.DropwizardConfig
import io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry
import io.micrometer.core.instrument.util.HierarchicalNameMapper
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication class SmartFactory

val logger = LoggerFactory.getLogger(SmartFactory::class.java)
val metrics = MetricRegistry()

fun main(args: Array<String>) {
  val role = System.getenv("ROLE")

  ActorRuntime.getInstance().config.setActorIdleTimeout(Duration.ofHours(24))

  when (role) {
    "jobcontroller" -> ActorRuntime.getInstance().registerActor(JobControllerActorImpl::class.java)
    "monitor" -> ActorRuntime.getInstance().registerActor(MonitorActorImpl::class.java)
    "messageprocessor" ->
      ActorRuntime.getInstance().registerActor(MessageProcessorActorImpl::class.java)
    "belt" -> ActorRuntime.getInstance().registerActor(BeltActorImpl::class.java)
    "arm" -> ActorRuntime.getInstance().registerActor(ArmActorImpl::class.java)
    "assemblycontroller" ->
      ActorRuntime.getInstance().registerActor(AssemblyControllerActorImpl::class.java)
    else -> logger.error("Unknown role $role")
  }

  // Creating and saving persistent variables
  val daprClient = DaprClientBuilder().build()
  daprClient.saveState("statestore", "isJobDone", false).block()
  daprClient.saveState("statestore", "logs", mutableListOf<String>()).block()
  daprClient.saveState("statestore", "productsCompleted", 0).block()

  // Initializing metrics
  val metricsPeriod = System.getenv("METRICS_PERIOD")?.toLong() ?: 1L
  CsvReporter.forRegistry(metrics).build(File("./metrics")).start(metricsPeriod, TimeUnit.SECONDS)

  val dropWizardMetricsRegistry =
    object :
        DropwizardMeterRegistry(
          object : DropwizardConfig {
            override fun get(key: String): String? = null

            override fun prefix(): String = ""
          },
          metrics,
          HierarchicalNameMapper.DEFAULT,
          Clock.SYSTEM,
        ) {
        override fun nullGaugeValue(): Double = Double.NaN
      }
      .apply {
        ProcessorMetrics().bindTo(this)
        JvmMemoryMetrics().bindTo(this)
        JvmGcMetrics().bindTo(this)
      }

  runApplication<SmartFactory>(*args)
}
