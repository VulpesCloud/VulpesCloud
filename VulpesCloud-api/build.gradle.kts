plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("cn.lalaki.central") version "1.2.5"
}

dependencies {
    compileOnly(libs.slf4jApi)
    compileOnly(libs.nightConfig.json)
    compileOnly(libs.nightConfig.toml)
    compileOnly(libs.nightConfig.yaml)
    compileOnly(libs.json)
}

tasks.jar {
    archiveFileName.set("vulpescloud-api.jar")
}

tasks.shadowJar {
    archiveFileName.set("vulpescloud-api.jar")
}
