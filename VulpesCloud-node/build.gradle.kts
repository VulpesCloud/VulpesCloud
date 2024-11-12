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
    compileOnly(libs.nightConfig.json)
    compileOnly(libs.nightConfig.toml)
    compileOnly(libs.nightConfig.yaml)
}

sourceSets {
    getByName("main") {
        kotlin {
            srcDir("src/main/kotlin")
        }
    }
}



tasks.jar {
    manifest {
        attributes["Main-Class"] = "de.vulpescloud.node.NodeLauncher"
    }
    archiveFileName.set("vulpescloud-node.jar")
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "de.vulpescloud.node.NodeLauncher"
    }
    archiveFileName.set("vulpescloud-node.jar")
}

tasks.dokkaHtmlPartial {
    dokkaSourceSets {
        create("main") {
            includeNonPublic.set(true)
            sourceRoots.from(file("src/main/kotlin"))
        }
    }
}