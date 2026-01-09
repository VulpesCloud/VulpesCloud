package de.vulpescloud.node.modules

import java.net.URL
import java.net.URLClassLoader

class ModuleClassLoader(
    urls: Array<URL>,
    parent: ClassLoader,
    private val classLoaders: Map<String, ModuleClassLoader>,
) : URLClassLoader(urls, parent) {

    private val parentFirstPackages =
        setOf(
            "kotlin.",
            "org.jetbrains.exposed.",
            "java.",
            "javax.",
            "sun.",
            "com.sun.",
            "net.kyori.adventure.",
            "org.slf4j.",
            "ch.qos.logback.",
            "de.vulpescloud."
        )

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        return loadClassInternal(name, resolve, mutableSetOf())
    }

    private fun loadClassInternal(
        name: String,
        resolve: Boolean,
        visited: MutableSet<ModuleClassLoader>,
    ): Class<*> {
        if (!visited.add(this)) {
            throw ClassNotFoundException(name)
        }

        if (shouldLoadFromParentFirst(name)) {
            try {
                return parent.loadClass(name)
            } catch (_: ClassNotFoundException) {}
        }

        findLoadedClass(name)?.let {
            return it
        }

        try {
            val clazz = findClass(name)
            if (resolve) resolveClass(clazz)
            return clazz
        } catch (_: ClassNotFoundException) {}

        try {
            val clazz = parent.loadClass(name)
            return clazz
        } catch (_: ClassNotFoundException) {}

        for (loader in classLoaders.values) {
            if (loader === this) continue
            try {
                return loader.loadClassInternal(name, resolve, visited)
            } catch (_: ClassNotFoundException) {}
        }
        throw ClassNotFoundException(name)
    }

    private fun shouldLoadFromParentFirst(name: String): Boolean {
        return parentFirstPackages.any { name.startsWith(it) }
    }
}