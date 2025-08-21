plugins {
    kotlin("jvm") version "2.2.10"
    //id("com.github.johnrengelman.shadow") version "8.1.1"
    alias(libs.plugins.shadow)
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":bridge"))
    implementation(libs.bundles.kotlin)
    implementation(libs.logbackCore)
    implementation(libs.logbackClassic)
    implementation(libs.nightConfig.json)
}

java {
    withSourcesJar()
    withJavadocJar()
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
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs").get().asFile)
}