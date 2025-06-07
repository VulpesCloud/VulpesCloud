plugins {
    kotlin("jvm") apply false
    id("org.jetbrains.dokka")
}

dependencies {
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0")
    dokka(project(":VulpesCloud-api"))
    dokka(project(":VulpesCloud-bridge"))
    dokka(project(":VulpesCloud-connector"))
    dokka(project(":VulpesCloud-launcher"))
    dokka(project(":VulpesCloud-node"))
    dokka(project(":VulpesCloud-wrapper"))
}

dokka {
    moduleName.set("VulpesCloud")
}
