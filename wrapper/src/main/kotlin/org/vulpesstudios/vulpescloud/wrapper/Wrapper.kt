/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.wrapper

import build.buf.gen.vulpescloud.services.v1.getByUuidRequest
import build.buf.gen.vulpescloud.services.v1.serviceSnapshot
import build.buf.gen.vulpescloud.services.v1.updateServiceSnapshotRequest
import com.sun.management.OperatingSystemMXBean
import io.grpc.LoadBalancerRegistry
import io.grpc.internal.PickFirstLoadBalancerProvider
import kotlinx.coroutines.*
import org.vulpesstudios.vulpescloud.wrapper.Premain.preClassCall
import org.vulpesstudios.vulpescloud.wrapper.grpc.GrpcClient
import java.lang.management.ManagementFactory
import java.net.URLClassLoader
import java.util.*
import java.util.jar.JarFile
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds

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

        val grpcHostname = System.getenv("grpc_hostname") ?: "127.0.0.1"
        val grpcPort = (System.getenv("grpc_port") ?: "6565").toInt()
        val secret = System.getenv("secret") ?: "1osajdf3"

        val certDir = Path("vulpescloud/certs")
        val nodeCertFile = certDir.resolve("node.crt").toFile()
        val nodeKeyFile = certDir.resolve("node.key").toFile()
        val caCertFile = certDir.resolve("ca.crt").toFile()

        val sslContext = if (nodeCertFile.exists() && nodeKeyFile.exists() && caCertFile.exists()) {
            println("Loading TLS certificates from ${certDir.toAbsolutePath()}")
            try {
                GrpcClient.buildClientSslContext(
                    nodeCertPem = nodeCertFile.readText(),
                    nodeKeyPem = nodeKeyFile.readText(),
                    caCertPem = caCertFile.readText()
                )
            } catch (e: Exception) {
                println("Failed to build SSL context: ${e.message}")
                null
            }
        } else {
            println("TLS certificates not found in ${certDir.toAbsolutePath()}, connecting without TLS")
            null
        }

        grpcClient.connect(
            host = grpcHostname,
            port = grpcPort,
            sslContext = sslContext,
            secret = secret,
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
    }
}
