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

package org.vulpesstudios.vulpescloud.bridge.impl.rollout

import build.buf.gen.vulpescloud.rollout.v1.*
import org.vulpesstudios.vulpescloud.api.rollout.RolloutOptions
import org.vulpesstudios.vulpescloud.api.rollout.RolloutProgress
import org.vulpesstudios.vulpescloud.bridge.RolloutAPI
import org.vulpesstudios.vulpescloud.wrapper.Wrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RolloutCoroutineAPI : RolloutAPI.RolloutCoroutineAPI {

    private val rolloutStub = Wrapper.instance.grpcClient.rolloutAPI

    override suspend fun startRollout(taskName: String, options: RolloutOptions?): RolloutProgress? {
        val response =
            rolloutStub.startRollout(
                startRolloutRequest {
                    this.taskName = taskName
                    options?.let { this.options = it.toDefinition() }
                }
            )

        if (!response.success) {
            println("VC-Bridge: Failed to start rollout for task $taskName: ${response.error}")
            return null
        }

        return RolloutProgress.fromDefinition(response.rollout)
    }

    override suspend fun getRolloutStatus(rolloutId: String): RolloutProgress? {
        return try {
            RolloutProgress.fromDefinition(
                rolloutStub.getRolloutStatus(getRolloutStatusRequest { this.rolloutId = rolloutId }).rollout
            )
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getRolloutStatusByTask(taskName: String): RolloutProgress? {
        return try {
            RolloutProgress.fromDefinition(
                rolloutStub.getRolloutStatus(getRolloutStatusRequest { this.taskName = taskName }).rollout
            )
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun listActiveRollouts(): List<RolloutProgress> {
        return rolloutStub
            .listActiveRollouts(listActiveRolloutsRequest {})
            .rolloutsList
            .map { RolloutProgress.fromDefinition(it) }
    }

    override suspend fun cancelRollout(rolloutId: String): String {
        val response = rolloutStub.cancelRollout(cancelRolloutRequest { this.rolloutId = rolloutId })
        return response.message
    }

    override fun streamRolloutProgress(rolloutId: String): Flow<RolloutProgress> {
        return rolloutStub
            .streamRolloutProgress(streamRolloutProgressRequest { this.rolloutId = rolloutId })
            .map { RolloutProgress.fromDefinition(it) }
    }
}
