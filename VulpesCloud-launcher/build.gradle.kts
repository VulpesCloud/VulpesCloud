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
    kotlin("jvm") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
}

tasks.jar {
    dependsOn(project(":VulpesCloud-api").tasks.jar)
    dependsOn(project(":VulpesCloud-bridge").tasks.jar)
    dependsOn(project(":VulpesCloud-node").tasks.shadowJar)
    dependsOn(project(":VulpesCloud-wrapper").tasks.shadowJar)
    dependsOn(project(":VulpesCloud-connector").tasks.shadowJar)
    manifest {
        attributes["Main-Class"] = "de.vulpescloud.launcher.VulpesLauncher"
    }
    archiveFileName.set("vulpescloud-launcher.jar")
}

tasks.shadowJar {
    dependsOn(project(":VulpesCloud-api").tasks.jar)
    dependsOn(project(":VulpesCloud-bridge").tasks.jar)
    dependsOn(project(":VulpesCloud-node").tasks.shadowJar)
    dependsOn(project(":VulpesCloud-wrapper").tasks.shadowJar)
    dependsOn(project(":VulpesCloud-connector").tasks.shadowJar)
    manifest {
        attributes["Main-Class"] = "de.vulpescloud.launcher.VulpesLauncher"
    }
    archiveFileName.set("vulpescloud-launcher.jar")
}