plugins {
    kotlin("jvm") version "2.2.21"
    //id("com.github.johnrengelman.shadow") version "8.1.1"
    alias(libs.plugins.shadow)
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":api"))
    implementation(libs.bundles.kotlin)
    implementation(libs.logbackCore)
    implementation(libs.logbackClassic)
    implementation(libs.nightConfig.json)
    implementation(libs.bundles.proto)
    implementation(libs.kotlinx.serialization)
    implementation(libs.grpc.core)
    implementation(libs.json)
    implementation(libs.mongodb.bson.kotlinx)
}

java {
    withSourcesJar()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "de.vulpescloud.wrapper.Wrapper"
        attributes["Premain-Class"] = "de.vulpescloud.wrapper.Premain"
    }
}

tasks.shadowJar {
    relocate("io.netty", "de.vulpescloud.wrapper.relocate.io.netty")

    manifest {
        attributes["Main-Class"] = "de.vulpescloud.wrapper.Wrapper"
        attributes["Premain-Class"] = "de.vulpescloud.wrapper.Premain"
    }
}