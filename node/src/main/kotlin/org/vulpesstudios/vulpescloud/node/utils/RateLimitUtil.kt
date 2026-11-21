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

package org.vulpesstudios.vulpescloud.node.utils

import java.util.concurrent.ConcurrentHashMap

class RateLimitUtil(
    private val maxLogs: Int = 3,
    private val windowMs: Long = 20 * 60 * 1000L
) {
    private val timestamps = ConcurrentHashMap<String, ArrayDeque<Long>>()

    fun tryAcquire(key: String): Boolean {
        val now = System.currentTimeMillis()
        val deque = timestamps.computeIfAbsent(key) { ArrayDeque() }
        synchronized(deque) {
            while (deque.isNotEmpty() && now - deque.first() >= windowMs) {
                deque.removeFirst()
            }
            if (deque.size >= maxLogs) return false
            deque.addLast(now)
            return true
        }
    }
}
