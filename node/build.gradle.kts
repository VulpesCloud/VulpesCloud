import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.named

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
    alias(libs.plugins.shadow)
    kotlin("plugin.serialization") version "2.2.0"
}

repositories {
    mavenCentral()
    maven {
        name = "buf"
        url = uri("https://buf.build/gen/maven")
    }
}

dependencies {
    compileOnly(project(":launcher"))
    compileOnly(project(":api"))
    compileOnly(libs.jline)
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
    compileOnly(libs.caffeine)
    compileOnly(libs.adventure.text.serializer.ansi)
    compileOnly(libs.adventure.text.serializer.legacy)
    compileOnly(libs.adventure.text.minimessage)
    compileOnly(libs.guava)
    compileOnly(libs.bundles.proto)
    compileOnly(libs.bundles.mongo)
    compileOnly(libs.bundles.kotlin)
}

sourceSets { getByName("main") { kotlin { srcDir("src/main/kotlin") } } }

java {
    withSourcesJar()
    withJavadocJar()
}

val generateDependenciesJson by tasks.registering {
    val outFile = layout.buildDirectory.file("dependencies.json")
    outputs.file(outFile)
    doLast {
        exportDependenciesJson("dependencies.json")
    }
}

tasks.named<ShadowJar>("shadowJar") {
    dependsOn(generateDependenciesJson)
    from(layout.buildDirectory.file("dependencies.json")) {
        rename { "dependencies.json" }
    }
}

tasks.shadowJar {
    val buildNumber = System.getenv("BUILD_NUMBER")
        val versionString = if (buildNumber != null) {
            "${version}_${getGitBranch()}@${getGitCommit()}_$buildNumber"
        } else {
            "${version}_${getGitBranch()}@${getGitCommit()}"
        }

    manifest {
        attributes["Main-Class"] = "de.vulpescloud.node.Node"
        attributes["Implementation-Version"] = versionString
    }
}
