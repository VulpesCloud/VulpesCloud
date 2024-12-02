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

package de.vulpescloud.node.networking.redis

import de.vulpescloud.node.Node
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

class RedisConnectionChecker : CoroutineScope {

    companion object {
        lateinit var instance: RedisConnectionChecker
    }

    private val job = Job()
    private val redis = Node.instance.getRC()
    private var runtime = 0
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job
    private val logger = LoggerFactory.getLogger(RedisConnectionChecker::class.java)

    init {
        instance = this
    }

    fun cancel() {
        job.cancel()
    }

    fun schedule() = launch {
        while (true) {
            runtime += 1
            redis?.setHashField("VulpesCloud-HeartBeat", Node.instance.config.name, "$runtime")
            if (redis?.getHashField("VulpesCloud-HeartBeat", Node.instance.config.name)?.toInt() != runtime) {
                logger.error("REDIS CONNECTION SEEMS TO BE LOST!")
                logger.error("REDIS CONNECTION SEEMS TO BE LOST!")
                logger.error("REDIS CONNECTION SEEMS TO BE LOST!")
                logger.error("REDIS CONNECTION SEEMS TO BE LOST!")
                logger.error("REDIS CONNECTION SEEMS TO BE LOST!")
                logger.error("REDIS CONNECTION SEEMS TO BE LOST!")
                logger.error("REDIS CONNECTION SEEMS TO BE LOST!")
                logger.error("REDIS CONNECTION SEEMS TO BE LOST!")
            } else {
                logger.debug(" || RedisConnectionListener -> Redis Connection is still valid. ||")
            }
            delay(60000)
        }
    }
}