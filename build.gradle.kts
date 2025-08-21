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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("vulpescloud.parent-build-logic")
    kotlin("jvm") version "2.2.10"
    id("org.jetbrains.dokka") version "2.0.0"
    id("signing")
    id("maven-publish")
    alias(libs.plugins.shadow)
}

group = "de.vulpescloud"
version = "2.0.0"

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

    version = "3.0.0"
    group = "de.vulpescloud"

    repositories {
        mavenCentral()
        maven("https://repo.vulpescloud.de/snapshots")
        maven {
            name = "buf"
            url = uri("https://buf.build/gen/maven")
        }
    }

    dependencies {
        compileOnly(rootProject.libs.annotations)
        compileOnly(rootProject.libs.gson)
        compileOnly(kotlin("reflect"))
    }

    publishing {
        repositories {
            maven {
                name = "vulpescloudReleases"
                url = uri("https://repo.vulpescloud.de/releases/")
                credentials{
                    username = System.getenv("REPO_USERNAME")
                    password = System.getenv("REPO_PASSWORD")
                }
            }

            maven {
                name = "vulpescloudSnapshots"
                url = uri("https://repo.vulpescloud.de/snapshots/")
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
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
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