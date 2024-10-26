package io.github.thecguygithub.node.networking.redis

import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class RedisConnectionChecker : CoroutineScope {

    companion object {
        lateinit var instance: RedisConnectionChecker
    }

    private val job = Job()
    private val redis = Node.instance!!.getRC()
    private var runtime = 0
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    init {
        instance = this
    }

    fun cancel() {
        job.cancel()
    }

    fun schedule() = launch {
        while (true) {
            redis?.setHashField("VulpesCloud-HeartBeat", Node.nodeConfig!!.name, "$runtime")
            runtime += 1
            if (redis?.getHashField("VulpesCloud-HeartBeat", Node.nodeConfig!!.name)?.toInt() != runtime) {
                Logger.instance.error("REDIS CONNECTION SEEMS TO BE LOST!")
                Logger.instance.error("REDIS CONNECTION SEEMS TO BE LOST!")
                Logger.instance.error("REDIS CONNECTION SEEMS TO BE LOST!")
                Logger.instance.error("REDIS CONNECTION SEEMS TO BE LOST!")
                Logger.instance.error("REDIS CONNECTION SEEMS TO BE LOST!")
                Logger.instance.error("REDIS CONNECTION SEEMS TO BE LOST!")
                Logger.instance.error("REDIS CONNECTION SEEMS TO BE LOST!")
                Logger.instance.error("REDIS CONNECTION SEEMS TO BE LOST!")
            } else {
                Logger.instance.debug(" || RedisConnectionListener -> Redis Connection is still valid. ||")
            }
            delay(60000)
        }
    }
}