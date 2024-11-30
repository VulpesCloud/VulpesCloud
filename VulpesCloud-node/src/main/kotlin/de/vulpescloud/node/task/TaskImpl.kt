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

package de.vulpescloud.node.task

import de.vulpescloud.api.services.ClusterService
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.api.version.VersionInfo
import de.vulpescloud.node.Node
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path

open class TaskImpl(
    val name: String,
    val maxMemory: Int,
    val version: VersionInfo,
    val templates: List<String?>,
    val nodes: List<String?>,
    val maxPlayers: Int,
    val staticServices: Boolean,
    val minOnlineCount: Int,
    val maintenance: Boolean,
    val startPort: Int,
    val fallback: Boolean
) : Task {

    override fun details(): String? {
        return ""
    }

    override fun maintenance(): Boolean {
        return maintenance
    }

    override fun maxMemory(): Int {
        return maxMemory
    }

    override fun maxPlayers(): Int {
        return maxPlayers
    }

    override fun minOnlineCount(): Int {
        return minOnlineCount
    }

    override fun name(): String {
        return name
    }

    override fun nodes(): List<String?> {
        return nodes
    }

    override fun version(): VersionInfo {
        return version
    }

    override fun serviceCount(): Long? {
        return Node.serviceProvider.services()?.stream()?.filter { it.task().name() == name() }?.count()
    }

    override fun services(): List<ClusterService?>? {
        return Node.serviceProvider.services()?.stream()?.filter { it.task().name() == name() }?.toList()
    }

    override fun startPort(): Int {
        return startPort
    }

    override fun staticService(): Boolean {
        return staticServices
    }

    override fun templates(): List<String?> {
        return templates
    }

    override fun update() {
        Files.writeString(Path.of("local/tasks/${name()}.json"), JSONObject(this).toString())
    }

    override fun fallback(): Boolean {
        return fallback
    }
}