plugins { id("common-conventions") }

group = "at.ac.uibk.dps.dapr.execution.object"

dependencies {
  implementation("org.slf4j:slf4j-api:2.0.16")
  runtimeOnly("ch.qos.logback:logback-classic:1.5.16")
}

application { mainClass.set("at.ac.uibk.dps.dapr.execution.object.MetricsCollectorKt") }

tasks.test { useJUnitPlatform() }

springBoot { mainClass.set("at.ac.uibk.dps.dapr.execution.object.MetricsCollectorKt") }

tasks.bootJar { archiveFileName.set("DaprMetricsCollector.jar") }
