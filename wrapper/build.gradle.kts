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
    kotlin("jvm") version "2.4.10"
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
    implementation(libs.bouncy.castle)
}

java {
    withSourcesJar()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.vulpesstudios.vulpescloud.wrapper.Wrapper"
        attributes["Premain-Class"] = "org.vulpesstudios.vulpescloud.wrapper.Premain"
    }
}

tasks.shadowJar {
    relocate("io.netty", "org.vulpesstudios.vulpescloud.wrapper.relocate.io.netty")

    manifest {
        attributes["Main-Class"] = "org.vulpesstudios.vulpescloud.wrapper.Wrapper"
        attributes["Premain-Class"] = "org.vulpesstudios.vulpescloud.wrapper.Premain"
    }
}