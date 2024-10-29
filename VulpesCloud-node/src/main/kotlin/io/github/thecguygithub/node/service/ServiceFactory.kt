package io.github.thecguygithub.node.service

import io.github.thecguygithub.api.services.ClusterService
import io.github.thecguygithub.api.services.ClusterServiceFactory
import io.github.thecguygithub.api.services.ClusterServiceStates
import io.github.thecguygithub.api.tasks.Task
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.template.TemplateFactory
import io.github.thecguygithub.node.util.JavaFileAttach
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.stream.IntStream

class ServiceFactory : ClusterServiceFactory {

    override fun shutdownAllServicesOnTask(task: Task) {
        TODO("Not yet implemented")
    }

    override fun shutdownService(clusterService: ClusterService) {
        TODO("Not yet implemented")
    }

    override fun startServiceOnTask(task: Task) {

        val localService = LocalService(
                task,
                generateOrderedId(task),
                UUID.randomUUID(),
            25565,
                //detectServicePort(task),
                "127.0.0.1", // todo Make it pull the hostname from the Node's Config
                Node.nodeConfig?.name!!
            )

        val version = localService.version()

        Node.instance!!.getRC()?.sendMessage("SERVICE;${localService.name()};EVENT;STATE;PREPARE", "vulpescloud-event-service")

        try {

            Logger().debug("Adding to services")

            Node.serviceProvider.services()!!.add(localService)

            Logger().debug("Cloning Template")

            TemplateFactory.cloneTemplate(localService)

            Logger().debug("Prepairing Version")

            try {
                version?.prepare(task.version(), localService)
            } catch (e: Exception) {
                Logger.instance.error(e.printStackTrace())
            }

            Logger().debug("generate args")

            val arguments = generateServiceArguments(localService)

            Logger().debug("Making processBuilder")

            val processBuilder = ProcessBuilder(*arguments.toTypedArray()).directory(
                localService.runningDir.toFile()
            ).redirectErrorStream(true)

            Logger().debug("Adding Environment vars")

            processBuilder.environment()["bootstrapFile"] = "${task.version().name}-${task.version().versions}.jar"
            processBuilder.environment()["redis_user"] = Node.nodeConfig?.redis?.user
            processBuilder.environment()["redis_hostname"] = Node.nodeConfig?.redis?.hostname
            processBuilder.environment()["redis_password"] = Node.nodeConfig?.redis?.password
            processBuilder.environment()["redis_port"] = Node.nodeConfig?.redis?.port.toString()
            processBuilder.environment()["serviceId"] = localService.id().toString()
            processBuilder.environment()["forwarding_secret"] = Node.versionProvider.FORWARDING_SECRET
            processBuilder.environment()["hostname"] = localService.hostname()
            processBuilder.environment()["port"] = localService.port().toString()

            Logger().debug("Making PluginDir")

            val pluginDir = localService.runningDir.resolve(version?.pluginDir!!)

            pluginDir.toFile().mkdirs()

            Logger.instance.debug("here is a todo")

            // todo copy the vulpescloud plugin

            Logger().debug("Calling update")

            // localService.state() // todo Update the Service state
            localService.update()

            Logger().debug("Starting Service")

            localService.start(processBuilder)
        } catch (e: Exception) {
            Logger().error(e)
        }
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
        return services.stream()
            .filter { it != null } // Filter out null elements in the list
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
        for (service in Node.serviceProvider.services()!!) {
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

    fun generateServiceArguments(clusterService: ClusterService): MutableList<String> {
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
                "-DIReallyKnowWhatIAmDoingISwear=true"
            )
        )

        arguments.add("-Xms" + clusterService.task().maxMemory() + "M")
        arguments.add("-Xmx" + clusterService.task().maxMemory() + "M")

        arguments.add("-cp")

        val path = "../../local/dependencies/"

        val neededDependencies = listOf(
            "testcloud-api.jar"
        )

        arguments.add(
            java.lang.String.join(
                if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")) ";" else ":",
                neededDependencies.stream().map { it: String -> path + it }.toList()
            )
        )

        return arguments
    }

    companion object {
        var PROCESS_TIMEOUT: Int = 5
    }
}