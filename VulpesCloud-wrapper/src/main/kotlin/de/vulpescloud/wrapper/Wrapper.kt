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

package de.vulpescloud.wrapper

import de.vulpescloud.wrapper.redis.RedisController
import java.io.IOException
import java.lang.instrument.Instrumentation
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarInputStream


class Wrapper(args: Array<String>) {
    companion object {
        lateinit var instance: Wrapper
    }

    private var redisController: RedisController? = null

    var service: LocalServiceInformation

    init {

        instance = this

        service = LocalServiceInformation(
            UUID.fromString(System.getenv("serviceId")),
            System.getenv("serviceName")
        )

        redisController = RedisController()

        redisController?.sendMessage("SERVICE;${System.getenv("serviceName")};AUTH;${System.getenv("serviceId")}", "vulpescloud-auth-service")

        //
        // Actually start the Service
        //

        val file = Path.of(System.getenv("bootstrapFile")).toFile()
        val jar = JarFile(file)

        var loader = ClassLoader.getSystemClassLoader()
        if (System.getenv("separateClassLoader").toBoolean()) {
            loader = URLClassLoader(
                arrayOf(file.toURI().toURL()),
                ClassLoader.getSystemClassLoader()
            )
            preloadClasses(Path.of(System.getenv("bootstrapFile")), loader)
        }
        Premain.INSTRUMENTATION.appendToSystemClassLoaderSearch(jar)

        val thread = Thread {
            try {
                val mainClass = jar.manifest.mainAttributes.getValue("Main-Class")
                val main = Class.forName(mainClass, true, loader)
                val arguments = Arrays.stream(args).filter { it != "--separateClassLoader" }.toArray {size -> arrayOfNulls<String>(size)}

                main.methods[0].invoke(null, arguments)

                //main.getMethod("main").invoke(null, arguments)
            } catch (e: Exception) {
                println("Error in new Thread: ->>   " + e.printStackTrace())
            }
        }

        thread.contextClassLoader = loader
        thread.start()

    }

    fun getRC(): RedisController? {
        return redisController
    }

    private fun preClassCall(jarFile: JarFile, attribute: String, loader: ClassLoader) {
        if (jarFile.manifest.mainAttributes.containsKey(Attributes.Name(attribute))) {
            val preClass = Class.forName(jarFile.manifest.mainAttributes.getValue(attribute), true, loader)
            preClass.getMethod("premain", String::class.java, Instrumentation::class.java)
                .invoke(null, null, Premain.INSTRUMENTATION)
        }
    }

    private fun preloadClasses(file: Path, loader: ClassLoader) {
        try {
            JarInputStream(Files.newInputStream(file)).use { stream ->
                var entry: JarEntry
                while ((stream.nextJarEntry.also { entry = it }) != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".class")) {
                        val className = entry.name.replace('/', '.').replace(".class", "")
                        try {
                            Class.forName(className, false, loader)
                        } catch (ignored: ClassNotFoundException) {
                        }
                    }
                }
            }
        } catch (exception: IOException) {
            throw IllegalStateException("Unable to preload classes in app file", exception)
        }
    }
}