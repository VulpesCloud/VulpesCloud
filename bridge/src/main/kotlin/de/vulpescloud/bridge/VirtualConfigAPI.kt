package de.vulpescloud.bridge

import de.vulpescloud.api.virtualconfig.VirtualConfig
import java.util.concurrent.CompletableFuture
import kotlinx.serialization.KSerializer

interface VirtualConfigAPI {

    interface VirtualConfigCoroutineAPI {
        suspend fun <T> getCustomConfigObject(
            name: String,
            serializer: KSerializer<T>,
            forceGet: Boolean = false,
        ): T?

        suspend fun getCustomConfig(name: String, forceGet: Boolean = false): VirtualConfig?

        suspend fun <T> updateCustomConfig(name: String, serializer: KSerializer<T>, value: T)

        suspend fun updateCustomConfig(config: VirtualConfig)

        suspend fun updateLocalConfigFromDatabase(name: String)
    }

    interface VirtualConfigFutureAPI {
        fun <T> getCustomConfigObject(
            name: String,
            serializer: KSerializer<T>,
            forceGet: Boolean = false,
        ): CompletableFuture<T?>

        fun getCustomConfig(
            name: String,
            forceGet: Boolean = false,
        ): CompletableFuture<VirtualConfig?>

        fun <T> updateCustomConfig(name: String, serializer: KSerializer<T>, value: Any)

        fun updateCustomConfig(config: VirtualConfig)

        fun updateLocalConfigFromDatabase(name: String)
    }
}
