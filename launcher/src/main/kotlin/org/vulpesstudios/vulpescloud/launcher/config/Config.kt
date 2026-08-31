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

package org.vulpesstudios.vulpescloud.launcher.config

import com.electronwill.nightconfig.core.file.CommentedFileConfig
import org.vulpesstudios.vulpescloud.launcher.util.FileSystemUtil
import java.nio.file.Path

class Config {

    private val config = CommentedFileConfig.builder("launcher/launcher-config.toml")
        .sync()
        .build()

    init {
        if (!Path.of("launcher/launcher-config.toml").toFile().exists()) {
            FileSystemUtil.copyClassPathFile(this::class.java.classLoader, "launcher-config.toml", "launcher/launcher-config.toml")
        }
        config.load()

        try {
            autoUpdatesEnabled()
        } catch (e: NullPointerException) {
            config.set<Boolean>("auto-updates.enabled", true)
            config.save()
        }

        try {
            autoUpdatesBranch()
        } catch (e: NullPointerException) {
            config.set<Boolean>("auto-updates.branch", "v3")
            config.save()
        }
    }

    fun debug() {
        config.set<Boolean>("auto-updates.enabled", true)
        config.set<String>("auto-updates.branch", "development")
        config.save()
    }

    fun autoUpdatesEnabled(): Boolean = config.get("auto-updates.enabled")
    fun autoUpdatesBranch(): String = config.get("auto-updates.branch")


}