pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm") version "2.3.0"
        kotlin("kapt") version "2.3.0"
        id("com.ncorti.ktfmt.gradle") version "0.24.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include("smartFactory", "api", ":benchmark-s1:smart-factory-service", ":benchmark-s1:metrics-collector", ":benchmark-s1:event-publisher")