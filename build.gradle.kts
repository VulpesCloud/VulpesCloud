plugins {
    id("org.jetbrains.dokka") version "1.9.20"
    id("maven-publish")
    id("signing")
}

group = "io.github.thecguygithub"
version = "1.0-SNAPSHOT"

tasks.register<Jar>("javadocJar") {
    dependsOn("dokkaHtmlMultiModule") // Make sure `dokkaHtmlMultiModule` runs first
    from(buildDir.resolve("dokka/htmlMultiModule")) // Use the specified output directory
    archiveClassifier.set("javadoc")
}

allprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")

    version = "1.0.0-alpha"
    group = "io.github.thecguygithub"

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
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            artifact(tasks["javadocJar"])

            pom {
                name.set("VulpesCloud")
                description.set("VulpesCloud is a cloud-system for managing Minecraft servers.")
                url.set("https://github.com/VulpesCloud/VulpesCloud")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("thecguygithub")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/VulpesCloud/VulpesCloud.git")
                    developerConnection.set("scm:git:ssh://github.com/VulpesCloud/VulpesCloud.git")
                    url.set("https://github.com/VulpesCloud/VulpesCloud")
                }
            }
        }
    }

    repositories {
        maven {
            name = "Sonatype"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("SONATYPE_USER")
                password = System.getenv("SONATYPE_TOKEN")
            }
        }
    }
}



