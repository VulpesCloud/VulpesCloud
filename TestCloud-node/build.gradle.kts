import org.jetbrains.kotlin.ir.backend.js.compile

plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    compileOnly(project(":TestCloud-launcher"))
    compileOnly(project(":TestCloud-api"))
    compileOnly(libs.jline)
    compileOnly(libs.json)
    implementation(libs.jedis)
    compileOnly(libs.slf4jApi)
    compileOnly(libs.logbackCore)
    compileOnly(libs.logbackClassic)
    implementation(libs.cloud)
    implementation(libs.aerogel)
    // implementation(libs.cloud.annotations)
    // implementation(libs.cloud.task)
    implementation("org.incendo:cloud-kotlin-coroutines:2.0.0")
    implementation("org.incendo:cloud-kotlin-coroutines-annotations:2.0.0")
    implementation("org.incendo:cloud-kotlin-extensions:2.0.0")
    implementation("org.incendo:cloud-annotations:2.0.0")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
    compileOnly("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.mariadb.jdbc:mariadb-java-client:3.4.0")
}



tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.github.thecguygithub.node.NodeLauncher"
    }
    archiveFileName.set("testcloud-node.jar")
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "io.github.thecguygithub.node.NodeLauncher"
    }
    archiveFileName.set("testcloud-node.jar")
}