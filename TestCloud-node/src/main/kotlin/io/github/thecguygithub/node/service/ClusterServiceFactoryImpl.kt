package io.github.thecguygithub.node.service

import io.github.thecguygithub.api.services.ClusterService
import io.github.thecguygithub.api.services.ClusterServiceFactory
import io.github.thecguygithub.api.services.ClusterServiceStates
import io.github.thecguygithub.api.tasks.ClusterTask
import io.github.thecguygithub.launcher.util.FileSystemUtil
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.platforms.PlatformService
import io.github.thecguygithub.node.service.utils.ClusterDefaultArgs
import io.github.thecguygithub.node.service.utils.ServicePortDetector.detectServicePort
import io.github.thecguygithub.node.templates.TemplateFactory.cloneTemplate
import io.github.thecguygithub.node.util.JavaFileAttach
import lombok.SneakyThrows
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.IntStream


class ClusterServiceFactoryImpl : ClusterServiceFactory {

    val logger = Logger()

    @SneakyThrows
    override fun runGroupService(task: ClusterTask) {

        logger.debug("Making localService")

        try {
            logger.warn(task.name())
            logger.warn(generateOrderedId(task).toString())
            logger.warn(UUID.randomUUID().toString())
            logger.warn(detectServicePort(task).toString())
            logger.warn(Node.nodeConfig?.localNode.toString())
        } catch (e: Exception) {
            logger.error(e.toString())
        }

        val localService = ClusterLocalServiceImpl(
            task,
            generateOrderedId(task),
            UUID.randomUUID(),
            detectServicePort(task),
            "0.0.0.0",
            Node.nodeConfig?.localNode
        )

        logger.debug("setting platform")

        val platform = localService.platform()

        Node.instance?.getRC()?.sendMessage("SERVICE;${localService.name()};EVENT;START;${Node.nodeConfig?.localNode}", "testcloud-service-events")
        logger.info("The service &8'&f${localService.name()}&8' &7is starting now&8...")
        Node.serviceProvider?.services()?.add(localService)

        // call other nodes
        // Node.instance().clusterProvider().broadcast(ClusterSyncRegisterServicePacket(localService))
        cloneTemplate(localService)

        platform?.prepare(task.platform()!!, localService)

        val arguments = generateServiceArguments(localService)

        val platformArgs = platform?.arguments
        if (platformArgs != null) {
            arguments.addAll(platformArgs)
        }

        val processBuilder = ProcessBuilder(*arguments.toTypedArray()).directory(
            localService.runningDir.toFile()
        ).redirectErrorStream(true)

        // send the platform boot jar
        processBuilder.environment()["bootstrapFile"] = task.platform()!!.platformJarName()
        // processBuilder.environment()["nodeEndPointPort"] = java.lang.String.valueOf(Node.instance().clusterProvider().localNode().data().port())
        processBuilder.environment()["redis_user"] = Node.nodeConfig?.redis?.user
        processBuilder.environment()["redis_hostname"] = Node.nodeConfig?.redis?.hostname
        processBuilder.environment()["redis_password"] = Node.nodeConfig?.redis?.password
        processBuilder.environment()["redis_port"] = Node.nodeConfig?.redis?.port.toString()
        processBuilder.environment()["serviceId"] = localService.id().toString()
        processBuilder.environment()["forwarding_secret"] = PlatformService.FORWARDING_SECRET
        processBuilder.environment()["hostname"] = localService.hostname()
        processBuilder.environment()["port"] = localService.port().toString()
        // processBuilder.environment()["node-hostname"] = Node.instance().clusterProvider().localNode().localServiceBindingAddress()

        // copy platform plugin for have a better control of service
        val pluginDir = localService.runningDir.resolve(platform?.pluginDir!!)
        pluginDir.toFile().mkdirs()

        Files.copy(
            Path.of("local/dependencies/testcloud-plugin.jar"),
            pluginDir.resolve("testcloud-plugin.jar"),
            StandardCopyOption.REPLACE_EXISTING
        )

        // add the platform plugin data
        JavaFileAttach.append(
            pluginDir.resolve("polocloud-plugin.jar").toFile(),
            platform.pluginData,
            platform.pluginDataPath
        )

        val serverIconPath = localService.runningDir.resolve("server-icon.png")
        if (!Files.exists(serverIconPath)) {
            FileSystemUtil.copyClassPathFile(javaClass.classLoader, "server-icon.png", serverIconPath.toString())
        }

        // clone group arguments
        // localService.properties().extendsProperties(localService.group().properties())

        localService.state(ClusterServiceStates.STARTING)
        localService.update()

        // run platform
        localService.start(processBuilder)
    }

    fun generateServiceArguments(clusterService: ClusterService): MutableList<String> {
        val arguments = LinkedList<String>()

        arguments.add("java")

        arguments.addAll(ClusterDefaultArgs.ARGUMENTS)

        arguments.add("-Xms" + clusterService.group().maxMemory() + "M")
        arguments.add("-Xmx" + clusterService.group().maxMemory() + "M")

        arguments.add("-cp")

        val path = "../../local/dependencies/"
        //todo use dynamic queue
        val neededDependencies = listOf(
            "testcloud-instance.jar",
            "testcloud-api.jar"
        )

        arguments.add(
            java.lang.String.join(
                if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")) ";" else ":",
                neededDependencies.stream().map { it: String -> path + it }.toList()
            )
        )

        // arguments.add("-javaagent:../../local/dependencies/testcloud-instance.jar")
        // arguments.add("dev.httpmarco.polocloud.instance.ClusterInstanceLauncher")
        return arguments
    }

    override fun shutdownGroupService(clusterService: ClusterService) {
        if (clusterService is ClusterLocalServiceImpl) {
            clusterService.state(ClusterServiceStates.STOPPING)
            Node.instance?.getRC()?.sendMessage("SERVICE;${clusterService.name()};EVENT;STOP", "testcloud-service-events")

            if (clusterService.hasProcess()) {
                // try with platform command a clean shutdown
                clusterService.executeCommand(clusterService.platform()?.type?.shutdownTypeCommand)

                try {
                    checkNotNull(clusterService.process)
                    if (clusterService.process!!.waitFor(PROCESS_TIMEOUT.toLong(), TimeUnit.SECONDS)) {
                        clusterService.process!!.exitValue()
                        clusterService.postShutdownProcess()
                        return
                    }
                } catch (ignored: InterruptedException) {
                }
                clusterService.process?.toHandle()?.destroyForcibly()
            }
            clusterService.postShutdownProcess()
        }
    }

    private fun generateOrderedId(task: ClusterTask): Int {
        return IntStream.iterate(1) { i: Int -> i + 1 }.filter { id: Int ->
            !isIdPresent(
                task,
                id
            )
        }.findFirst().orElseThrow()
    }

    private fun isIdPresent(task: ClusterTask, id: Int): Boolean {
        return task.services()!!.stream().anyMatch { it -> it?.orderedId() == id }
    }

    companion object {
        var PROCESS_TIMEOUT: Int = 5
    }
}