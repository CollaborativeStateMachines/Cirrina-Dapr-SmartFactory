plugins {
    kotlin("jvm")
    `maven-publish`
}

group = " ac.at.uibk.dps.dapr.smartfactory"
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    // Fory
    implementation("org.apache.fory:fory-core:0.15.0")
    implementation("org.apache.fory:fory-kotlin:0.15.0")
}