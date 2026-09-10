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

package org.vulpesstudios.vulpescloud.wrapper.grpc

import io.grpc.*

class AuthClientInterceptor(private val token: String) : ClientInterceptor {

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel,
    ): ClientCall<ReqT, RespT> {
        val call = next.newCall(method, callOptions)
        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(call) {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                val authKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
                val communicationTypeKey =
                    Metadata.Key.of("communication-type", Metadata.ASCII_STRING_MARSHALLER)

                headers.put(authKey, "Bearer $token")
                headers.put(communicationTypeKey, "internal")

                super.start(responseListener, headers)
            }
        }
    }
}
