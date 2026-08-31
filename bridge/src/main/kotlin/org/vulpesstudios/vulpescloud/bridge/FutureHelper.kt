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

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.CompletableFuture

object FutureHelper {

    fun <T> ListenableFuture<T>.toCompletableFuture(): CompletableFuture<T> {
        val cf = CompletableFuture<T>()
        Futures.addCallback(
            this,
            object : FutureCallback<T> {
                override fun onSuccess(result: T) {
                    cf.complete(result)
                }

                override fun onFailure(t: Throwable) {
                    cf.completeExceptionally(t)
                }
            },
            MoreExecutors.directExecutor(),
        )
        return cf
    }
}
