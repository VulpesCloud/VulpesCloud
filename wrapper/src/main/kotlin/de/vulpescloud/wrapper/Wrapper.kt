package de.vulpescloud.wrapper

import build.buf.gen.vulpescloud.services.v1.getByUuidRequest
import build.buf.gen.vulpescloud.services.v1.serviceSnapshot
import build.buf.gen.vulpescloud.services.v1.updateServiceSnapshotRequest
import com.sun.management.OperatingSystemMXBean
import de.vulpescloud.wrapper.Premain.preClassCall
import de.vulpescloud.wrapper.grpc.GrpcClient
import io.grpc.LoadBalancerRegistry
import io.grpc.internal.PickFirstLoadBalancerProvider
import java.lang.management.ManagementFactory
import java.net.URLClassLoader
import java.util.*
import java.util.jar.JarFile
import kotlin.io.path.Path
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*

@OptIn(DelicateCoroutinesApi::class)
class Wrapper(args: Array<String>) {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty("io.grpc.netty.disableUnixDomainSockets", "true")
            Wrapper(args)
        }

        lateinit var instance: Wrapper
        val SERVICE_NAME = System.getenv("serviceName") ?: ""
        val SERVICE_UUID =
            UUID.fromString(System.getenv("serviceUUID") ?: "00000000-0000-0000-0000-000000000000")

        val osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
        val memoryBean = ManagementFactory.getMemoryMXBean()

        var startTime: Long = 0
    }

    val grpcClient = GrpcClient()

    suspend fun triggerSnapshotUpdate() {
        runCatching {
                val service =
                    grpcClient.serviceAPI
                        .getByUuid(getByUuidRequest { uuid = SERVICE_UUID.toString() })
                        .service!!
                val uptime = System.currentTimeMillis() - startTime
                val snapshot = serviceSnapshot {
                    this.uuid = service.uuid.toString()
                    this.task = service.task
                    this.node = service.node
                    this.playerCount = service.playerCount
                    this.startTime = service.startTime
                    this.state = service.state
                    this.metadata.putAll(service.metadataMap)
                    this.hostname = service.hostname
                    this.port = service.port
                    this.orderedId = service.orderedId
                    this.pid = ProcessHandle.current().pid()
                    this.cpuUsage = osBean.processCpuLoad
                    this.systemCpuUsage = osBean.cpuLoad
                    this.maxHeapMemory = memoryBean.heapMemoryUsage.max
                    this.heapUsageMemory = memoryBean.heapMemoryUsage.used
                    this.noHeapUsageMemory = memoryBean.nonHeapMemoryUsage.used
                    this.uptimeMillis = uptime
                }

                grpcClient.serviceAPI.updateServiceSnapshot(
                    updateServiceSnapshotRequest { this.snapshot = snapshot }
                )
            }
            .onFailure { println("VC-Wrapper: Failed to update snapshot: ${it.message}") }
    }

    init {
        startTime = System.currentTimeMillis()
        instance = this

        LoadBalancerRegistry.getDefaultRegistry().register(PickFirstLoadBalancerProvider())

        grpcClient.connect(
            System.getenv("grpc_hostname") ?: "127.0.0.1",
            (System.getenv("grpc_port") ?: "6565").toInt(),
            System.getenv("secret") ?: "1osajdf3",
        )

        val file = Path(System.getenv("bootstrapFile")).toFile()

        val classLoader =
            if (Arrays.stream(args).anyMatch { it.equals("--separateClassLoader", true) }) {
                URLClassLoader(arrayOf(file.toURI().toURL()), ClassLoader.getSystemClassLoader())
            } else {
                Premain.INSTRUMENTATION.appendToSystemClassLoaderSearch(JarFile(file))
                ClassLoader.getSystemClassLoader()
            }

        System.setProperty("fabric.systemLibraries", System.getProperty("java.class.path"))

        val thread = Thread {
            val jar = JarFile(file)
            preClassCall(jar, "Premain-Class", classLoader)
            preClassCall(jar, "Launcher-Agent-Class", classLoader)

            val mainClass = jar.manifest.mainAttributes.getValue("Main-Class")
            val main =
                Class.forName(mainClass, true, classLoader)
                    .getMethod("main", Array<String>::class.java)
            val arguments =
                Arrays.stream(args)
                    .filter { it != "--separateClassLoader" }
                    .toArray { size -> arrayOfNulls<String>(size) }

            main.invoke(null, arguments)
        }

        thread.name = "MinecraftServer-${System.getenv("serviceName")}"
        thread.contextClassLoader = classLoader
        thread.setUncaughtExceptionHandler { exceptionThread, exception ->
            println("Uncaught exception in thread ${exceptionThread.name}:")
            exception.printStackTrace()
        }
        thread.start()

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            while (true) {
                triggerSnapshotUpdate()
                delay(5.seconds)
            }
        }

        thread.join()
        exitProcess(0)
    }
}
