package io.github.thecguygithub.launcher

import io.github.thecguygithub.launcher.dependency.Dependency
import io.github.thecguygithub.launcher.dependency.DependencyDownloader
import lombok.SneakyThrows
import java.net.URLClassLoader

class Launcher {

    var CLASS_LOADER: TestCloudClassLoader = TestCloudClassLoader()

    companion object {
        @SneakyThrows
        @JvmStatic
        fun main(args: Array<String>) {

            val CLASS_LOADER = Launcher().CLASS_LOADER


            val boot = Boot()

            val jLineDependency = Dependency("org.jline", "jline", "3.27.0")
            val kotlinSTD = Dependency("org.jetbrains.kotlin", "kotlin-stdlib", "2.0.0")

            CLASS_LOADER.addURL(boot.bootFile().toURI().toURL())


            DependencyDownloader.download(jLineDependency, kotlinSTD)

            Launcher().executeLauncherMain(CLASS_LOADER, args)

            Thread.currentThread().contextClassLoader = CLASS_LOADER
            // Class.forName(boot.mainClass(), true, CLASS_LOADER).getMethod("main", Array<String>::class.java).invoke(null, args as Any)

        }
    }


    fun executeLauncherMain(classLoader: URLClassLoader, args: Array<String>) {
        val loadedClass = classLoader.loadClass("io.github.thecguygithub.node.NodeLauncher")
        val method = loadedClass.getMethod("main", Array<String>::class.java)
        method.invoke(null, args)
    }

}