/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.vulpesstudios.vulpescloud.node.templates

import io.grpc.*
import kotlinx.coroutines.ThreadContextElement
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/** Carries a worker identity through local calls and trusted node-to-node template RPCs. */
object TemplateJobCallContext {
    private val localIdentity = ThreadLocal<TemplateJobIdentity?>()
    private val grpcIdentity = Context.key<TemplateJobIdentity>("vulpescloud-template-job-identity")
    private val metadataJobId = Metadata.Key.of("x-vulpescloud-template-job-id", Metadata.ASCII_STRING_MARSHALLER)
    private val metadataToken = Metadata.Key.of("x-vulpescloud-template-job-token", Metadata.ASCII_STRING_MARSHALLER)

    fun currentIdentity(): TemplateJobIdentity? = localIdentity.get() ?: grpcIdentity.get()
    fun currentJobId(): String? = currentIdentity()?.jobId

    fun clientInterceptor(identity: TemplateJobIdentity): ClientInterceptor = object : ClientInterceptor {
        override fun <ReqT : Any?, RespT : Any?> interceptCall(
            method: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
            next: Channel,
        ): ClientCall<ReqT, RespT> =
            object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                    headers.put(metadataJobId, identity.jobId)
                    headers.put(metadataToken, identity.token)
                    super.start(responseListener, headers)
                }
            }
    }

    fun serverInterceptor(): ServerInterceptor = object : ServerInterceptor {
        override fun <ReqT : Any?, RespT : Any?> interceptCall(
            call: ServerCall<ReqT, RespT>,
            headers: Metadata,
            next: ServerCallHandler<ReqT, RespT>,
        ): ServerCall.Listener<ReqT> {
            val jobId = headers.get(metadataJobId)
            val token = headers.get(metadataToken)
            val identity = if (jobId.isNullOrBlank() || token.isNullOrBlank()) null else TemplateJobIdentity(jobId, token)
            val context = if (identity == null) Context.current() else Context.current().withValue(grpcIdentity, identity)
            return Contexts.interceptCall(context, call, headers, next)
        }
    }

    class CoroutineElement(private val identity: TemplateJobIdentity) : ThreadContextElement<TemplateJobIdentity?>,
        AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<CoroutineElement>

        override fun updateThreadContext(context: CoroutineContext): TemplateJobIdentity? {
            val previous = localIdentity.get()
            localIdentity.set(identity)
            return previous
        }

        override fun restoreThreadContext(context: CoroutineContext, oldState: TemplateJobIdentity?) {
            if (oldState == null) localIdentity.remove() else localIdentity.set(oldState)
        }
    }
}
