package de.vulpescloud.node.grpc.security

import io.grpc.*

class AuthInterceptor(private val validToken: String) : ServerInterceptor {

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val authHeaderKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
        val token = headers.get(authHeaderKey)

        if (token == null || token != "Bearer $validToken") {
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid or missing authorization token"), headers)
            return object : ServerCall.Listener<ReqT>() {} // empty listener, request aborted
        }

        return next.startCall(call, headers)
    }
}
