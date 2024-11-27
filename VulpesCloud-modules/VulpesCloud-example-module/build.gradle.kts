plugins {

}

group = "de.vulpescloud"
version = "1.0.0-alpha"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":VulpesCloud-api"))
    compileOnly(project(":VulpesCloud-node"))
    compileOnly(rootProject.libs.slf4jApi)
}