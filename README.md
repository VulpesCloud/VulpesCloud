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

| artifact id           | usage                                                                                         |
|-----------------------|-----------------------------------------------------------------------------------------------|
| api       | When developing almost anything related to VulpesCloud                                        |
| bridge    | When developing Minecraft plugins for VulpesCloud                                             |
| connector | When developing something that needs access to the Main Plugin, for example a Minestom Server |
| node      | When developing modules for the Cloud                                                         |
| wrapper   | Generally no usage for Developers                                                             |

### Adding VulpesCloud to the Dependencies 
#### build.gradle.kts
```kotlin
repositories {
    maven("https://repo.vulpescloud.de/releases")
}

dependencies {
    compileOnly("de.vulpescloud:api:%version%")
    compileOnly("de.vulpescloud:node:%version%")
    compileOnly("de.vulpescloud:bridge:%version%")
    compileOnly("de.vulpescloud:connector:%version%")
    compileOnly("de.vulpescloud:wrapper:%version%")
}
```
#### Replace the `%version%` to the latest version of VulpesCloud (current: `2.0.0-ALPHA)

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
