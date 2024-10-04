plugins {
    kotlin("jvm") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "io.github.thecguygithub"
version = "1.0.0-alpha"


dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
    implementation(libs.jline)
}

tasks.jar {
    from(project(":TestCloud-node").tasks.jar)
    from(project(":TestCloud-api").tasks.jar)
    manifest {
        attributes["Main-Class"] = "io.github.thecguygithub.launcher.Launcher"
    }
    archiveFileName.set("testcloud-launcher.jar")
}

tasks.shadowJar {
    from(project(":TestCloud-node").tasks.shadowJar)
    from(project(":TestCloud-api").tasks.shadowJar)
    manifest {
        attributes["Main-Class"] = "io.github.thecguygithub.launcher.Launcher"
    }
    archiveFileName.set("testcloud-launcher.jar")
}