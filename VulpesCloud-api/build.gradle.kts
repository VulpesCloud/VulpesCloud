plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    compileOnly(libs.slf4jApi)
}

tasks.jar {
    archiveFileName.set("vulpescloud-api.jar")
}

tasks.shadowJar {
    archiveFileName.set("vulpescloud-api.jar")
}