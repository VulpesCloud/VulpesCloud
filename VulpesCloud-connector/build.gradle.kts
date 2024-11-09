plugins {
    id("java")
    kotlin("jvm") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "de.vulpescloud"
version = "1.0.0-alpha"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":VulpesCloud-api"))
    implementation(project(":VulpesCloud-bridge"))
    compileOnly(project(":VulpesCloud-wrapper"))

    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)
    compileOnly(libs.jedis)
}

tasks.jar {
    archiveFileName.set("vulpescloud-connector.jar")
}
tasks.shadowJar {
    archiveFileName.set("vulpescloud-connector.jar")
}

tasks.dokkaHtmlPartial {
    dokkaSourceSets {
        create("main") {
            includeNonPublic.set(true)
            sourceRoots.from(file("src/main/kotlin"))
        }
    }
}