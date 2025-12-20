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
    kotlin("jvm") version "2.3.0"
    alias(libs.plugins.shadow)

}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":bridge"))
    compileOnly(project(":wrapper"))

    compileOnly(libs.paper)
    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)

    compileOnly(libs.bundles.proto)
    implementation("org.bstats:bstats-bukkit:3.1.0")
    implementation("org.bstats:bstats-velocity:3.1.0")

    implementation(libs.commandapi.velocity.shade)
    implementation(libs.commandapi.kotlin.velocity)
    implementation(libs.kotlinx.serialization)
}

java {
    withSourcesJar()
}

tasks.shadowJar {
    relocate("org.bstats", "de.vulpescloud.connector.libs.bstats")
}
