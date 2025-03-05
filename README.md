# VulpesCloud

![build](https://jenkins.vulpescloud.de/job/VulpesCloud/badge/icon) ![Downloads](https://img.shields.io/github/downloads/VulpesCloud/VulpesCloud/total) ![GitHub Release](https://img.shields.io/github/v/release/VulpesCloud/VulpesCloud?include_prereleases)



> [!CAUTION]
> # VulpesCloud is in Early Development! Bugs may occur, please Report them!

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
| VulpesCloud-wrapper    | When developing a plugin that needs more access to the service than what the bridge can offer                |

### Adding VulpesCloud to the Dependencies 
#### build.gradle.kts
```kotlin
repositories {
    maven("https://repo.vulpescloud.de/releases")
}

dependencies {
    compileOnly("de.vulpescloud:VulpesCloud-api:%version%")
    compileOnly("de.vulpescloud:VulpesCloud-node:%version%")
    compileOnly("de.vulpescloud:VulpesCloud-bridge:%version%")
    compileOnly("de.vulpescloud:VulpesCloud-connector:%version%")
    compileOnly("de.vulpescloud:VulpesCloud-wrapper:%version%")
}
```
#### Replace the `%version%` to the latest version of VulpesCloud (current: `1.0.0`)

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
