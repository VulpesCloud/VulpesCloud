plugins {
    kotlin("jvm") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "de.vulpescloud"
version = "1.0.0-alpha"


dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
}

tasks.jar {
    from(project(":VulpesCloud-node").tasks.shadowJar)
    from(project(":VulpesCloud-api").tasks.jar)
    from(project(":VulpesCloud-wrapper").tasks.shadowJar)
    from(project(":VulpesCloud-connector").tasks.shadowJar)
    from(project(":VulpesCloud-bridge").tasks.jar)
    manifest {
        attributes["Main-Class"] = "de.vulpescloud.launcher.VulpesLauncher"
    }
    archiveFileName.set("vulpescloud-launcher.jar")
}

tasks.shadowJar {
    dependsOn(project(":VulpesCloud-api").tasks.jar)
    dependsOn(project(":VulpesCloud-bridge").tasks.jar)
    dependsOn(project(":VulpesCloud-node").tasks.shadowJar)
    dependsOn(project(":VulpesCloud-wrapper").tasks.shadowJar)
    dependsOn(project(":VulpesCloud-connector").tasks.shadowJar)
    manifest {
        attributes["Main-Class"] = "de.vulpescloud.launcher.VulpesLauncher"
    }
    archiveFileName.set("vulpescloud-launcher.jar")
}