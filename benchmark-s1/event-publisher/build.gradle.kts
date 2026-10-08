plugins {
  id("common-conventions")
  id("com.gradleup.shadow") version "9.0.0"
}

group = "at.ac.uibk.dps.dapr.execution.object"

dependencies {
  implementation(project(":api"))

  implementation("org.slf4j:slf4j-api:2.0.16")
  runtimeOnly("ch.qos.logback:logback-classic:1.5.16")
}

application { mainClass.set("at.ac.uibk.dps.dapr.execution.object.EventPublisherKt") }

tasks.test { useJUnitPlatform() }

tasks.shadowJar {
  archiveFileName.set("DaprSmartFactoryEventPublisher.jar")

  manifest { attributes["Main-Class"] = application.mainClass.get() }

  mergeServiceFiles()
}
