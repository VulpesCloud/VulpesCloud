plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.dokka") version "1.9.20"
    id("signing")
}

group = "de.vulpescloud"
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
}




