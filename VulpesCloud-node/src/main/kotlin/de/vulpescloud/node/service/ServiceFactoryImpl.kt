package de.vulpescloud.node.service

import de.vulpescloud.api.cluster.AuthenticationProvider
import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.service.Service
import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.api.service.ServiceStates
import de.vulpescloud.api.task.Task
import de.vulpescloud.api.template.TemplateStorageProvider
import de.vulpescloud.api.version.VersionProvider
import de.vulpescloud.api.version.VersionType
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.config.NodeConfig
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.stream.IntStream
import kotlin.io.path.Path

class ServiceFactoryImpl(
    serviceProvider: ServiceProvider,
    private val clusterProvider: ClusterProvider,
    private val templateStorageProvider: TemplateStorageProvider,
    private val versionProvider: VersionProvider,
    private val config: NodeConfig,
    private val authenticationProvider: AuthenticationProvider,
    private val eventManager: EventManager
) : ServiceFactory {

    private val serviceProvider = serviceProvider as ServiceProviderImpl
    private val logger = LoggerFactory.getLogger(ServiceFactoryImpl::class.java)

    override fun prepareService(task: Task): Pair<Service, LocalService> {
        val localService = LocalService(
            task,
            UUID.randomUUID(),
            generateOrderedId(task),
            detectServicePort(task),
            clusterProvider.localNode(),
            ServiceStates.PREPARED,
            task.maxPlayers,
            0,
            environmentVars = emptyList(),
            onlinePlayers = emptyList(),
            serviceProvider = serviceProvider,
            eventManager = eventManager
        )

        localService.path().resolve(localService.task.version.pluginDir).toFile().mkdirs()

        task.templates.forEach {
            templateStorageProvider.getTemplateStorageByName(it.storage)?.copyTemplateToPath(it, localService.path())
        }
        versionProvider.prepareVersion(task.version, localService.path())

        val arguments = generateServiceArguments(localService)

        if (task.version.type == VersionType.SERVER) {
            arguments.add("--nogui")
            arguments.add("--separateClassLoader")
        }

        val processBuilder = ProcessBuilder(*arguments.toTypedArray()).directory(localService.path().toFile())
            .redirectErrorStream(true)

        processBuilder.environment()["bootstrapFile"] = "server.jar"
        processBuilder.environment()["redis_user"] = config.redis().user
        processBuilder.environment()["redis_hostname"] = config.redis().hostname
        processBuilder.environment()["redis_password"] = config.redis().password
        processBuilder.environment()["redis_port"] = config.redis().port.toString()
        processBuilder.environment()["serviceUUID"] = localService.uuid.toString()
        processBuilder.environment()["serviceName"] = localService.name
        processBuilder.environment()["hostname"] = config.hostname()
        processBuilder.environment()["port"] = localService.port.toString()
        processBuilder.environment()["secret"] = authenticationProvider.getAuthenticationToken()

        Files.copy(
            Path("launcher/dependencies/vulpescloud-connector.jar"),
            localService.path().resolve(localService.task.version.pluginDir).resolve("vulpescloud-connector.jar"),
            StandardCopyOption.REPLACE_EXISTING
        )

        ServiceConfig.updateConfigs(localService)

        val service = Service(
            localService.task,
            localService.uuid,
            localService.orderedId,
            localService.port,
            localService.runningNode,
            localService.state,
            localService.maxPlayers,
            localService.onlinePlayerCount,
            localService.name,
            localService.onlinePlayers,
            localService.environmentVars,
        )

        localService.processBuilder = processBuilder

        serviceProvider.localServices.add(localService)
        getRC()?.setHashField("VULPESCLOUD_SERVICES", service.name, JSONObject(service).toString())

        return Pair(service, localService)
    }

    private fun generateOrderedId(task: Task): Int {
        return IntStream.iterate(1) { i: Int -> i + 1 }.filter { id: Int ->
            !isIdPresent(
                task,
                id
            )
        }.findFirst().orElseThrow()
    }

    private fun isIdPresent(task: Task, id: Int): Boolean {
        val services = task.services // Check if services is null

        services.forEach {
            logger.debug(it.name)
            logger.debug(it.orderedId.toString())
        }

        return services.stream()
            .anyMatch { it!!.orderedId == id }
    }

    private fun detectServicePort(task: Task): Int {
        var serverPort: Int = task.startPort


        while (isUsed(serverPort)) {
            serverPort++
        }

        return serverPort
    }

    private fun isUsed(port: Int): Boolean {
        for (service in serviceProvider.services()) {
            if (service.port == port) {
                return true
            }
        }
        try {
            ServerSocket().use { testSocket ->
                testSocket.bind(InetSocketAddress(port))
                return false
            }
        } catch (e: Exception) {
            return true
        }
    }

    private fun generateServiceArguments(clusterService: LocalService): MutableList<String> {
        val arguments = LinkedList<String>()

        arguments.add("java")

        arguments.addAll(
            listOf(
                "-XX:+UseG1GC",
                "-XX:+ParallelRefProcEnabled",
                "-XX:MaxGCPauseMillis=200",
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+DisableExplicitGC",
                "-XX:+AlwaysPreTouch",
                "-XX:G1NewSizePercent=30",
                "-XX:G1MaxNewSizePercent=40",
                "-XX:G1HeapRegionSize=8M",
                "-XX:G1ReservePercent=20",
                "-XX:G1HeapWastePercent=5",
                "-XX:G1MixedGCCountTarget=4",
                "-XX:InitiatingHeapOccupancyPercent=15",
                "-XX:G1MixedGCLiveThresholdPercent=90",
                "-XX:G1RSetUpdatingPauseTimePercent=5",
                "-XX:SurvivorRatio=32",
                "-XX:+PerfDisableSharedMem",
                "-XX:MaxTenuringThreshold=1",
                "-Dusing.aikars.flags=https://mcflags.emc.gs",
                "-Daikars.new.flags=true",
                "-XX:-UseAdaptiveSizePolicy",
                "-XX:CompileThreshold=100",
                "-Dio.netty.recycler.maxCapacity=0",
                "-Dio.netty.recycler.maxCapacity.default=0",
                "-Djline.terminal=jline.UnsupportedTerminal",
                "-Dfile.encoding=UTF-8",
                "-Dclient.encoding.override=UTF-8",
                "-DIReallyKnowWhatIAmDoingISwear=true",
                "-Djava.util.logging.ConsoleHandler.level=FINE"
            )
        )

        arguments.add("-Xms" + clusterService.task.maxMemory + "M")
        arguments.add("-Xmx" + clusterService.task.maxMemory + "M")

        arguments.add("-cp")

        val path = if (clusterService.task.staticServices) {
            "../../../../launcher/dependencies/"
        } else {
            "../../../../../launcher/dependencies/"
        }

        val neededDependencies = listOf(
            "vulpescloud-api.jar",
            "vulpescloud-wrapper.jar",
            "vulpescloud-bridge.jar"
        )

        arguments.add(
            java.lang.String.join(
                if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")) ";" else ":",
                neededDependencies.stream().map { it: String -> path + it }.toList()
            )
        )

        if (clusterService.task.staticServices) {
            arguments.add("-javaagent:../../../../launcher/dependencies/vulpescloud-wrapper.jar")
        } else {
            arguments.add("-javaagent:../../../../../launcher/dependencies/vulpescloud-wrapper.jar")
        }
        arguments.add("de.vulpescloud.wrapper.Wrapper")

        return arguments
    }
}
