package de.vulpescloud.node.services.impl.local

import build.buf.gen.vulpescloud.events.v1.serviceStateChangedEvent
import build.buf.gen.vulpescloud.services.v1.ServiceState
import build.buf.gen.vulpescloud.templates.v1.createTemplateRequest
import com.electronwill.nightconfig.core.file.FileConfig
import com.electronwill.nightconfig.toml.TomlFormat
import com.electronwill.nightconfig.yaml.YamlFormat
import de.vulpescloud.api.services.Service
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.node.Node
import de.vulpescloud.node.event.EventsService
import de.vulpescloud.node.serversoftware.impl.*
import de.vulpescloud.node.services.AbstractServiceFactory
import de.vulpescloud.node.templates.TemplateStorageRegistry
import de.vulpescloud.node.utils.MongoUtils
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalServiceFactory : AbstractServiceFactory() {

    override val factoryName: String = "local"

    override suspend fun prepareService(service: Service): LocalService {
        val localService =
            LocalService(
                service.copy(
                    state = ServiceStates.PREPARED,
                    node = Node.instance.configProvider.config.nodeName,
                )
            )

        Node.instance.nodeServices.add(localService)

        MongoUtils.updateService(localService.service)

        localService.path().resolve(localService.service.task.software.pluginDir).toFile().mkdirs()

        service.task.templates
            .sortedBy { it.weight }
            .forEach { template ->
                TemplateStorageRegistry.getTemplateStorageByName(template.location.storageName)?.apply {
                    createTemplate(template)
                    Node.instance.localGrpcClient.templateAPI.createTemplate(createTemplateRequest {
                        this.template = template.toDefinition()
                        this.destination = template.location.toDefinition()
                    }) // TODO: This is prob. only temporary as I am to lazy to manually re-create my templates
                    copyTemplateToPath(template, localService.path())
                }
            }

        val arguments = mutableListOf<String>()
        arguments.add("java")
        arguments.addAll(
            listOf(
                "-DIReallyKnowWhatIAmDoingISwear=true",
                "-Djline.terminal=jline.UnsupportedTerminal",
            )
        )
        arguments.add("-Xms" + service.task.minMemory + "M")
        arguments.add("-Xmx" + service.task.maxMemory + "M")

        service.task.jvmArgs.forEach { arguments.add(it) }

        arguments.add("-cp")

        val path =
            if (service.task.staticServices) {
                "../../../launcher/dependencies/vulpescloud/"
            } else {
                "../../../../launcher/dependencies/vulpescloud/"
            }

        val neededDependencies =
            listOf("vulpescloud-api.jar", "vulpescloud-wrapper.jar", "vulpescloud-bridge.jar")

        arguments.add(
            java.lang.String.join(
                if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win"))
                    ";"
                else ":",
                neededDependencies.stream().map { it: String -> path + it }.toList(),
            )
        )

        if (service.task.staticServices) {
            arguments.add(
                "-javaagent:../../../launcher/dependencies/vulpescloud/vulpescloud-wrapper.jar"
            )
        } else {
            arguments.add(
                "-javaagent:../../../../launcher/dependencies/vulpescloud/vulpescloud-wrapper.jar"
            )
        }
        arguments.add("de.vulpescloud.wrapper.Wrapper")

        when (service.task.software.name) {
            "Canvas" -> {
                CanvasDownloader.apply {
                    downloadSoftware(service.task.software.version)
                    getLatestVersionPath(service.task.software.version)
                        .copyTo(localService.path().resolve("server.jar"), true)
                }
                acceptEULA(localService)
                setServerProperties(localService)
                updatePaperGlobalConfig(localService)
                arguments.add("--nogui")
            }
            "Folia" -> {
                FoliaDownloader.apply {
                    downloadSoftware(service.task.software.version)
                    getLatestVersionPath(service.task.software.version)
                        .copyTo(localService.path().resolve("server.jar"), true)
                }
                acceptEULA(localService)
                setServerProperties(localService)
                updatePaperGlobalConfig(localService)

                arguments.add("--separateClassLoader")
                arguments.add("--nogui")
            }
            "Paper" -> {
                PaperDownloader.apply {
                    downloadSoftware(service.task.software.version)
                    getLatestVersionPath(service.task.software.version)
                        .copyTo(localService.path().resolve("server.jar"), true)
                }
                acceptEULA(localService)
                setServerProperties(localService)
                updatePaperGlobalConfig(localService)

                arguments.add("--separateClassLoader")
                arguments.add("--nogui")
            }
            "Purpur" -> {
                PurpurDownloader.apply {
                    downloadSoftware(service.task.software.version)
                    getLatestVersionPath(service.task.software.version)
                        .copyTo(localService.path().resolve("server.jar"), true)
                }
                acceptEULA(localService)
                setServerProperties(localService)
                updatePaperGlobalConfig(localService)

                arguments.add("--separateClassLoader")
                arguments.add("--nogui")
            }
            "Velocity" -> {
                VelocityDownloader.apply {
                    downloadSoftware(service.task.software.version)
                    getLatestVersionPath(service.task.software.version)
                        .copyTo(localService.path().resolve("server.jar"), true)
                }
                updateVelocityConfig(localService)
            }
            "Minestom" -> {
                if (Node.instance.configProvider.config.useModernForwarding) {
                    withContext(Dispatchers.IO) {
                        Files.writeString(
                            localService.path().resolve("forwarding.secret"),
                            Node.instance.getVelocitySecret(),
                        )
                    }
                }
            }
        }

        val processBuilder =
            ProcessBuilder(*arguments.toTypedArray())
                .directory(localService.path().toFile())
                .redirectErrorStream(true)

        processBuilder.environment()["bootstrapFile"] = "server.jar"
        processBuilder.environment()["grpc_hostname"] = Node.instance.configProvider.config.grpcHost
        processBuilder.environment()["grpc_port"] =
            Node.instance.configProvider.config.grpcPort.toString()
        processBuilder.environment()["serviceUUID"] = service.uuid.toString()
        processBuilder.environment()["serviceName"] = service.task.name + "-" + service.orderedId
        processBuilder.environment()["hostname"] =
            Node.instance.configProvider.config.serviceBindAdress
        processBuilder.environment()["port"] = service.port.toString()
        processBuilder.environment()["secret"] = Node.instance.secret

        withContext(Dispatchers.IO) {
            Files.copy(
                Path("launcher/dependencies/vulpescloud/vulpescloud-connector.jar"),
                localService
                    .path()
                    .resolve(service.task.software.pluginDir)
                    .resolve("vulpescloud-connector.jar"),
                StandardCopyOption.REPLACE_EXISTING,
            )

            // TLS Certificates
            val certDir = localService.path().resolve("vulpescloud/certs")
            Files.createDirectories(certDir)
            Files.copy(
                Path("launcher/.secret/certs/node.key.pem"),
                certDir.resolve("node.key"),
                StandardCopyOption.REPLACE_EXISTING
            )
            Files.copy(
                Path("launcher/.secret/certs/node.cert.pem"),
                certDir.resolve("node.crt"),
                StandardCopyOption.REPLACE_EXISTING
            )
            Files.copy(
                Path("launcher/.secret/certs/ca.cert.pem"),
                certDir.resolve("ca.crt"),
                StandardCopyOption.REPLACE_EXISTING
            )
        }

        Node.instance.moduleProvider
            .getAllModules()
            .filter { module ->
                module.moduleInfo.copyToServices &&
                    module.moduleInfo.platforms
                        .map { it.lowercase() }
                        .contains(service.task.software.name.lowercase())
            }
            .forEach {
                Files.copy(
                    Node.instance.moduleProvider.moduleFolder.resolve("${it.moduleInfo.name}.jar"),
                    localService
                        .path()
                        .resolve(service.task.software.pluginDir)
                        .resolve("${it.moduleInfo.name}.jar"),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }

        localService.processBuilder = processBuilder

        MongoUtils.updateService(localService.service)

        EventsService.publish(
            serviceStateChangedEvent {
                this.service = localService.service.toDefinition()
                this.oldState = ServiceState.SERVICE_STATE_UNSPECIFIED
                this.newState = ServiceState.SERVICE_STATE_PREPARED
            },
            true,
        )

        servicePrepareSyncHooks.forEach { hook -> hook(localService) }

        return localService
    }

    private fun acceptEULA(service: LocalService) {
        val properties = Properties()
        properties.clear()

        properties.setProperty("eula", "true")

        val outEula = Files.newOutputStream(service.path().resolve("eula.txt"))
        properties.store(
            outEula,
            "Auto Eula by VulpesCloud (https://account.mojang.com/documents/minecraft_eula)",
        )
    }

    private fun setServerProperties(service: LocalService) {
        val properties = Properties()
        val out = Files.newOutputStream(service.path().resolve("server.properties"))
        if (!service.path().resolve("server.properties").toFile().exists())
            properties.store(out, null)

        properties.load(service.path().resolve("server.properties").toFile().inputStream())

        properties.setProperty("server-ip", Node.instance.configProvider.config.serviceBindAdress)
        properties.setProperty("server-port", service.service.port.toString())
        properties.setProperty("motd", "A VulpesCloud Service!")
        properties.setProperty("online-mode", false.toString())
        properties.setProperty("max-players", service.service.task.maxPlayers.toString())

        properties.store(out, "Minecraft server properties - edited by VulpesCloud")
    }

    fun updatePaperGlobalConfig(service: LocalService) {
        if (Node.instance.configProvider.config.useModernForwarding) {
            service.path().resolve("config").toFile().mkdirs()
            val globalConf =
                FileConfig.builder(
                        service.path().resolve("config/paper-global.yml"),
                        YamlFormat.defaultInstance(),
                    )
                    .sync()
                    .preserveInsertionOrder()
                    .build()

            globalConf.load()
            globalConf.set<String>("proxies.velocity.secret", Node.instance.getVelocitySecret())
            globalConf.set<Boolean>("proxies.velocity.enabled", true)
            globalConf.save()
        }
    }

    fun updateVelocityConfig(service: LocalService) {
        val config =
            FileConfig.builder(
                    service.path().resolve("velocity.toml").toFile(),
                    TomlFormat.instance(),
                )
                .sync()
                .preserveInsertionOrder()
                .build()

        config.load()

        config.set<String>(
            "bind",
            Node.instance.configProvider.config.serviceBindAdress + ":" + service.service.port,
        )

        if (Node.instance.configProvider.config.useModernForwarding) {
            config.set<String>("player-info-forwarding-mode", "modern")
            Files.writeString(
                service.path().resolve("forwarding.secret"),
                Node.instance.getVelocitySecret(),
            )
        }

        config.save()
        config.close()
    }

    companion object {
        private val servicePrepareSyncHooks = mutableListOf<suspend (LocalService) -> Unit>()

        fun addServicePrepareSyncHook(hook: suspend (LocalService) -> Unit) {
            servicePrepareSyncHooks.add(hook)
        }
    }
}
