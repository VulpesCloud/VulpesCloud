package de.vulpescloud.wrapper

import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.bridge.service.ServiceProviderImpl
import de.vulpescloud.jediswrapper.JedisWrapper
import de.vulpescloud.wrapper.Premain.preClassCall
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.net.URLClassLoader
import java.util.*
import java.util.jar.JarFile
import kotlin.io.path.Path

class Wrapper(args: Array<String>) {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Wrapper(args)
        }
    }

    init {
//        startKoin {
//            modules(
//                module {
//                    single<ServiceProvider> { ServiceProviderImpl() }
//                }
//            )
//        }

        JedisWrapper.initializeRedisControllerWithSecret(
            System.getenv("redis_password"),
            Integer.parseInt(System.getenv("redis_port")),
            System.getenv("redis_hostname"),
            System.getenv("secret")
        )

        // TODO send service authentication Message

        val file = Path(System.getenv("bootstrapFile")).toFile()

        val classLoader = if (Arrays.stream(args)
                .anyMatch { it.equals("--separateClassLoader", true) }
        ) {
            URLClassLoader(arrayOf(file.toURI().toURL()), ClassLoader.getSystemClassLoader())
        } else {
            Premain.INSTRUMENTATION.appendToSystemClassLoaderSearch(JarFile(file))
            ClassLoader.getSystemClassLoader()
        }

        System.setProperty("fabric.systemLibraries", System.getProperty("java.class.path"))

        val thread = Thread {
            try {
                val jar = JarFile(file)
                preClassCall(jar, "Premain-Class", classLoader)
                preClassCall(jar, "Launcher-Agent-Class", classLoader)

                val mainClass = jar.manifest.mainAttributes.getValue("Main-Class")
                val main = Class.forName(mainClass, true, classLoader).getMethod("main", Array<String>::class.java)
                val arguments = Arrays.stream(args).filter { it != "--separateClassLoader" }
                    .toArray { size -> arrayOfNulls<String>(size) }

                main.invoke(null, arguments)
            } catch (e: Exception) {
                println("Error in new Thread: ->>   " + e.printStackTrace())
            }
        }

        thread.name = "MinecraftServer-${System.getenv("serviceName")}"
        thread.contextClassLoader = classLoader
        thread.setUncaughtExceptionHandler { exceptionThread, exception ->  
            println("Uncaught exception in thread ${exceptionThread.name}: $exception")
        }
        thread.start()
    }
}
