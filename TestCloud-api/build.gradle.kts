plugins {
    kotlin("jvm") version "2.0.0"
}

dependencies {
    compileOnly(libs.jline)
    compileOnly(libs.netty5)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.github.thecguygithub.node.NodeLauncher"
    }
    archiveFileName.set("testcloud-node.jar")
}