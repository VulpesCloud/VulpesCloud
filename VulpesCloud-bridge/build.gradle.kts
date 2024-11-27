plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.dokka") version "1.9.20"
}

group = "de.vulpescloud"
version = "1.0.0-alpha"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":VulpesCloud-api"))
    compileOnly(project(":VulpesCloud-wrapper"))
    compileOnly(rootProject.libs.annotations)
    compileOnly(libs.jedis)
}

tasks.jar {
    archiveFileName.set("vulpescloud-bridge.jar")
}

// todo Add the tasks.dokkaHtmlPartial to Launcher when rewritten
