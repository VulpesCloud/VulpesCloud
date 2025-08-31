package de.vulpescloud.node.services.impl.docker

import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.*
import com.github.dockerjava.core.DockerClientImpl
import de.vulpescloud.api.serversoftware.SoftwareType
import de.vulpescloud.api.services.Service
import de.vulpescloud.node.Node
import de.vulpescloud.node.serversoftware.impl.*
import de.vulpescloud.node.services.AbstractServiceFactory
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.copyTo

class DockerServiceFactory : AbstractServiceFactory() {

    override val factoryName: String = "docker"

    override suspend fun prepareService(service: Service): DockerService {
        val dockerService = DockerService(service)

        dockerService.path().resolve(dockerService.service.task.software.pluginDir).toFile().mkdirs()

        service.task.templates
            .sortedBy { it.weight }
            .forEach { template ->
                Node.instance.templateStorageProvider.getTemplateStorage(template.storage).apply {
                    createTemplate(template)
                    copyTemplateToPath(template, dockerService.path())
                }
            }

        when (service.task.software.name) {
            "Canvas" -> {
                CanvasDownloader.apply {
                    downloadSoftware(service.task.software.version)
                    getLatestVersionPath(service.task.software.version)
                        .copyTo(dockerService.path().resolve("server.jar"), overwrite = true)
                }
            }
            "Folia" -> {
                FoliaDownloader.apply {
                    downloadSoftware(service.task.software.version)
                    getLatestVersionPath(service.task.software.version)
                        .copyTo(dockerService.path().resolve("server.jar"), overwrite = true)
                }
            }
            "Paper" -> {
                PaperDownloader.apply {
                    downloadSoftware(service.task.software.version)
                    getLatestVersionPath(service.task.software.version)
                        .copyTo(dockerService.path().resolve("server.jar"), overwrite = true)
                }
            }
            "Purpur" -> {
                PurpurDownloader.apply {
                    downloadSoftware(service.task.software.version)
                    getLatestVersionPath(service.task.software.version)
                        .copyTo(dockerService.path().resolve("server.jar"), overwrite = true)
                }
            }
            "Velocity" -> {
                VelocityDownloader.apply {
                    downloadSoftware(service.task.software.version)
                    getLatestVersionPath(service.task.software.version)
                        .copyTo(dockerService.path().resolve("server.jar"), overwrite = true)
                }
            }
        }

        val jvmArgs = mutableListOf<String>()
        val env = mutableListOf<String>()
        jvmArgs.addAll(
            listOf(
                "-DIReallyKnowWhatIAmDoingISwear=true",
                "-Djline.terminal=jline.UnsupportedTerminal",
            )
        )
        jvmArgs.add("-Xms" + service.task.minMemory + "M")
        jvmArgs.add("-Xmx" + service.task.maxMemory + "M")

        jvmArgs.add("-cp")

        val path =
            if (service.task.staticServices) {
                "../../../launcher/dependencies/vulpescloud"
            } else {
                "../../../../launcher/dependencies/vulpescloud"
            }

        val neededDependencies =
            listOf("vulpescloud-api.jar", "vulpescloud-wrapper.jar", "vulpescloud-bridge.jar")

        jvmArgs.add(
            java.lang.String.join(
                if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win"))
                    ";"
                else ":",
                neededDependencies.stream().map { it: String -> path + it }.toList(),
            )
        )

        // TODO: re-add wrapper, current it throws an error when trying to load the java agent, because the jar is empty or does not exist
//        if (service.task.staticServices) {
//            jvmArgs.add(
//                "-javaagent:../../../launcher/dependencies/vulpescloud/vulpescloud-wrapper.jar"
//            )
//        } else {
//            jvmArgs.add(
//                "-javaagent:../../../../launcher/dependencies/vulpescloud/vulpescloud-wrapper.jar"
//            )
//        }
//        jvmArgs.add("de.vulpescloud.wrapper.Wrapper")
        jvmArgs.add("-Xmx" + service.task.maxMemory + "M")
        jvmArgs.add("-Xms" + service.task.minMemory + "M")
        jvmArgs.addAll(service.task.jvmArgs)

        val dockerClient = DockerClientImpl.getInstance(Node.instance.dockerClientConfig, Node.instance.dockerHttpClient)
        val imageName = if (service.task.software.type == SoftwareType.SERVER) {
            "itzg/minecraft-server"
        } else {
            "itzg/mc-proxy"
        }

        dockerClient.pullImageCmd(imageName)
            .withTag("latest")
            .exec(PullImageResultCallback())
            .awaitCompletion()

        val existsImage = dockerClient.listImagesCmd().withImageNameFilter(imageName).exec().firstOrNull { img ->
            img.repoTags?.contains("$imageName:latest") == true
        }

        if (existsImage == null || existsImage.repoTags == null || !existsImage.repoTags.contains("$imageName:latest")) {
            throw Exception("Docker image not found")
        }

        env.add("JVM_OPTS=" + jvmArgs.joinToString(" "))
        env.add("EULA=TRUE")
        env.add("TYPE=CUSTOM")
        env.add("CUSTOM_SERVER=server.jar")
        env.add("MEMORY=${service.task.maxMemory}")
        env.add("MAX_PLAYERS=${service.task.maxPlayers}")
        env.add("MOTD=VulpesCloud provided dockerized service")
        env.add("grpc_hostname=${Node.instance.configProvider.config.grpcHost}")
        env.add("grpc_port=${Node.instance.configProvider.config.grpcPort}")
        env.add("serviceUUID=${service.uuid}")
        env.add("serviceName=${service.task.name}-${service.orderedId}")
        env.add("hostname=${Node.instance.configProvider.config.serviceBindAdress}")
        env.add("secret=${Node.instance.secret}")
        env.add("ENABLE_RCON=false")
        env.add("CREATE_CONSOLE_IN_PIPE=true")

        val ports = Ports()
        ports.bind(ExposedPort.tcp(25565), Ports.Binding.bindPort(service.port))

        service.task.attributes?.fieldsMap?.get("docker-extra-ports")?.toString()
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.forEach { portSpec ->
                val parts = portSpec.split("/")
                val port = parts[0].toInt()
                val protocol = if (parts.size > 1 && parts[1].lowercase() == "udp") "udp" else "tcp"

                val exposed = if (protocol == "udp") ExposedPort.udp(port) else ExposedPort.tcp(port)
                ports.bind(exposed, Ports.Binding.bindPort(port))
            }

        val binds = mutableListOf<Bind>()

        if (service.task.staticServices) {
            val volumeName = "vulpescloud-static-${service.task.name}-${service.orderedId}-${service.uuid}"
            try {
                dockerClient.createVolumeCmd().withName(volumeName).exec()
            } catch (_: Exception) {
            }

            binds.add(Bind.parse("$volumeName:/data"))
        } else {
            binds.add(Bind.parse("${dockerService.path().toAbsolutePath()}:/data"))
        }

        binds.add(Bind.parse("${Path("launcher/dependencies/vulpescloud/vulpescloud-connector.jar").toAbsolutePath()}:/data/plugins/vulpescloud-connector.jar"))

        val hostConfig = HostConfig.newHostConfig()
            .withBinds(binds)
            .withPortBindings(ports)


        DockerClientImpl.getInstance(Node.instance.dockerClientConfig, Node.instance.dockerHttpClient).createContainerCmd(imageName)
            .withEnv(env)
            .withVolumes(
                Volume(dockerService.path().toString()),
                Volume("launcher/dependencies/vulpescloud/vulpescloud-connector.jar")
            )
            .withHostConfig(hostConfig)
            .withName("vulpescloud-service-${service.task.name}-${service.orderedId}-${service.uuid}")
            .withExposedPorts(ExposedPort.tcp(25565))
            .exec()

        return dockerService
    }
}
