# VulpesCloud

![build](https://github.com/VulpesCloud/VulpesCloud/actions/workflows/gradle.yml/badge.svg)

# API

### Adding the Vulpescloud Repository
##### build.gradle.kts
```kotlin
repositories {
    maven("https://repo.vulpescloud.de/releases")
}
```
### Artifacts

| artifact id            | usage                                                                                                        |
|------------------------|--------------------------------------------------------------------------------------------------------------|
| VulpesCloud-api        | When developing almost anything related to VulpesCloud                                                       |
| VulpesCloud-bridge     | When developing Minecraft plugins for VulpesCloud                                                            |
| VulpesCloud-connector  | When developing something that needs access to the Main Plugin                                               |
| VulpesCloud-node       | When developing modules for the Cloud                                                                        |
| VulpesCloud-wrapper    | When developing a plugin that needs more access to the service that what the bridge can offer                |

### Adding VulpesCloud to the Dependencies 
#### build.gradle.kts
```kotlin
repositories {
    maven("https://repo.vulpescloud.de/snapshots")
}

dependencies {
    compileOnly("de.vulpescloud:VulpesCloud-api:%version%")
    compileOnly("de.vulpescloud:VulpesCloud-node:%version%")
    compileOnly("de.vulpescloud:VulpesCloud-bridge:%version%")
    compileOnly("de.vulpescloud:VulpesCloud-connector:%version%")
    compileOnly("de.vulpescloud:VulpesCloud-wrapper:%version%")
}
```
#### Replace the `%version%` to the latest version of VulpesCloud (current: `1.0.0-alpha`)

### Snapshots
Snapshots are available from the snapshot repository!
##### build.gradle.kts
```kotlin
repositories {
    maven("https://repo.vulpescloud.de/snapshots")
}
```

## Links

- [Discord](https://discord.gg/dcFSujWqfw)
- [Dokka Docs](https://dokka.vulpescloud.de)
- [Repository](https://repo.vulpescloud.de)
