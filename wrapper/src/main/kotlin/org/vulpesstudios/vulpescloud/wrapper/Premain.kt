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

import java.lang.instrument.Instrumentation
import java.util.jar.Attributes
import java.util.jar.JarFile

object Premain {

    @JvmStatic lateinit var INSTRUMENTATION: Instrumentation

    @JvmStatic
    fun premain(args: String?, instrumentation: Instrumentation) {
        INSTRUMENTATION = instrumentation
    }

    fun preClassCall(jarFile: JarFile, attribute: String, loader: ClassLoader) {
        if (jarFile.manifest.mainAttributes.containsKey(Attributes.Name(attribute))) {
            val preClass =
                Class.forName(jarFile.manifest.mainAttributes.getValue(attribute), true, loader)
            preClass
                .getMethod("premain", String::class.java, Instrumentation::class.java)
                .invoke(null, null, INSTRUMENTATION)
        }
    }
}
