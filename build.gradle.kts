plugins {
    kotlin("jvm") version "1.4.10"
    application
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

group = "com.github.TarCV.testing-team"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("junit:junit:4.13.1")
    testImplementation(kotlin("test-junit"))
}
