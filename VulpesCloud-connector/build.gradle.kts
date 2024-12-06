/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

plugins {
    id("java")
    kotlin("jvm") version "2.1.0"
    //id("io.papermc.paperweight.userdev") version "1.7.4"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

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
    implementation("org.incendo:cloud-velocity:2.0.0-SNAPSHOT")
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
    destinationDirectory = File("D:\\Christian\\Development\\VulpesCloud\\VulpesCloud-launcher\\build\\libs\\launcher\\dependencies")
}

tasks.shadowJar {
    archiveFileName.set("vulpescloud-connector.jar")
    dependsOn(":VulpesCloud-api:jar")
    dependsOn(":VulpesCloud-bridge:jar")
    destinationDirectory = File("D:\\Christian\\Development\\VulpesCloud\\VulpesCloud-launcher\\build\\libs\\launcher\\dependencies")
}
