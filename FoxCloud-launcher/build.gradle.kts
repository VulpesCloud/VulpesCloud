plugins {
    kotlin("jvm") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "io.github.thecguygithub"
version = "1.0.0-alpha"


dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
    implementation(libs.jline)
    compileOnly(libs.jedis)
    implementation(libs.slf4jApi)
    implementation(libs.logbackCore)
    implementation(libs.logbackClassic)

    implementation(libs.cloud)
    implementation(libs.aerogel)
    implementation("org.incendo:cloud-kotlin-coroutines:2.0.0")
    implementation("org.incendo:cloud-kotlin-coroutines-annotations:2.0.0")
    implementation("org.incendo:cloud-kotlin-extensions:2.0.0")
    implementation("org.incendo:cloud-annotations:2.0.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.4.0")

    implementation(project(":FoxCloud-api"))
}

tasks.jar {
    from(project(":FoxCloud-node").tasks.shadowJar)
    from(project(":FoxCloud-api").tasks.jar)
    manifest {
        attributes["Main-Class"] = "io.github.thecguygithub.launcher.Launcher"
    }
    archiveFileName.set("foxcloud-launcher.jar")
}

tasks.shadowJar {
    from(project(":FoxCloud-node").tasks.shadowJar)
    from(project(":FoxCloud-api").tasks.shadowJar)
    manifest {
        attributes["Main-Class"] = "io.github.thecguygithub.launcher.Launcher"
    }
    archiveFileName.set("foxcloud-launcher.jar")
}