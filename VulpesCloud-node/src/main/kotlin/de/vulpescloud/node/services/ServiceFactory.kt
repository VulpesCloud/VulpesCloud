package de.vulpescloud.node.services

import de.vulpescloud.api.redis.RedisHashNames
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.json.ServiceSerializer.jsonFromService
import de.vulpescloud.node.services.config.ServiceConfig
import de.vulpescloud.node.template.TemplateFactory
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.stream.IntStream

object ServiceFactory {

    private val logger = LoggerFactory.getLogger(ServiceFactory::class.java)

    fun prepareService(task: Task): Pair<ProcessBuilder, LocalServiceImpl> {
        val localService = LocalServiceImpl(
            task,
            generateOrderedId(task),
            UUID.randomUUID(),
            detectServicePort(task),
            Node.instance.config.hostname,
            Node.instance.config.name
        )
        val ls = Node.instance.serviceProvider.localServices()
        ls.add(localService)
        Node.instance.serviceProvider.updateLocalServices(ls)
        val version = localService.version()

        localService.updateState(ServiceStates.LOADING)

        Node.instance.getRC()?.setHashField(
            RedisHashNames.VULPESCLOUD_SERVICES.name,
            localService.name(),
            jsonFromService(localService).toString()
        )

        Node.instance.serviceProvider.localServices().add(localService)

        TemplateFactory.cloneTemplate(localService)

        version?.prepare(task.version(), localService)

        val arguments = generateServiceArguments(localService)

        val processBuilder = ProcessBuilder(*arguments.toTypedArray()).directory(localService.runningDir.toFile())
            .redirectErrorStream(true)

        processBuilder.environment()["bootstrapFile"] = "${task.version().environment}-${task.version().version}.jar"
        processBuilder.environment()["redis_user"] = Node.instance.config.redis.user
        processBuilder.environment()["redis_hostname"] = Node.instance.config.redis.hostname
        processBuilder.environment()["redis_password"] = Node.instance.config.redis.password
        processBuilder.environment()["redis_port"] = Node.instance.config.redis.port.toString()
        processBuilder.environment()["serviceId"] = localService.id().toString()
        processBuilder.environment()["serviceName"] = localService.name()
        processBuilder.environment()["hostname"] = localService.hostname()
        processBuilder.environment()["port"] = localService.port().toString()

        if (localService.version()?.environment?.name.equals("Velocity", true))
            processBuilder.environment()["separateClassLoader"] = false.toString()
        else processBuilder.environment()["separateClassLoader"] = true.toString()

        val pluginDir = localService.runningDir.resolve(version?.pluginDir!!)
        pluginDir.toFile().mkdirs()

        Files.copy(
            Path.of("launcher/dependencies/vulpescloud-connector.jar"),
            pluginDir.resolve("vulpescloud-connector.jar"),
            StandardCopyOption.REPLACE_EXISTING
        )

        ServiceConfig.makeServiceConfigs(localService)

        localService.updateState(ServiceStates.PREPARED)

        return Pair(processBuilder, localService)
    }

    fun prepareStartedService(task: Task) {
        val pair = this.prepareService(task)
        pair.second.start(pair.first)
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
        val services = task.services() ?: return false // Check if services is null

        services.forEach {
            logger.debug(it!!.name())
            logger.debug(it.orderedId().toString())
        }

        return services.stream()
            .anyMatch { it!!.orderedId() == id }
    }

    private fun detectServicePort(task: Task): Int {
        var serverPort: Int = task.startPort()


        while (isUsed(serverPort)) {
            serverPort++
        }

        return serverPort
    }

    private fun isUsed(port: Int): Boolean {
        for (service in Node.instance.serviceProvider.services()) {
            if (service.port() == port) {
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

    private fun generateServiceArguments(clusterService: Service): MutableList<String> {
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
                "-Djava.util.logging.ConsoleHandler.level=FINE",
                "--nogui"
            )
        )

        arguments.add("-Xms" + clusterService.task().maxMemory() + "M")
        arguments.add("-Xmx" + clusterService.task().maxMemory() + "M")

        arguments.add("-cp")

        val path = "../../launcher/dependencies/"

        val neededDependencies = listOf(
            "vulpescloud-api.jar"
        )

        arguments.add(
            java.lang.String.join(
                if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")) ";" else ":",
                neededDependencies.stream().map { it: String -> path + it }.toList()
            )
        )

        if (clusterService.task().staticService()) {
            arguments.add("-javaagent:../../../launcher/dependencies/vulpescloud-wrapper.jar")
        } else {
            arguments.add("-javaagent:../../../../launcher/dependencies/vulpescloud-wrapper.jar")
        }
        arguments.add("de.vulpescloud.wrapper.WrapperLauncher")

        return arguments
    }

}