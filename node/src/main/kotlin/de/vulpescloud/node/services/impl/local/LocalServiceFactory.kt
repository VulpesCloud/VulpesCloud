package de.vulpescloud.node.services.impl.local

import de.vulpescloud.api.services.Service
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.node.Node
import de.vulpescloud.node.serversoftware.impl.*
import de.vulpescloud.node.services.AbstractServiceFactory
import de.vulpescloud.node.utils.MongoUtils
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.copyTo

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

        localService.path().resolve(localService.service.task.software.pluginDir).toFile().mkdirs()

        service.task.templates
            .sortedBy { it.weight }
            .forEach { template ->
                Node.instance.templateStorageProvider.getTemplateStorage(template.storage).apply {
                    createTemplate(template)
                    copyTemplateToPath(template, localService.path())
                }
            }

        when (service.task.software.name) {
            "Canvas" -> {
                CanvasDownloader.apply {
                    downloadSoftware(service.task.software.version)
                    getLatestVersionPath(service.task.software.version)
                        .copyTo(localService.path().resolve("server.jar"), true)
                }
            }
            "Folia" -> {
                FoliaDownloader.apply {
                    downloadSoftware(service.task.software.version)
                    getLatestVersionPath(service.task.software.version)
                        .copyTo(localService.path().resolve("server.jar"), true)
                }
            }
            "Paper" -> {
                PaperDownloader.apply {
                    downloadSoftware(service.task.software.version)
                    getLatestVersionPath(service.task.software.version)
                        .copyTo(localService.path().resolve("server.jar"), true)
                }
            }
            "Purpur" -> {
                PurpurDownloader.apply {
                    downloadSoftware(service.task.software.version)
                    getLatestVersionPath(service.task.software.version)
                        .copyTo(localService.path().resolve("server.jar"), true)
                }
            }
            "Velocity" -> {
                VelocityDownloader.apply {
                    downloadSoftware(service.task.software.version)
                    getLatestVersionPath(service.task.software.version)
                        .copyTo(localService.path().resolve("server.jar"), true)
                }
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

        arguments.add("-cp")

        val path =
            if (service.task.staticServices) {
                "../../../launcher/dependencies/vulpescloud"
            } else {
                "../../../../launcher/dependencies/vulpescloud"
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

        val processBuilder =
            ProcessBuilder(*arguments.toTypedArray())
                .directory(localService.path().toFile())
                .redirectErrorStream(true)

        processBuilder.environment()["bootstrapFile"] = "server.jar"
        processBuilder.environment()["grpc_hostname"] =
            "127.0.0.1" // Node.instance.configProvider.config.grpcHost
        processBuilder.environment()["grpc_port"] =
            Node.instance.configProvider.config.grpcPort.toString()
        processBuilder.environment()["serviceUUID"] = service.uuid.toString()
        processBuilder.environment()["serviceName"] = service.task.name + "-" + service.orderedId
        processBuilder.environment()["hostname"] =
            Node.instance.configProvider.config.serviceBindAdress
        processBuilder.environment()["port"] = service.port.toString()
        processBuilder.environment()["secret"] = Node.instance.secret

        Files.copy(
            Path("launcher/dependencies/vulpescloud/vulpescloud-connector.jar"),
            localService
                .path()
                .resolve(service.task.software.pluginDir)
                .resolve("vulpescloud-connector.jar"),
            StandardCopyOption.REPLACE_EXISTING,
        )

        localService.processBuilder = processBuilder
        Node.instance.nodeServices.add(localService)

        MongoUtils.updateService(localService.service)

        return localService
    }
}
