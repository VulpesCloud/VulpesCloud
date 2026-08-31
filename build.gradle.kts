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
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("vulpescloud.parent-build-logic")
    kotlin("jvm") version "2.4.10"
    id("org.jetbrains.dokka") version "2.2.0"
    id("signing")
    id("maven-publish")
    alias(libs.plugins.shadow)
    kotlin("plugin.serialization") version "2.4.10"
}

group = "org.vulpesstudios.vulpescloud"
version = "3.0.0-beta7"

tasks.named("build") {
    enabled = false
}

tasks.named("shadowJar") {
    enabled = false
}

allprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    version = "3.0.0-beta7"
    group = "org.vulpesstudios.vulpescloud"

    repositories {
        mavenCentral()
        maven("https://repo.vulpesstudios.org/snapshots")
        maven {
            name = "buf"
            url = uri("https://buf.build/gen/maven")
        }
    }

    publishing {
        repositories {
            maven {
                name = "vulpescloudReleases"
                url = uri("https://repo.vulpesstudios.org/releases/")
                credentials{
                    username = System.getenv("REPO_USERNAME")
                    password = System.getenv("REPO_PASSWORD")
                }
            }

            maven {
                name = "vulpescloudSnapshots"
                url = uri("https://repo.vulpesstudios.org/snapshots/")
                credentials{
                    username = System.getenv("REPO_USERNAME")
                    password = System.getenv("REPO_PASSWORD")
                }
            }
        }
        publications {
            create<MavenPublication>("maven") {
                groupId = rootProject.group.toString()
                artifactId = project.name
                version = rootProject.version.toString()
                from(project.components["java"])
            }
        }
    }

    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
        }
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = "25"
        targetCompatibility = "25"
    }
}

subprojects {
    tasks.withType<ShadowJar> {
        archiveBaseName.set("vulpescloud-${project.name}")
        archiveFileName.set("vulpescloud-${project.name}.jar")

        destinationDirectory.set(file("${rootProject.layout.buildDirectory.get()}/libs"))
    }
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds all valid subprojects using shadowJar"



    dependsOn(
        subprojects.filter { sub ->
            val hasBuildFile = file("${sub.projectDir}/build.gradle.kts").exists()
            val hasPlugin = sub.plugins.hasPlugin("java") || sub.plugins.hasPlugin("org.jetbrains.kotlin.jvm")
            hasBuildFile && hasPlugin
        }.mapNotNull { sub ->
            sub.tasks.findByName("shadowJar")
        }
    )
}

tasks.register("copyFilesForMetaRepo") {
    dependsOn(tasks.named("buildAll"))

    doLast {
        val buildDir = rootProject.layout.buildDirectory.get().asFile
        val libsDir = File(buildDir, "libs")

        // Copy all JAR files from the libs directory to the meta-repo directory
        val metaRepoDir = File(buildDir, "meta-repo")
        if (metaRepoDir.exists()) {
            metaRepoDir.deleteRecursively()
        }
        metaRepoDir.mkdirs()

        libsDir.listFiles { file -> file.extension == "jar" }?.forEach { jarFile ->
            jarFile.copyTo(File(metaRepoDir, jarFile.name), overwrite = true)
        }

        println("All JAR files copied to ${metaRepoDir.absolutePath}")

        val jarFiles = metaRepoDir.listFiles { file -> file.extension == "jar" }
        if (jarFiles != null && jarFiles.isNotEmpty()) {
            generateCheckSums(File(buildDir, "meta-repo").toPath(), jarFiles.map { it.name })
        }
    }
}
