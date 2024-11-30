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

import de.vulpescloud.api.tasks.Task
import de.vulpescloud.api.tasks.TaskProvider
import de.vulpescloud.api.version.VersionInfo
import de.vulpescloud.node.Node
import java.util.*
import java.util.concurrent.CompletableFuture

class TaskProvider : TaskProvider() {

    private var tasks: MutableSet<Task> = TaskFactory.readGroups().toMutableSet()

    override fun createAsync(
        name: String,
        templates: List<String?>,
        nodes: List<String?>,
        version: VersionInfo,
        maxMemory: Int,
        staticService: Boolean,
        minOnlineCount: Int,
        maintenance: Boolean,
        maxPlayers: Int,
        startPort: Int,
        fallback: Boolean
    ): CompletableFuture<Optional<String?>?> {
        val taskFuture = CompletableFuture<Optional<String?>?>()
        val task = TaskImpl(
            name,
            maxMemory,
            version,
            templates,
            nodes,
            maxPlayers,
            staticService,
            minOnlineCount,
            maintenance,
            startPort,
            fallback
        )
        Node.instance?.getRC()?.sendMessage("TASK;CREATE;${task}", "vulpescloud-event-task-update")
        return taskFuture
    }

    override fun deleteAsync(task: String?): CompletableFuture<Optional<String?>?> {
        val taskFuture = CompletableFuture<Optional<String?>?>()
        Node.instance!!.getRC()?.sendMessage("TASK;DELETE;$task", "vulpescloud-event-task-update")
        return taskFuture
    }

    override fun existsAsync(task: String?): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(tasks.stream().anyMatch { it.name().equals(task, true) })
    }

    override fun findAsync(task: String): CompletableFuture<Task?> {
        return CompletableFuture.completedFuture(tasks.stream().filter { it.name().equals(task, true)}.findFirst().orElse(null))
    }

    override fun tasksAsync(): CompletableFuture<MutableSet<Task>>? {
        return CompletableFuture.completedFuture(tasks)
    }

    fun reload() {
        this.tasks = TaskFactory.readGroups().toMutableSet()
    }
}