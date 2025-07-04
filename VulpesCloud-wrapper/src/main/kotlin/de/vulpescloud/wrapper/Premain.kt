package de.vulpescloud.wrapper

import java.lang.instrument.Instrumentation
import java.util.jar.Attributes
import java.util.jar.JarFile

object Premain {

    @JvmStatic
    lateinit var INSTRUMENTATION: Instrumentation

    @JvmStatic
    fun premain(args: String?, instrumentation: Instrumentation) {
        INSTRUMENTATION = instrumentation
    }

    fun preClassCall(jarFile: JarFile, attribute: String, loader: ClassLoader) {
        if (jarFile.manifest.mainAttributes.containsKey(Attributes.Name(attribute))) {
            val preClass = Class.forName(jarFile.manifest.mainAttributes.getValue(attribute), true, loader)
            preClass.getMethod("premain", String::class.java, Instrumentation::class.java)
                .invoke(null, null, INSTRUMENTATION)
        }
    }

}