package de.vulpescloud.node.grpc.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import de.vulpescloud.node.grpc.GrpcContextKeys
import io.grpc.*

class AuthInterceptor(private val internalToken: String, jwtSecret: String) : ServerInterceptor {

    private val jwtVerifier: JWTVerifier =
        JWT.require(Algorithm.HMAC256(jwtSecret)).withIssuer("vulpescloud").build()

    private val publicRpcs = setOf("authenticate", "refreshtoken", "istokenvalid")

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val methodName = call.methodDescriptor.fullMethodName
        val (_, rpcName) = methodName.split("/").let { it[0] to it[1] }

        if (rpcName.lowercase() in publicRpcs) {
            return next.startCall(call, headers)
        }

        val authHeaderKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
        val communicationTypeKey =
            Metadata.Key.of("communication-type", Metadata.ASCII_STRING_MARSHALLER)

        val tokenHeader = headers.get(authHeaderKey)
        val communicationType = headers.get(communicationTypeKey)

        if (tokenHeader.isNullOrBlank()) {
            return unauthenticated(call, "Missing authorization header")
        }

        val token = tokenHeader.removePrefix("Bearer ").trim()
        val sslSession = call.attributes.get(Grpc.TRANSPORT_ATTR_SSL_SESSION)

        return when (communicationType) {
            "internal" -> {
                if (sslSession == null || sslSession.peerCertificates.isNullOrEmpty()) {
                    return unauthenticated(call, "Internal communication requires mutual TLS (client certificate)")
                }
                handleInternalAuth(call, headers, next, token)
            }
            "external" -> handleExternalAuth(call, headers, next, token)
            else -> unauthenticated(call, "Unknown communication type: $communicationType")
        }
    }

    private fun <ReqT, RespT> handleInternalAuth(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
        token: String,
    ): ServerCall.Listener<ReqT> {
        if (token != internalToken) {
            return unauthenticated(call, "Invalid internal token")
        }

        val ctx =
            Context.current()
                .withValue(GrpcContextKeys.USERNAME, "internal")
                .withValue(GrpcContextKeys.ROLE, "system")
                .withValue(GrpcContextKeys.PERMISSIONS, listOf("internal.*"))
                .withValue(GrpcContextKeys.COMMUNICATION_TYPE, "internal")

        return Contexts.interceptCall(ctx, call, headers, next)
    }

    private fun <ReqT, RespT> handleExternalAuth(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
        token: String,
    ): ServerCall.Listener<ReqT> {
        val decoded =
            try {
                jwtVerifier.verify(token)
            } catch (e: Exception) {
                return unauthenticated(call, "Invalid or expired JWT: ${e.message}")
            }

        val username = decoded.subject
        val role = decoded.getClaim("role").asString()
        val permissions = decoded.getClaim("permissions").asList(String::class.java) ?: emptyList()

        val ctx =
            Context.current()
                .withValue(GrpcContextKeys.USERNAME, username)
                .withValue(GrpcContextKeys.ROLE, role)
                .withValue(GrpcContextKeys.PERMISSIONS, permissions)
                .withValue(GrpcContextKeys.COMMUNICATION_TYPE, "external")

        return Contexts.interceptCall(ctx, call, headers, next)
    }

    private fun <ReqT> unauthenticated(
        call: ServerCall<ReqT, *>,
        message: String,
    ): ServerCall.Listener<ReqT> {
        call.close(Status.UNAUTHENTICATED.withDescription(message), Metadata())
        return object : ServerCall.Listener<ReqT>() {}
    }
}
