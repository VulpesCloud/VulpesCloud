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

package org.vulpesstudios.vulpescloud.node.players

import build.buf.gen.vulpescloud.events.v1.PlayerActions
import build.buf.gen.vulpescloud.events.v1.playerActionEvent
import build.buf.gen.vulpescloud.players.v1.*
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.event.EventsService

class PlayerActionServiceImpl : PlayerActionsServiceGrpcKt.PlayerActionsServiceCoroutineImplBase() {
    private val playerStub by lazy { Node.instance.localGrpcClient.playerAPI }

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
            true,
        )
        return SendTitleResponse.newBuilder().setSuccess(true).build()
    }

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
            true,
        )
        return KickPlayerResponse.newBuilder().setSuccess(true).build()
    }

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
            true,
        )
        return SendActionBarResponse.newBuilder().setSuccess(true).build()
    }

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
            true,
        )
        return ConnectPlayerResponse.newBuilder().setSuccess(true).build()
    }
}
