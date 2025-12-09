package de.vulpescloud.node.grpc.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import de.vulpescloud.node.grpc.GrpcContextKeys
import io.grpc.*

class AuthInterceptor(private val internalToken: String, jwtSecret: String) : ServerInterceptor {

    private val jwtVerifier: JWTVerifier =
        JWT.require(Algorithm.HMAC256(jwtSecret)).withIssuer("vulpescloud").build()

    private val publicRpcs = setOf(
        "AuthService/Authenticate",
        "AuthService/RefreshToken",
        "AuthService/IsTokenValid"
    )

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val methodName = call.methodDescriptor.fullMethodName
        if (methodName in publicRpcs) {
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

        return when (communicationType) {
            "internal" -> handleInternalAuth(call, headers, next, token)
            "external" -> handleExternalAuth(call, headers, next, token)
            else -> unauthenticated(call, "Unknown communication type: $communicationType")
        }
    }

    private fun <ReqT : Any?, RespT : Any?> handleInternalAuth(
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

        return Contexts.interceptCall(ctx, call, headers, next)
    }

    private fun <ReqT : Any?, RespT : Any?> handleExternalAuth(
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
