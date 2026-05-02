package de.vulpescloud.bridge.impl.player

import build.buf.gen.vulpescloud.players.v1.getAllOnlinePlayersRequest
import build.buf.gen.vulpescloud.players.v1.getOfflinePlayersRequest
import de.vulpescloud.api.players.OfflinePlayer
import de.vulpescloud.api.players.OnlinePlayer
import de.vulpescloud.api.players.toAPI
import de.vulpescloud.bridge.PlayerAPI
import de.vulpescloud.wrapper.Wrapper

class PlayerCoroutineAPIImpl : PlayerAPI.PlayerCoroutineAPI {

    private val playerStub = Wrapper.instance.grpcClient.playerAPI

    override suspend fun getAllOnlinePlayers(): List<OnlinePlayer> {
        return playerStub.getAllOnlinePlayers(getAllOnlinePlayersRequest {}).onlinePlayersList.map {
            it.toAPI()
        }
    }

    override suspend fun getOnlinePlayerByName(name: String): OnlinePlayer? {
        return getAllOnlinePlayers().find { it.name == name }
    }

    override suspend fun getOnlinePlayerByUUID(uuid: String): OnlinePlayer? {
        return getAllOnlinePlayers().find { it.uuid == uuid }
    }

    override suspend fun getAllRegisteredPlayers(): List<OfflinePlayer> {
        return playerStub.getAllOfflinePlayers(getOfflinePlayersRequest {}).offlinePlayersList.map {
            it.toAPI()
        }
    }

    override suspend fun getRegisteredPlayerByName(name: String): OfflinePlayer? {
        return getAllRegisteredPlayers().find { it.name == name }
    }

    override suspend fun getRegisteredPlayerByUUID(uuid: String): OfflinePlayer? {
        return getAllRegisteredPlayers().find { it.uuid == uuid }
    }
}
