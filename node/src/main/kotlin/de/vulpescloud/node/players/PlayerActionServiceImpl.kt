package de.vulpescloud.node.players

import build.buf.gen.vulpescloud.events.v1.PlayerActions
import build.buf.gen.vulpescloud.events.v1.playerActionEvent
import build.buf.gen.vulpescloud.players.v1.ConnectPlayerRequest
import build.buf.gen.vulpescloud.players.v1.ConnectPlayerResponse
import build.buf.gen.vulpescloud.players.v1.KickPlayerRequest
import build.buf.gen.vulpescloud.players.v1.KickPlayerResponse
import build.buf.gen.vulpescloud.players.v1.PlayerActionsServiceGrpcKt
import build.buf.gen.vulpescloud.players.v1.SendActionBarRequest
import build.buf.gen.vulpescloud.players.v1.SendActionBarResponse
import build.buf.gen.vulpescloud.players.v1.SendMessageRequest
import build.buf.gen.vulpescloud.players.v1.SendMessageResponse
import build.buf.gen.vulpescloud.players.v1.SendTitleRequest
import build.buf.gen.vulpescloud.players.v1.SendTitleResponse
import build.buf.gen.vulpescloud.players.v1.getAllOnlinePlayersRequest
import de.vulpescloud.node.Node
import de.vulpescloud.node.event.EventsService
import de.vulpescloud.node.grpc.security.annotations.RequiresPermission

class PlayerActionServiceImpl : PlayerActionsServiceGrpcKt.PlayerActionsServiceCoroutineImplBase() {
    private val playerStub by lazy { Node.instance.localGrpcClient.playerAPI }

    @RequiresPermission("players.actions.sendMessage")
    override suspend fun sendMessage(request: SendMessageRequest): SendMessageResponse {
        val player =
            playerStub.getAllOnlinePlayers(getAllOnlinePlayersRequest {}).onlinePlayersList.find {
                it.uuid == request.uuid
            }

        if (player == null) return SendMessageResponse.newBuilder().setSuccess(false).build()

        EventsService.publish(
            playerActionEvent {
                this.player = player
                this.action = PlayerActions.MESSAGE
                this.data.put("message", request.message)
            },
            true,
        )
        return SendMessageResponse.newBuilder().setSuccess(true).build()
    }

    @RequiresPermission("players.actions.sendTitle")
    override suspend fun sendTitle(request: SendTitleRequest): SendTitleResponse {
        val player =
            playerStub.getAllOnlinePlayers(getAllOnlinePlayersRequest {}).onlinePlayersList.find {
                it.uuid == request.uuid
            }

        if (player == null) return SendTitleResponse.newBuilder().setSuccess(false).build()

        EventsService.publish(
            playerActionEvent {
                this.player = player
                this.action = PlayerActions.TITLE
                this.data.apply {
                    put("title", request.title)
                    put("subtitle", request.subtitle)
                }
            },
            true
        )
        return SendTitleResponse.newBuilder().setSuccess(true).build()
    }

    @RequiresPermission("players.actions.kickPlayer")
    override suspend fun kickPlayer(request: KickPlayerRequest): KickPlayerResponse {
        val player =
            playerStub.getAllOnlinePlayers(getAllOnlinePlayersRequest {}).onlinePlayersList.find {
                it.uuid == request.uuid
            }

        if (player == null) return KickPlayerResponse.newBuilder().setSuccess(false).build()

        EventsService.publish(
            playerActionEvent {
                this.player = player
                this.action = PlayerActions.KICK
                this.data.apply { put("reason", request.reason) }
            },
            true
        )
        return KickPlayerResponse.newBuilder().setSuccess(true).build()
    }

    @RequiresPermission("players.actions.sendActionBar")
    override suspend fun sendActionBar(request: SendActionBarRequest): SendActionBarResponse {
        val player =
            playerStub.getAllOnlinePlayers(getAllOnlinePlayersRequest {}).onlinePlayersList.find {
                it.uuid == request.uuid
            }

        if (player == null) return SendActionBarResponse.newBuilder().setSuccess(false).build()

        EventsService.publish(
            playerActionEvent {
                this.player = player
                this.action = PlayerActions.ACTION_BAR
                this.data.apply { put("message", request.message) }
            },
            true
        )
        return SendActionBarResponse.newBuilder().setSuccess(true).build()
    }

    @RequiresPermission("players.actions.connectPlayer")
    override suspend fun connectPlayer(request: ConnectPlayerRequest): ConnectPlayerResponse {
        val player =
            playerStub.getAllOnlinePlayers(getAllOnlinePlayersRequest {}).onlinePlayersList.find {
                it.uuid == request.uuid
            }

        if (player == null) return ConnectPlayerResponse.newBuilder().setSuccess(false).build()

        EventsService.publish(
            playerActionEvent {
                this.player = player
                this.action = PlayerActions.CONNECT
                this.data.apply { put("targetServer", request.targetServer) }
            },
            true
        )
        return ConnectPlayerResponse.newBuilder().setSuccess(true).build()
    }
}
