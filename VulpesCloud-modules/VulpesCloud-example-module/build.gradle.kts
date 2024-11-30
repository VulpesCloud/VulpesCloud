plugins {

}

group = "de.vulpescloud"
version = "1.0.0-alpha"

repositories {
    mavenCentral()
    maven("https://repo.vulpescloud.de/snapshots")
}

dependencies {
    compileOnly("de.vulpescloud:VulpesCloud-api:1.0.0-alpha")
    compileOnly("de.vulpescloud:VulpesCloud-node:1.0.0-alpha")
    compileOnly(rootProject.libs.slf4jApi)
}