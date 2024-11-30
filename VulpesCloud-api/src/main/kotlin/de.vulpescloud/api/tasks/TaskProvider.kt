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

package de.vulpescloud.api.tasks

import de.vulpescloud.api.version.VersionInfo
import lombok.SneakyThrows
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


abstract class TaskProvider {

    abstract fun tasksAsync(): CompletableFuture<MutableSet<Task>>?

    @SneakyThrows
    fun tasks(): MutableSet<Task>? {
        return tasksAsync()?.get(5, TimeUnit.SECONDS)
    }

    abstract fun existsAsync(task: String?): CompletableFuture<Boolean>

    @SneakyThrows
    fun exists(task: String?): Boolean {
        return existsAsync(task)[5, TimeUnit.SECONDS]
    }

    abstract fun deleteAsync(task: String?): CompletableFuture<Optional<String?>?>

    @SneakyThrows
    fun delete(task: String?): Optional<String?>? {
        return deleteAsync(task)[5, TimeUnit.SECONDS]
    }

    abstract fun createAsync(
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
    ): CompletableFuture<Optional<String?>?>

    @SneakyThrows
    fun create(
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
    ): Optional<String?>? {
        return createAsync(name, templates, nodes, version, maxMemory, staticService, minOnlineCount, maintenance, maxPlayers, startPort, fallback)[5, TimeUnit.SECONDS]
    }

    abstract fun findAsync(task: String): CompletableFuture<Task?>

    @SneakyThrows
    fun findByName(task: String): Task? {
        return findAsync(task)[5, TimeUnit.SECONDS]
    }
}