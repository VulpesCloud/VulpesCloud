package de.vulpescloud.node.networking.redis


import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeShutdown
import org.json.JSONObject
import org.slf4j.LoggerFactory
import redis.clients.jedis.BinaryJedisPubSub
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.exceptions.JedisConnectionException
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class RedisController : BinaryJedisPubSub(), Runnable {

    private val logger = LoggerFactory.getLogger(RedisController::class.java)

    private val jedisPool: JedisPool
    private var channelsInByte: Array<ByteArray>
    private val isConnectionBroken = AtomicBoolean(true)
    private val isConnecting = AtomicBoolean(false)

    init {

        val jConfig = JedisPoolConfig()
        val maxConnections = 10

        jConfig.maxTotal = maxConnections
        jConfig.maxIdle = maxConnections
        jConfig.minIdle = 1
        jConfig.blockWhenExhausted = true

        val password = Node.nodeConfig!!.redis?.password
        jedisPool = if (password?.isEmpty() == true) {
            JedisPool(
                jConfig,
                Node.nodeConfig!!.redis?.hostname,
                Node.nodeConfig!!.redis?.port ?: 6379,
                9000,
                false
            )
        } else {
            JedisPool(
                jConfig,
                Node.nodeConfig!!.redis?.hostname,
                Node.nodeConfig!!.redis?.port ?: 6379,
                9000,
                password,
                false
            )
        }

        channelsInByte = setupChannels()
        Thread(this).start() // Start the connection thread
    }

    override fun run() {
        if (!isConnectionBroken.get() || isConnecting.get()) {
            return
        }
        logger.info("Connecting to Redis server...")
        isConnecting.set(true)
        try {
            jedisPool.resource.use { jedis ->
                isConnectionBroken.set(false)
                logger.info("Connection to Redis server has been established!")
                jedis.subscribe(this, *channelsInByte)
            }
        } catch (e: Exception) {
            isConnecting.set(false)
            isConnectionBroken.set(true)
            logger.error("Connection to Redis server has failed! Please check your details in the configuration. $e")
            NodeShutdown.nodeShutdown(true)
        }
    }

    fun sendMessage(message: String, channel: String) {
        val json = JSONObject()
        json.put("messages", message)
        json.put("action", "TestCloud")
        json.put("date", System.currentTimeMillis())
        finishSendMessage(json, channel)
    }

    private fun finishSendMessage(json: JSONObject, channel: String) {
        try {
            val message = json.toString().toByteArray(StandardCharsets.UTF_8)
            jedisPool.resource.use { jedis ->
                try {
                    jedis.publish(channel.toByteArray(StandardCharsets.UTF_8), message)
                } catch (e: Exception) {
                    logger.error("Error sending redis message! $e")
                }
            }
        } catch (exception: JedisConnectionException) {
            logger.error("Jedis connection exception occurred $exception")
        }
    }

    fun testHashOperation() {
        println("Testing hash operation...")
        try {
            jedisPool.resource.use { jedis ->
                val testHashName = "testHash_${System.currentTimeMillis()}" // Unique hash name
                jedis.hset(testHashName, "testField", "testValue")
                println("Hash operation successful for $testHashName")
            }
        } catch (e: Exception) {
            println("Exception during hash operation: ${e.message}")
            e.printStackTrace()
        }
    }

    fun removeFromListByValue(listName: String, value: String) {
        jedisPool.resource.use { jedis ->
            jedis.lrem(listName, 0, value)
        }
    }

    fun setHashField(hashName: String, fieldName: String, value: String) {
        try {
            jedisPool.resource.use { jedis ->
                jedis.hset(hashName, fieldName, value)
            }
        } catch (e: Exception) {
            println("Exception occurred during setting hash field: ${e.message}")
            e.printStackTrace()
        }
    }

    fun deleteHashField(hashName: String, fieldName: String) {
        jedisPool.resource.use { jedis ->
            jedis.hdel(hashName, fieldName)
        }
    }

    fun createHashFromMap(hashName: String, values: Map<String, String>) {
        if (values.isEmpty()) {
            return
        }

        try {
            jedisPool.resource.use { jedis ->
                jedis.hmset(hashName, values)
            }
        } catch (e: Exception) {
            println("Exception occurred during setting hash field: ${e.message}")
            e.printStackTrace()
        }
    }

    fun createListFromList(listName: String, values: List<String>) {
        jedisPool.resource.use { jedis ->
            jedis.rpush(listName, *values.toTypedArray())
        }
    }

    fun deleteEntriesFromArray(hashName: String, keys: Array<String>) {
        if (keys.isNotEmpty()) {
            jedisPool.resource.use { jedis ->
                jedis.hdel(hashName, *keys)
            }
        }
    }

    fun deleteHash(hashName: String) {
        jedisPool.resource.use { jedis ->
            jedis.del(hashName)
        }
    }

    fun addToList(listName: String, values: String) {
        jedisPool.resource.use { jedis ->
            jedis.rpush(listName, values)
        }
    }

    fun setListValue(listName: String, index: Int, value: String) {
        jedisPool.resource.use { jedis ->
            val listLength = jedis.llen(listName)
            if (index >= listLength) {
                System.err.println("Error: Index $index does not exist in the list $listName.")
            } else {
                jedis.lset(listName, index.toLong(), value)
            }
        }
    }

    fun getHashValuesAsPair(hashName: String): Map<String, String> {
        val values = mutableMapOf<String, String>()
        jedisPool.resource.use { jedis ->
            val keys = jedis.hkeys(hashName)
            for (key in keys) {
                values[key] = jedis.hget(hashName, key)
            }
        }
        return values
    }

    fun deleteHashFieldByValue(hashName: String, value: String) {
        jedisPool.resource.use { jedis ->
            val keys = jedis.hkeys(hashName)
            for (key in keys) {
                if (jedis.hget(hashName, key) == value) {
                    jedis.hdel(hashName, key)
                }
            }
        }
    }

    fun removeFromListByIndex(listName: String, index: Int) {
        jedisPool.resource.use { jedis ->
            val listLength = jedis.llen(listName)
            if (index >= listLength) {
                System.err.println("Error: Index $index does not exist in the list $listName.")
            } else {
                val tempKey = UUID.randomUUID().toString()
                jedis.lset(listName, index.toLong(), tempKey)
                jedis.lrem(listName, 0, tempKey)
            }
        }
    }

    fun removeFromList(listName: String, value: String) {
        jedisPool.resource.use { jedis ->
            jedis.lrem(listName, 0, value)
        }
    }

    fun deleteList(listName: String) {
        jedisPool.resource.use { jedis ->
            jedis.del(listName)
        }
    }

    fun setString(key: String, value: String) {
        try {
            jedisPool.resource.use { jedis ->
                jedis.set(key, value)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getString(key: String): String? {
        return jedisPool.resource.use { jedis ->
            jedis.get(key)
        }
    }

    fun deleteString(key: String) {
        jedisPool.resource.use { jedis ->
            jedis.del(key)
        }
    }

    fun getHashField(hashName: String, fieldName: String): String? {
        return jedisPool.resource.use { jedis ->
            jedis.hget(hashName, fieldName)
        }
    }

    fun getHashFieldNameByValue(hashName: String, value: String): String? {
        jedisPool.resource.use { jedis ->
            val keys = jedis.hkeys(hashName)
            for (key in keys) {
                if (jedis.hget(hashName, key) == value) {
                    return key
                }
            }
        }
        return null
    }

    fun getAllHashFields(hashName: String): Set<String>? {
        return jedisPool.resource.use { jedis ->
            jedis.hkeys(hashName)
        }
    }

    fun getAllHashValues(hashName: String): List<String>? {
        return jedisPool.resource.use { jedis ->
            jedis.hvals(hashName)
        }
    }

    fun getList(listName: String): List<String>? {
        return jedisPool.resource.use { jedis ->
            jedis.lrange(listName, 0, -1)
        }
    }

    fun getHashFieldNamesByValue(hashName: String, value: String): List<String> {
        val fieldNames = mutableListOf<String>()
        jedisPool.resource.use { jedis ->
            val keys = jedis.keys(hashName)
            for (key in keys) {
                val fieldsAndValues = jedis.hgetAll(key)
                for (entry in fieldsAndValues.entries) {
                    if (entry.value == value) {
                        fieldNames.add(entry.key)
                    }
                }
            }
        }
        return fieldNames
    }


    fun shutdown() {
        if (this.isSubscribed) {
            try {
                this.unsubscribe()
            } catch (e: Exception) {
                logger.error("Something went wrong during unsubscribing... $e")
            }
        }
        jedisPool.close()
    }

    private fun setupChannels(): Array<ByteArray> {
        val channels = listOf("global", "messaging", "friends", "utils", "other")
        return Array(channels.size) { channels[it].toByteArray(StandardCharsets.UTF_8) }
    }

    fun isRedisConnectionOffline(): Boolean {
        return isConnectionBroken.get()
    }

    fun getJedisPool(): JedisPool {
        return jedisPool
    }
}
