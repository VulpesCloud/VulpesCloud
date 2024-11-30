plugins {
    kotlin("jvm") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":VulpesCloud-api"))
    implementation(libs.kotlin.stdlib)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation(libs.jedis)
    implementation(libs.logbackCore)
    implementation(libs.logbackClassic)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "de.vulpescloud.wrapper.WrapperLauncher"
        attributes["Premain-Class"] = "de.vulpescloud.wrapper.Premain"
    }
    archiveFileName.set("vulpescloud-wrapper.jar")
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "de.vulpescloud.wrapper.WrapperLauncher"
        attributes["Premain-Class"] = "de.vulpescloud.wrapper.Premain"
    }
    archiveFileName.set("vulpescloud-wrapper.jar")
}