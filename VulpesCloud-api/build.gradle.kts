plugins {
    kotlin("jvm") version "2.1.0"
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
