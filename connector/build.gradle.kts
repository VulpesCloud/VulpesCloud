/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



plugins {
    id("java")
    kotlin("jvm") version "2.4.10"
    alias(libs.plugins.shadow)
    kotlin("kapt")

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
    implementation("org.bstats:bstats-bukkit:3.2.1")
    implementation("org.bstats:bstats-velocity:3.2.1")

    implementation(libs.commandapi.velocity.shade)
    implementation(libs.commandapi.kotlin.velocity)
    implementation(libs.kotlinx.serialization)
    testImplementation(kotlin("test"))
}

java {
    withSourcesJar()
}

tasks.shadowJar {
    relocate("org.bstats", "org.vulpesstudios.vulpescloud.connector.libs.bstats")
}
