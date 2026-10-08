plugins {
  id("common-conventions")
  id("com.gradleup.shadow") version "9.0.0"
}

group = "ac.at.uibk.dps.dapr.smartfactory.service"

dependencies {
  implementation(project(":api"))

  implementation("org.apache.fory:fory-core:0.15.0")
  implementation("org.apache.fory:fory-kotlin:0.15.0")

  implementation("com.microsoft.onnxruntime:onnxruntime:latest.release")
}

application { mainClass.set("ac.at.uibk.dps.dapr.smartfactory.service.FactoryServiceKt") }

tasks.shadowJar {
  archiveFileName.set("backend.jar")

  manifest { attributes["Main-Class"] = application.mainClass.get() }

  mergeServiceFiles()
}
