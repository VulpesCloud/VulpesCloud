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

        val path = "/launcher/dependencies/vulpescloud"

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

        jvmArgs.add(
            "-javaagent:/launcher/dependencies/vulpescloud/vulpescloud-wrapper.jar"
        )

        jvmArgs.add("de.vulpescloud.wrapper.Wrapper")
        if (service.task.software.type == SoftwareType.SERVER) {
            jvmArgs.add("--nogui")
        }
        jvmArgs.addAll(service.task.jvmArgs)

        val dockerClient = DockerClientImpl.getInstance(Node.instance.dockerClientConfig, Node.instance.dockerHttpClient)
        val imageName = "bypixeltv/vulpescloud-wrapper"

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
        env.add("bootstrapFile=server.jar")
        env.add("grpc_hostname=${Node.instance.configProvider.config.grpcHost}")
        env.add("grpc_port=${Node.instance.configProvider.config.grpcPort}")
        env.add("serviceUUID=${service.uuid}")
        env.add("serviceName=${service.task.name}-${service.orderedId}")
        env.add("hostname=${Node.instance.configProvider.config.serviceBindAdress}")
        env.add("secret=${Node.instance.secret}")

        val ports = Ports()
        ports.bind(ExposedPort.tcp(25565), Ports.Binding.bindPort(service.port))

        service.task.attributes.get("docker-extra-ports")
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

            binds.add(Bind.parse("$volumeName:/app"))
        } else {
            binds.add(Bind.parse("${dockerService.path().toAbsolutePath()}:/app"))
        }

        binds.add(Bind.parse("${Path("launcher/dependencies/vulpescloud/vulpescloud-connector.jar").toAbsolutePath()}:/app/plugins/vulpescloud-connector.jar"))
        binds.add(Bind.parse("${Path("${dockerService.path().toAbsolutePath()}/server.jar").toAbsolutePath()}:/app/server.jar"))
        binds.add(Bind.parse("${Path("launcher/dependencies/maven").toAbsolutePath()}:/launcher/dependencies/maven"))
        binds.add(Bind.parse("${Path("launcher/dependencies/vulpescloud").toAbsolutePath()}:/launcher/dependencies/vulpescloud"))
        binds.add(Bind.parse("${Path("launcher/.secret/certs/node.key.pem").toAbsolutePath()}:/app/vulpescloud/certs/node.key"))
        binds.add(Bind.parse("${Path("launcher/.secret/certs/node.cert.pem").toAbsolutePath()}:/app/vulpescloud/certs/node.crt"))
        binds.add(Bind.parse("${Path("launcher/.secret/certs/ca.cert.pem").toAbsolutePath()}:/app/vulpescloud/certs/ca.crt"))

        val hostConfig = HostConfig.newHostConfig()
            .withBinds(binds)
            .withPortBindings(ports)

        DockerClientImpl.getInstance(Node.instance.dockerClientConfig, Node.instance.dockerHttpClient).createContainerCmd(imageName)
            .withEnv(env)
            .withVolumes(
                Volume(dockerService.path().toString()),
                Volume("launcher/dependencies/maven"),
                Volume("launcher/dependencies/vulpescloud"),
                Volume("/app/vulpescloud/certs"),
            )
            .withHostConfig(hostConfig)
            .withName("vulpescloud-service-${service.task.name}-${service.orderedId}-${service.uuid}")
            .withExposedPorts(ExposedPort.tcp(25565))
            .exec()

        Node.instance.nodeServices.add(dockerService)

        return dockerService
    }
}
