package de.vulpescloud.wrapper

import de.vulpescloud.wrapper.Premain.preClassCall
import de.vulpescloud.wrapper.grpc.GrpcClient
import kotlinx.coroutines.DelicateCoroutinesApi
import java.net.URLClassLoader
import java.util.*
import java.util.jar.JarFile
import kotlin.io.path.Path

@OptIn(DelicateCoroutinesApi::class)
class Wrapper(args: Array<String>) {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty("io.grpc.netty.disableUnixDomainSockets", "true")
            Wrapper(args)
        }

        lateinit var instance: Wrapper
    }

    val grpcClient = GrpcClient()

    init {
        instance = this

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

        // System.setProperty("fabric.systemLibraries", System.getProperty("java.class.path"))

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
    }
}
