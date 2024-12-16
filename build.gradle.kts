import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaTask

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
    id("org.jetbrains.dokka") version "2.0.0"
    id("signing")
    id("maven-publish")
    alias(libs.plugins.shadow)
}

group = "de.vulpescloud"
version = "1.0.0-SNAPSHOT"

//tasks.register<Jar>("javadocJar") {
//    dependsOn("dokkaHtmlMultiModule") // Make sure `dokkaHtmlMultiModule` runs first
//    from(buildDir.resolve("dokka/htmlMultiModule")) // Use the specified output directory
//    archiveClassifier.set("javadoc")
//}

allprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    version = "1.0.0-alpha1"
    group = "de.vulpescloud"

    repositories {
        mavenCentral()
    }

    dependencies {
        "implementation"(rootProject.libs.lombok)
        "annotationProcessor"(rootProject.libs.lombok)
        "implementation"(rootProject.libs.annotations)
        "implementation"(rootProject.libs.gson)
        "implementation"(rootProject.libs.guava)
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
}

tasks.register("copyFilesForMetaRepo") {
    dependsOn(project(":VulpesCloud-api").tasks.jar)
    dependsOn(project(":VulpesCloud-bridge").tasks.jar)
    dependsOn(project(":VulpesCloud-node").tasks.shadowJar)
    dependsOn(project(":VulpesCloud-wrapper").tasks.shadowJar)
    dependsOn(project(":VulpesCloud-connector").tasks.shadowJar)

    doLast {
        copy {
            from(project(":VulpesCloud-api").buildDir.resolve("libs/vulpescloud-api.jar"))
            into("$buildDir/meta-repo")
            rename { "vulpescloud-api.jar" }
        }
        copy {
            from(project(":VulpesCloud-bridge").buildDir.resolve("libs/vulpescloud-bridge.jar"))
            into("$buildDir/meta-repo")
            rename { "vulpescloud-bridge.jar" }
        }
        copy {
            from(project(":VulpesCloud-node").buildDir.resolve("libs/vulpescloud-node.jar"))
            into("$buildDir/meta-repo")
            rename { "vulpescloud-node.jar" }
        }
        copy {
            from(project(":VulpesCloud-wrapper").buildDir.resolve("libs/vulpescloud-wrapper.jar"))
            into("$buildDir/meta-repo")
            rename { "vulpescloud-wrapper.jar" }
        }
        copy {
            from(project(":VulpesCloud-connector").buildDir.resolve("libs/vulpescloud-connector.jar"))
            into("$buildDir/meta-repo")
            rename { "vulpescloud-connector.jar" }
        }
    }
}




