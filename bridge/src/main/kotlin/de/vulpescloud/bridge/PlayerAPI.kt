package de.vulpescloud.bridge

import de.vulpescloud.api.players.OfflinePlayer
import de.vulpescloud.api.players.OnlinePlayer
import java.util.concurrent.CompletableFuture

interface PlayerAPI {

    interface PlayerCoroutineAPI {

        suspend fun getAllOnlinePlayers(): List<OnlinePlayer>

        suspend fun getOnlinePlayerByName(name: String): OnlinePlayer?

        suspend fun getOnlinePlayerByUUID(uuid: String): OnlinePlayer?

        suspend fun getAllRegisteredPlayers(): List<OfflinePlayer>

        suspend fun getRegisteredPlayerByName(name: String): OfflinePlayer?

        suspend fun getRegisteredPlayerByUUID(uuid: String): OfflinePlayer?

    }

    interface PlayerFutureAPI {

        fun getAllOnlinePlayers(): CompletableFuture<List<OnlinePlayer>>

        fun getOnlinePlayerByName(name: String): CompletableFuture<OnlinePlayer?>

        fun getOnlinePlayerByUUID(uuid: String): CompletableFuture<OnlinePlayer?>

        fun getAllRegisteredPlayers(): CompletableFuture<List<OfflinePlayer>>

        fun getRegisteredPlayerByName(name: String): CompletableFuture<OfflinePlayer?>

        fun getRegisteredPlayerByUUID(uuid: String): CompletableFuture<OfflinePlayer?>
    }

}
