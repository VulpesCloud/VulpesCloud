plugins {

}

group = "io.github.thecguygithub"
version = "1.0-SNAPSHOT"

allprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

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

