plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    // id("java")
}

group = "io.github.thecguygithub"
version = "1.0.0-alpha"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":VulpesCloud-api"))
    implementation(libs.kotlin.stdlib)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation(libs.jedis)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.github.thecguygithub.wrapper.WrapperLauncher"
        attributes["Premain-Class"] = "io.github.thecguygithub.wrapper.Premain"
    }
    archiveFileName.set("vulpescloud-wrapper.jar")
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "io.github.thecguygithub.wrapper.WrapperLauncher"
        attributes["Premain-Class"] = "io.github.thecguygithub.wrapper.Premain"
    }
    archiveFileName.set("vulpescloud-wrapper.jar")
}