plugins {
    kotlin("jvm") version "2.0.21"
    // id("java")
}

group = "io.github.thecguygithub"
version = "1.0.0-alpha"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":VulpesCloud-api"))
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.github.thecguygithub.wrapper.WrapperLauncher"
    }
    archiveFileName.set("vulpescloud-launcher.jar")
}