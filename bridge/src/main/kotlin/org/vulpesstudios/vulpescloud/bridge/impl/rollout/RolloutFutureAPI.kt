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
import org.vulpesstudios.vulpescloud.bridge.FutureHelper.toCompletableFuture
import org.vulpesstudios.vulpescloud.bridge.RolloutAPI
import org.vulpesstudios.vulpescloud.wrapper.Wrapper
import java.util.concurrent.CompletableFuture

class RolloutFutureAPI : RolloutAPI.RolloutFutureAPI {

    private val rolloutStub = Wrapper.instance.grpcClient.futureRolloutAPI

    override fun startRollout(
        taskName: String,
        options: RolloutOptions?,
    ): CompletableFuture<RolloutProgress?> {
        return rolloutStub
            .startRollout(
                startRolloutRequest {
                    this.taskName = taskName
                    options?.let { this.options = it.toDefinition() }
                }
            )
            .toCompletableFuture()
            .thenApply { response ->
                if (!response.success) {
                    println("VC-Bridge: Failed to start rollout for task $taskName: ${response.error}")
                    null
                } else {
                    RolloutProgress.fromDefinition(response.rollout)
                }
            }
    }

    override fun getRolloutStatus(rolloutId: String): CompletableFuture<RolloutProgress?> {
        return rolloutStub
            .getRolloutStatus(getRolloutStatusRequest { this.rolloutId = rolloutId })
            .toCompletableFuture()
            .handle { response, _ -> response?.let { RolloutProgress.fromDefinition(it.rollout) } }
    }

    override fun getRolloutStatusByTask(taskName: String): CompletableFuture<RolloutProgress?> {
        return rolloutStub
            .getRolloutStatus(getRolloutStatusRequest { this.taskName = taskName })
            .toCompletableFuture()
            .handle { response, _ -> response?.let { RolloutProgress.fromDefinition(it.rollout) } }
    }

    override fun listActiveRollouts(): CompletableFuture<List<RolloutProgress>> {
        return rolloutStub
            .listActiveRollouts(listActiveRolloutsRequest {})
            .toCompletableFuture()
            .thenApply { response -> response.rolloutsList.map { RolloutProgress.fromDefinition(it) } }
    }

    override fun cancelRollout(rolloutId: String): CompletableFuture<String> {
        return rolloutStub
            .cancelRollout(cancelRolloutRequest { this.rolloutId = rolloutId })
            .toCompletableFuture()
            .thenApply { response -> response.message }
    }
}
