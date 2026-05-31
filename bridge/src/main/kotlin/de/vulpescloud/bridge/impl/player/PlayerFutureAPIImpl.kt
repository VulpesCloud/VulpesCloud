package de.vulpescloud.bridge.impl.player

import build.buf.gen.vulpescloud.players.v1.getAllOnlinePlayersRequest
import build.buf.gen.vulpescloud.players.v1.getOfflinePlayersRequest
import de.vulpescloud.api.players.OfflinePlayer
import de.vulpescloud.api.players.OnlinePlayer
import de.vulpescloud.api.players.toAPI
import de.vulpescloud.bridge.FutureHelper.toCompletableFuture
import de.vulpescloud.bridge.PlayerAPI
import de.vulpescloud.wrapper.Wrapper
import java.util.concurrent.CompletableFuture

class PlayerFutureAPIImpl : PlayerAPI.PlayerFutureAPI {
    private val playerStub = Wrapper.instance.grpcClient.futurePlayerAPI

    override fun getAllOnlinePlayers(): CompletableFuture<List<OnlinePlayer>> {
        return playerStub
            .getAllOnlinePlayers(getAllOnlinePlayersRequest {})
            .toCompletableFuture()
            .thenApply { player -> player.onlinePlayersList.map { it.toAPI() } }
    }

    override fun getOnlinePlayerByUUID(uuid: String): CompletableFuture<OnlinePlayer?> {
        return getAllOnlinePlayers().thenApply { it.find { player -> player.uuid == uuid } }
    }

    override fun getOnlinePlayerByName(name: String): CompletableFuture<OnlinePlayer?> {
        return getAllOnlinePlayers().thenApply { it.find { player -> player.name == name } }
    }

    override fun getAllRegisteredPlayers(): CompletableFuture<List<OfflinePlayer>> {
        return playerStub
            .getAllOfflinePlayers(getOfflinePlayersRequest {})
            .toCompletableFuture()
            .thenApply { player -> player.offlinePlayersList.map { it.toAPI() } }
    }

    override fun getRegisteredPlayerByName(name: String): CompletableFuture<OfflinePlayer?> {
        return getAllRegisteredPlayers().thenApply { it.find { player -> player.name == name } }
    }

    override fun getRegisteredPlayerByUUID(uuid: String): CompletableFuture<OfflinePlayer?> {
        return getAllRegisteredPlayers().thenApply { it.find { player -> player.uuid == uuid } }
    }
}
