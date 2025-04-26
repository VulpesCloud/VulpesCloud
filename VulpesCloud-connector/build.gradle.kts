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
    kotlin("jvm") version "2.1.20"
    //id("io.papermc.paperweight.userdev") version "1.7.4"
    id("com.gradleup.shadow") version "8.3.6"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    //paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")
    implementation(project(":VulpesCloud-api"))
    implementation(project(":VulpesCloud-bridge"))
    compileOnly(project(":VulpesCloud-wrapper"))

    implementation("dev.jorel:commandapi-velocity-shade:9.6.2-SNAPSHOT")
    implementation("dev.jorel:commandapi-bukkit-kotlin:9.7.0")

    implementation(libs.cloud)
    implementation(libs.cloud.velocity)
    implementation(libs.cloud.annotations)
    implementation(libs.cloud.extension)

    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)
    compileOnly(libs.jedis)
    compileOnly(libs.jedisWrapper)

    compileOnly(libs.paper)

}

sourceSets {
    getByName("main") {
        kotlin {
            srcDir("src/main/kotlin")
        }
    }
}

//paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
tasks.shadowJar {
    archiveFileName.set("vulpescloud-connector.jar")
    dependsOn(":VulpesCloud-api:jar")
    dependsOn(":VulpesCloud-bridge:jar")
    //relocate("dev.jorel.commandapi", "de.vulpescloud.connector.commandapi")
}
