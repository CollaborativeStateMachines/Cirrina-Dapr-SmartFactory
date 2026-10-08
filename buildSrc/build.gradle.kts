plugins { `kotlin-dsl` }

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:2.3.0")
    implementation(
        "org.jetbrains.kotlin.plugin.spring:org.jetbrains.kotlin.plugin.spring.gradle.plugin:2.3.0"
    )
    implementation("org.springframework.boot:spring-boot-gradle-plugin:3.4.4")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
    implementation("com.ncorti.ktfmt.gradle:com.ncorti.ktfmt.gradle.gradle.plugin:0.24.0")
}