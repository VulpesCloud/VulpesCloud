package de.vulpescloud.wrapper

import de.vulpescloud.bridge.VulpesBridge.getEventManager
import de.vulpescloud.jediswrapper.JedisWrapper
import de.vulpescloud.wrapper.Premain.preClassCall
import de.vulpescloud.wrapper.event.triggers.player.PlayerJoinEventTrigger
import de.vulpescloud.wrapper.event.triggers.player.PlayerLeaveEventTrigger
import de.vulpescloud.wrapper.event.triggers.player.PlayerSwitchServerEventTrigger
import de.vulpescloud.wrapper.event.triggers.service.ServiceStateChangeEventTrigger
import de.vulpescloud.wrapper.mysql.DatabaseProvider
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

        lateinit var instance: Wrapper
    }

    var databaseProvider: DatabaseProvider

    init {
        instance = this

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
            System.getenv("secret"),
        )

        databaseProvider =
            DatabaseProvider(
                System.getenv("mysql_hostname"),
                System.getenv("mysql_port").toInt(),
                System.getenv("mysql_user"),
                System.getenv("mysql_password"),
                System.getenv("mysql_database"),
            )

        ServiceStateChangeEventTrigger(getEventManager())
        PlayerJoinEventTrigger(getEventManager())
        PlayerLeaveEventTrigger(getEventManager())
        PlayerSwitchServerEventTrigger(getEventManager())

        // TODO send service authentication Message

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
            println("Uncaught exception in thread ${exceptionThread.name}: $exception")
        }
        thread.start()
    }
}
