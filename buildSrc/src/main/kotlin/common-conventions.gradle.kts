plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.ncorti.ktfmt.gradle")
    application
}

repositories { mavenCentral() }

java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.dapr:dapr-sdk:1.18.0")
    implementation("io.dapr:dapr-sdk-actors:1.18.0")
    implementation("io.dapr:dapr-sdk-springboot:1.18.0")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.dropwizard.metrics:metrics-core:4.2.38")
    implementation("io.micrometer:micrometer-core:1.16.4")
}

ktfmt { googleStyle() }