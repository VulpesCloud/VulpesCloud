plugins {
    kotlin("jvm") apply false
    id("org.jetbrains.dokka")
}

dependencies {
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.2.0")
    dokka(project(":api"))
    dokka(project(":bridge"))
    dokka(project(":connector"))
    dokka(project(":launcher"))
    dokka(project(":node"))
    dokka(project(":wrapper"))
}

dokka {
    moduleName.set("VulpesCloud")
}
