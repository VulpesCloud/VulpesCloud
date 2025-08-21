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
    kotlin("jvm") version "2.2.10"
    // id("com.github.johnrengelman.shadow") version "8.1.1"
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(project(":launcher"))
    compileOnly(project(":api"))
    implementation(libs.jline)
    compileOnly(libs.json)
    compileOnly(libs.slf4jApi)
    implementation(libs.logbackCore)
    implementation(libs.logbackClassic)
    implementation(libs.cloud)
    implementation(libs.cloud.kotlin.coroutines)
    implementation(libs.cloud.kotlin.coroutines.annotations)
    implementation(libs.cloud.extension)
    implementation(libs.cloud.annotations)
    implementation(libs.cloud.processors.confirmation)
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.nightConfig.json)
    compileOnly(libs.nightConfig.toml)
    compileOnly(libs.nightConfig.yaml)
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.2")
    implementation(libs.adventure.text.serializer.ansi)
    implementation(libs.adventure.text.serializer.legacy)
    implementation(libs.adventure.text.minimessage)
    implementation(rootProject.libs.guava)
}

sourceSets { getByName("main") { kotlin { srcDir("src/main/kotlin") } } }

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.shadowJar {
    val buildNumber = System.getenv("BUILD_NUMBER")
    //    val versionString = if (buildNumber != null) {
    //        "${version}_${getGitBranch()}@${getGitCommit()}_$buildNumber"
    //    } else {
    //        "${version}_${getGitBranch()}@${getGitCommit()}"
    //    }
    val versionString = "0.0.0-UNKNOWN"

    manifest {
        attributes["Main-Class"] = "de.vulpescloud.node.Node"
        attributes["Implementation-Version"] = versionString
    }
    archiveFileName.set("vulpescloud-node.jar")
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs").get().asFile)
}
