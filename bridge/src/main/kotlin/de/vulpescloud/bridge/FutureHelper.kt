package de.vulpescloud.bridge

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
