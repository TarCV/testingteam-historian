plugins {
    kotlin("jvm") version "1.4.10"
    application
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "9"
    }
}

group = "com.github.TarCV.testing-team"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8"))

    implementation("dev.nohus:AutoKonfig:1.0.0")
    implementation(kotlin("reflect"))

    testImplementation("junit:junit:4.13.1")
    testImplementation(kotlin("test-junit"))
}
