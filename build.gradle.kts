plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.dokka") version "1.9.20"
    id("signing")
    id("maven-publish")
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

    version = "1.0.0-alpha"
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

//tasks {
//    publishing {
//        repositories {
//            maven {
//                name = "vulpescloudReleases"
//                url = uri("https://repo.vulpescloud.de/releases/")
//                credentials{
//                    username = System.getenv("REPO_USERNAME")
//                    password = System.getenv("REPO_PASSWORD")
//                }
//            }
//
//            maven {
//                name = "vulpescloudSnapshots"
//                url = uri("https://repo.vulpescloud.de/snapshots/")
//                credentials{
//                    username = System.getenv("REPO_USERNAME")
//                    password = System.getenv("REPO_PASSWORD")
//                }
//            }
//        }
//        publications {
//            create<MavenPublication>("maven") {
//                groupId = project.group.toString()
//                artifactId = project.name
//                version = project.version.toString()
//                from(project.components["java"])
//            }
//        }
//    }
//}


