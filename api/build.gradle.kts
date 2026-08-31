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
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.slf4jApi)
    compileOnly(libs.nightConfig.json)
    compileOnly(libs.nightConfig.toml)
    compileOnly(libs.nightConfig.yaml)
    compileOnly(libs.json)
    compileOnly(libs.kotlinx.serialization)
    compileOnly(libs.bundles.proto)
    compileOnly(libs.mongodb.bson.kotlinx)
}

java {
    withSourcesJar()
    withJavadocJar()
}