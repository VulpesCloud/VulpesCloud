plugins {
    kotlin("jvm") version "2.1.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":VulpesCloud-api"))
    compileOnly(project(":VulpesCloud-bridge"))
    implementation(libs.jedisWrapper)
    implementation(libs.kotlin.stdlib)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation(libs.jedis)
    implementation(libs.logbackCore)
    implementation(libs.logbackClassic)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "de.vulpescloud.wrapper.Wrapper"
        attributes["Premain-Class"] = "de.vulpescloud.wrapper.Premain"
    }
    archiveFileName.set("vulpescloud-wrapper.jar")
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "de.vulpescloud.wrapper.Wrapper"
        attributes["Premain-Class"] = "de.vulpescloud.wrapper.Premain"
    }
    archiveFileName.set("vulpescloud-wrapper.jar")
}