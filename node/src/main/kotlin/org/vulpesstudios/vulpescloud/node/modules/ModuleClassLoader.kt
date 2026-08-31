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

package org.vulpesstudios.vulpescloud.node.modules

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
            "kotlinx.",
            "org.jetbrains.exposed.",
            "java.",
            "javax.",
            "sun.",
            "com.sun.",
            "net.kyori.adventure.",
            "org.slf4j.",
            "ch.qos.logback.",
            "org.vulpesstudios.vulpescloud.",
            "io.grpc.",
            "io.netty.",
            "org.apache.arrow.",
            "com.influxdb.",
            "com.google.protobuf.",
            "com.google.common.",
            "org.apache.arrow.flatbuf.",
            "io.grpc.netty.",
            "org.json",
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
