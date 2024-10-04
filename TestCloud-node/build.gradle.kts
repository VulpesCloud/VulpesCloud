import org.jetbrains.kotlin.ir.backend.js.compile

plugins {
    kotlin("jvm") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

dependencies {
    compileOnly(project(":TestCloud-launcher"))
    compileOnly(project(":TestCloud-api"))
    compileOnly(libs.jline)
    compileOnly(libs.netty5)
    compileOnly(libs.json)
    compileOnly(libs.jedis)
    // implementation(libs.cloud)
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
}



tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.github.thecguygithub.node.NodeLauncher"
    }
    archiveFileName.set("testcloud-node.jar")
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "io.github.thecguygithub.node.NodeLauncher"
    }
    archiveFileName.set("testcloud-node.jar")
}