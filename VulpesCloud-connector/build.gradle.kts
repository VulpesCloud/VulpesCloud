plugins {
    id("java")
    kotlin("jvm") version "2.0.21"
}

group = "io.github.thecguygithub"
version = "1.0.0-alpha"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(project(":VulpesCloud-api"))

    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)
}

tasks.jar {
    archiveFileName.set("vulpescloud-connector.jar")
}