plugins {
    kotlin("jvm") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"
    application
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "9"
    }
}

group = "com.github.TarCV.testing-team"
version = "0.1-SNAPSHOT"

application {
    mainClass.set("com.github.tarcv.testingteam.historian.Historian")
}

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = uri("https://dl.bintray.com/pdvrieze/maven")
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8"))

    implementation("dev.nohus:AutoKonfig:1.0.0")
    implementation(kotlin("reflect"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.1")
    implementation("net.devrieze:xmlutil:0.80.1")
    implementation("net.devrieze:xmlutil-serialization-jvm:0.80.1")
    implementation("com.github.jknack:handlebars:4.1.2")

    testImplementation("junit:junit:4.13.1")
    testImplementation(kotlin("test-junit"))
}
