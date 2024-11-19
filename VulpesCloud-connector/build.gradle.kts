plugins {
    id("java")
    kotlin("jvm") version "2.0.21"
    //id("io.papermc.paperweight.userdev") version "1.7.4"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    //id("io.github.goooler.shadow") version "8.1.8"
}

group = "de.vulpescloud"
version = "1.0.0-alpha"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    //paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")
    implementation(project(":VulpesCloud-api"))
    implementation(project(":VulpesCloud-bridge"))
    compileOnly(project(":VulpesCloud-wrapper"))

    implementation(libs.cloud)
    // implementation(libs.cloud.velocity)
    implementation("org.incendo:cloud-velocity:2.0.0-beta.10")
    implementation(libs.cloud.extension)
    implementation(libs.cloud.kotlin.coroutines)
    implementation(libs.cloud.kotlin.coroutines.annotations)

    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)
    compileOnly(libs.jedis)

    compileOnly(libs.paper)
    implementation(libs.kSpigot)

}

sourceSets {
    getByName("main") {
        kotlin {
            srcDir("src/main/kotlin")
        }
    }
}

//paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

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