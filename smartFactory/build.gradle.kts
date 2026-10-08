plugins { id("common-conventions") }

group = "ac.at.uibk.dps.dapr.smartfactory"

version =
  providers
    .fileContents(rootProject.layout.projectDirectory.file("version.txt"))
    .asText
    .get()
    .trim()

application {
  mainClass.set("ac.at.uibk.dps.dapr.smartfactory.SmartFactoryKt")
  applicationName = "smartfactory"
}

dependencies {
  // Fory
  implementation("org.apache.fory:fory-core:0.15.0")
  implementation("org.apache.fory:fory-kotlin:0.15.0")

  // Logging
  implementation("io.github.oshai:kotlin-logging-jvm:8.0.4")

  // JUnit
  testImplementation(platform("org.junit:junit-bom:5.11.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.junit-pioneer:junit-pioneer:2.3.0")

  // Fory Bindings
  implementation(project(":api"))
}

springBoot { mainClass.set("ac.at.uibk.dps.dapr.smartfactory.SmartFactoryKt") }

tasks.bootJar { archiveFileName.set("SmartFactoryApp.jar") }

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25) }
}

tasks.named("compileKotlin") { dependsOn(tasks.named("ktfmtFormat")) }

tasks.named<Test>("test") { useJUnitPlatform() }

tasks.named<Zip>("distZip") { archiveFileName.set("smartfactory.zip") }

tasks.withType<Jar>().configureEach {
  manifest {
    attributes(
      mapOf(
        "Implementation-Version" to project.version.toString(),
        "Enable-Native-Access" to "ALL-UNNAMED",
      )
    )
  }
}
