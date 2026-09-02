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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar



plugins {
    kotlin("jvm") version "2.4.10"
    alias(libs.plugins.shadow)
}

repositories {
    mavenCentral()
    maven {
        name = "buf"
        url = uri("https://buf.build/gen/maven")
    }
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
    }
}

dependencies {
    compileOnly(project(":launcher"))
    compileOnly(project(":api"))
    compileOnly(libs.jline)
    compileOnly(libs.jline.console.ui)
    compileOnly(libs.json)
    compileOnly(libs.slf4jApi)
    compileOnly(libs.logbackCore)
    compileOnly(libs.logbackClassic)
    compileOnly(libs.cloud)
    compileOnly(libs.cloud.kotlin.coroutines)
    compileOnly(libs.cloud.kotlin.coroutines.annotations)
    compileOnly(libs.cloud.extension)
    compileOnly(libs.cloud.annotations)
    compileOnly(libs.cloud.processors.confirmation)
    compileOnly(libs.nightConfig.json)
    compileOnly(libs.nightConfig.toml)
    compileOnly(libs.nightConfig.yaml)
    compileOnly(libs.snakeyml)
    compileOnly(libs.caffeine)
    compileOnly(libs.adventure.text.serializer.ansi)
    compileOnly(libs.adventure.text.serializer.legacy)
    compileOnly(libs.adventure.text.minimessage)
    compileOnly(libs.guava)
    compileOnly(libs.bundles.proto)
    compileOnly(libs.bundles.mongo)
    compileOnly(libs.bundles.kotlin)
    compileOnly(libs.bouncy.castle)
    compileOnly(libs.okio)
    compileOnly(libs.okhttp)
    compileOnly(libs.perfmark)
    compileOnly(libs.bundles.docker)
    compileOnly(kotlin("reflect"))
    compileOnly(libs.javaJWT)
    compileOnly(libs.bcrypt)

    compileOnly(libs.sqlite)
    compileOnly(libs.mariadb)

    compileOnly(libs.exposed.core)
    compileOnly(libs.exposed.jdbc)
    compileOnly(libs.hikaricp)

    compileOnly(libs.chronyx.core)
}

sourceSets { getByName("main") { kotlin { srcDir("src/main/kotlin") } } }

java {
    withSourcesJar()
    withJavadocJar()
}

val generateDependenciesJson by
    tasks.registering {
        description = "Generates a Json file containing all non implemented dependencies"
        val outFile = layout.buildDirectory.file("dependencies.json")
        outputs.file(outFile)
        doLast { exportDependenciesJson("dependencies.json") }
    }

tasks.named<ShadowJar>("shadowJar") {
    dependsOn(generateDependenciesJson)
    from(layout.buildDirectory.file("dependencies.json")) { rename { "dependencies.json" } }
}

tasks.shadowJar {
    val buildNumber = System.getenv("BUILD_NUMBER")
    val versionString =
        if (buildNumber != null) {
            "${version}_${getGitBranch()}@${getGitCommit()}_$buildNumber"
        } else {
            "${version}_${getGitBranch()}@${getGitCommit()}"
        }

    manifest {
        attributes["Main-Class"] = "org.vulpesstudios.vulpescloud.node.Node"
        attributes["Implementation-Version"] = versionString
    }
}
