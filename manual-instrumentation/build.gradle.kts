plugins {
    java
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)
    implementation(libs.slf4j.jul)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.sdk.metrics)
    implementation(libs.opentelemetry.sdk.extension.autoconfigure)
    implementation(libs.opentelemetry.sdk.extension.autoconfigure.spi)
    implementation(libs.opentelemetry.runtime.telemetry.java8)
    implementation(libs.opentelemetry.exporter.logging)
}

application {
    mainClass.set("dgroomes.manual_instrumentation.Runner")
}
