plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    compileOnly(project(":VulpesCloud-launcher"))
    compileOnly(project(":VulpesCloud-api"))
    compileOnly(libs.jline)
    compileOnly(libs.json)
    implementation(libs.jedis)
    compileOnly(libs.slf4jApi)
    implementation(libs.logbackCore)
    implementation(libs.logbackClassic)
    implementation(libs.cloud)
    implementation(libs.cloud.kotlin.coroutines)
    implementation(libs.cloud.kotlin.coroutines.annotations)
    implementation(libs.cloud.extension)
    implementation(libs.cloud.annotations)
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.hikariCP)
    compileOnly(libs.mariadb.java.client)
}



tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.github.thecguygithub.node.NodeLauncher"
    }
    archiveFileName.set("vulpescloud-node.jar")
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "io.github.thecguygithub.node.NodeLauncher"
    }
    archiveFileName.set("vulpescloud-node.jar")
}