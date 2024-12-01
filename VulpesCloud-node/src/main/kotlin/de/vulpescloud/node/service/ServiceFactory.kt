/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.vulpescloud.node.service

import de.vulpescloud.api.network.redis.RedisHashNames
import de.vulpescloud.api.services.ClusterService
import de.vulpescloud.api.services.ClusterServiceFactory
import de.vulpescloud.api.services.ClusterServiceStates
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.service.config.ServiceConfig
import de.vulpescloud.node.template.TemplateFactory
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.stream.IntStream

class ServiceFactory : ClusterServiceFactory {

    private val logger = LoggerFactory.getLogger(ServiceFactory::class.java)

    override fun shutdownAllServicesOnTask(task: Task) {
        TODO("Not yet implemented")
    }

    override fun shutdownService(clusterService: ClusterService) {
        TODO("Not yet implemented")
    }

    override fun startServiceOnTask(task: Task) {
        CompletableFuture.runAsync {
            val localService = LocalService(
                task,
                generateOrderedId(task),
                UUID.randomUUID(),
                detectServicePort(task),
                "0.0.0.0", // todo Make it pull the hostname from the Node's Config
                Node.nodeConfig?.name!!
            )

            val version = localService.version()

            localService.updateLocalServiceState(ClusterServiceStates.PREPARED)

            Node.instance!!.getRC()?.setHashField(
                RedisHashNames.VULPESCLOUD_SERVICES.name,
                localService.name(),
                JSONObject(localService).toString()
            )

            try {

                logger.debug("Adding to services")

                Node.serviceProvider.services()!!.add(localService)

                logger.debug("Cloning Template")

                TemplateFactory.cloneTemplate(localService)

                logger.debug("Prepairing Version")

                try {
                    version?.prepare(task.version(), localService)
                } catch (e: Exception) {
                    logger.error(e.toString())
                }

                logger.debug("generate args")

                val arguments = generateServiceArguments(localService)

                logger.debug("Making processBuilder")

                val processBuilder = ProcessBuilder(*arguments.toTypedArray()).directory(
                    localService.runningDir.toFile()
                ).redirectErrorStream(true)

                logger.debug("Adding Environment vars")

                processBuilder.environment()["bootstrapFile"] = "${task.version().name}-${task.version().versions}.jar"
                processBuilder.environment()["redis_user"] = Node.nodeConfig?.redis?.user
                processBuilder.environment()["redis_hostname"] = Node.nodeConfig?.redis?.hostname
                processBuilder.environment()["redis_password"] = Node.nodeConfig?.redis?.password
                processBuilder.environment()["redis_port"] = Node.nodeConfig?.redis?.port.toString()
                processBuilder.environment()["serviceId"] = localService.id().toString()
                processBuilder.environment()["serviceName"] = localService.name()
                processBuilder.environment()["forwarding_secret"] = Node.versionProvider.FORWARDING_SECRET
                processBuilder.environment()["hostname"] = localService.hostname()
                processBuilder.environment()["port"] = localService.port().toString()

                if (localService.version()?.name == "Velocity")
                    processBuilder.environment()["separateClassLoader"] = false.toString()
                else processBuilder.environment()["separateClassLoader"] = true.toString()

                logger.debug("Making PluginDir")

                val pluginDir = localService.runningDir.resolve(version?.pluginDir!!)
                pluginDir.toFile().mkdirs()

                Files.copy(
                    Path.of("launcher/dependencies/vulpescloud-connector.jar"),
                    pluginDir.resolve("vulpescloud-connector.jar"),
                    StandardCopyOption.REPLACE_EXISTING
                )

                ServiceConfig.makeServiceConfigs(localService)

                logger.debug("Calling update")

                localService.updateLocalServiceState(ClusterServiceStates.CONNECTING)

                logger.debug("Starting Service")

                localService.start(processBuilder)
            } catch (e: Exception) {
                logger.error(e.toString())
            }
        }
    }

    fun generateOrderedId(task: Task): Int {
        return IntStream.iterate(1) { i: Int -> i + 1 }.filter { id: Int ->
            !isIdPresent(
                task,
                id
            )
        }.findFirst().orElseThrow()
    }

    fun isIdPresent(task: Task, id: Int): Boolean {
        val services = task.services() ?: return false // Check if services is null

        services.forEach {
            logger.debug(it!!.name())
            logger.debug(it.orderedId().toString())
        }

        return services.stream()
            .anyMatch { it!!.orderedId() == id }
    }

    fun detectServicePort(task: Task): Int {
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

    private fun generateServiceArguments(clusterService: ClusterService): MutableList<String> {
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

    companion object {
        var PROCESS_TIMEOUT: Int = 5
    }
}