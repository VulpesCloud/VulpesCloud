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

package de.vulpescloud.api.services

import lombok.SneakyThrows
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


abstract class ClusterServiceProvider {

    @SneakyThrows
    fun services(): MutableList<ClusterService>? {
        return servicesAsync()?.get(5, TimeUnit.SECONDS)
    }

    abstract fun servicesAsync(): CompletableFuture<MutableList<ClusterService>>?


    @SneakyThrows
    fun findByID(id: UUID?): ClusterService? {
        return findAsync(id)!!.get(5, TimeUnit.SECONDS)
    }

    abstract fun findAsync(id: UUID?): CompletableFuture<ClusterService?>?

    @SneakyThrows
    fun findByName(name: String?): ClusterService? {
        return findAsync(name)!!.get(5, TimeUnit.SECONDS)
    }

    abstract fun findAsync(name: String?): CompletableFuture<ClusterService?>?

    @SneakyThrows
    fun findByFilter(filter: ClusterServiceFilter): List<ClusterService?>? {
        return findAsync(filter)[5, TimeUnit.SECONDS]
    }

    abstract fun findAsync(filter: ClusterServiceFilter): CompletableFuture<List<ClusterService?>?>

    abstract fun factory(): ClusterServiceFactory?

}