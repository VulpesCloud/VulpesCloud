plugins {
    kotlin("jvm") version "2.0.21"
}

group = "io.github.thecguygithub"
version = "1.0.0-alpha"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":VulpesCloud-api"))
    compileOnly(project(":VulpesCloud-wrapper"))
}

tasks.jar {
    archiveFileName.set("vulpescloud-bridge.jar")
}
