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

import de.vulpescloud.api.redis.RedisChannelNames
import de.vulpescloud.api.services.ServiceActions
import de.vulpescloud.wrapper.redis.RedisController
import org.json.JSONObject
import java.lang.instrument.Instrumentation
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.*
import java.util.jar.Attributes
import java.util.jar.JarFile


class Wrapper(args: Array<String>) {
    companion object {
        lateinit var instance: Wrapper
    }

    private var redisController: RedisController? = null

    val serviceName = System.getenv("serviceName")
    val serviceUUID = UUID.fromString(System.getenv("serviceId"))

    init {

        instance = this

        redisController = RedisController()

        val json = JSONObject()

        json.put("action", ServiceActions.AUTHORIZE.name)
        json.put("secret", System.getenv("secret"))
        json.put("serviceName", serviceName)
        json.put("serviceId", serviceUUID)

        redisController?.sendMessage(
            json.toString(),
            RedisChannelNames.VULPESCLOUD_SERVICE_AUTH.name
        )

        //
        // Actually start the Service
        //

        val file = Path.of(System.getenv("bootstrapFile")).toFile()
        val jar = JarFile(file)


        val classLoader = if (Arrays.stream(args)
                .anyMatch { it.equals("--separateClassLoader", true) }
        ) {
            URLClassLoader(arrayOf(file.toURI().toURL()), ClassLoader.getSystemClassLoader())
        } else {
            Premain.INSTRUMENTATION.appendToSystemClassLoaderSearch(jar)
            ClassLoader.getSystemClassLoader()
        }

        System.setProperty("fabric.systemLibraries", System.getProperty("java.class.path"))

        val thread = Thread {
            try {
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

        thread.name = "MinecraftServer-$serviceName"
        thread.contextClassLoader = classLoader
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
}