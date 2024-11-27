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
    private val redis = Node.instance!!.getRC()
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
            redis?.setHashField("VulpesCloud-HeartBeat", Node.nodeConfig!!.name, "$runtime")
            if (redis?.getHashField("VulpesCloud-HeartBeat", Node.nodeConfig!!.name)?.toInt() != runtime) {
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