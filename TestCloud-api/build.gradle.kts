plugins {
    kotlin("jvm") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

dependencies {
    compileOnly(libs.slf4jApi)
}

tasks.jar {
    archiveFileName.set("testcloud-api.jar")
}

tasks.shadowJar {
    archiveFileName.set("testcloud-api.jar")
}