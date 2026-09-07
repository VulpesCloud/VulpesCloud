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

package org.vulpesstudios.vulpescloud.bridge

import kotlinx.coroutines.flow.Flow
import org.vulpesstudios.vulpescloud.api.rollout.RolloutOptions
import org.vulpesstudios.vulpescloud.api.rollout.RolloutProgress
import java.util.concurrent.CompletableFuture

interface RolloutAPI {

    interface RolloutFutureAPI {
        /**
         * Starts a rollout for [taskName]. [options] is optional - any field left unset falls back
         * to the Task attributes, then the cluster-wide rollout_config VirtualConfig (see
         * RolloutConfigResolver). Returns null if the rollout could not be started (task not found,
         * task already has an active rollout, or no eligible services to replace); check the node
         * log for the reason.
         */
        fun startRollout(
            taskName: String,
            options: RolloutOptions? = null,
        ): CompletableFuture<RolloutProgress?>

        fun getRolloutStatus(rolloutId: String): CompletableFuture<RolloutProgress?>

        fun getRolloutStatusByTask(taskName: String): CompletableFuture<RolloutProgress?>

        fun listActiveRollouts(): CompletableFuture<List<RolloutProgress>>

        /** Returns a human-readable result message (success or failure reason). */
        fun cancelRollout(rolloutId: String): CompletableFuture<String>
    }

    interface RolloutCoroutineAPI {
        /**
         * Starts a rollout for [taskName]. [options] is optional - any field left unset falls back
         * to the Task attributes, then the cluster-wide rollout_config VirtualConfig (see
         * RolloutConfigResolver). Returns null if the rollout could not be started (task not found,
         * task already has an active rollout, or no eligible services to replace); check the node
         * log for the reason.
         */
        suspend fun startRollout(taskName: String, options: RolloutOptions? = null): RolloutProgress?

        suspend fun getRolloutStatus(rolloutId: String): RolloutProgress?

        suspend fun getRolloutStatusByTask(taskName: String): RolloutProgress?

        suspend fun listActiveRollouts(): List<RolloutProgress>

        /** Returns a human-readable result message (success or failure reason). */
        suspend fun cancelRollout(rolloutId: String): String

        /**
         * Streams live updates for a single rollout until it reaches a finished status
         * (COMPLETED/FAILED/CANCELLED), starting with its current state.
         */
        fun streamRolloutProgress(rolloutId: String): Flow<RolloutProgress>
    }
}
