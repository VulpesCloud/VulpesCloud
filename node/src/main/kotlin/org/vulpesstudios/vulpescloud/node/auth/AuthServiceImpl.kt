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

package org.vulpesstudios.vulpescloud.node.auth

import build.buf.gen.vulpescloud.auth.v1.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.grpc.security.PermissionHelper
import org.vulpesstudios.vulpescloud.node.grpc.security.annotations.RequiresPermission
import org.vulpesstudios.vulpescloud.node.utils.MongoUtils
import java.util.*

class AuthServiceImpl(private val jwtSecret: String, private val jwtRefreshSecret: String) :
    AuthServiceGrpcKt.AuthServiceCoroutineImplBase() {

    override suspend fun authenticate(request: AuthenticateRequest): AuthenticateResponse {
        if (Node.instance.configProvider.config.auth.allowAuthentication.not()) {
            return AuthenticateResponse.newBuilder().build()
        }

        val username = request.username.trim()
        val password = request.password

        val user =
            MongoUtils.getUserByName(username)
                ?: return AuthenticateResponse.newBuilder()
                    .setError(AuthError.AUTH_ERROR_INVALID_CREDENTIALS)
                    .build()

        val validPassword = MongoUtils.checkUserPassword(username, password)
        if (!validPassword) {
            return AuthenticateResponse.newBuilder()
                .setError(AuthError.AUTH_ERROR_INVALID_CREDENTIALS)
                .build()
        }

        val permissions = PermissionHelper.getAllPermissionsOfUser(username)

        val now = System.currentTimeMillis()
        val accessExpires = now + 15 * 60_000 // 15 minutes
        val refreshExpires = now + 30L * 24 * 60 * 60_000 // 30 days

        val accessToken =
            JWT.create()
                .withIssuer("vulpescloud")
                .withSubject(username)
                .withArrayClaim("groups", user.groups.toTypedArray())
                .withArrayClaim("permissions", permissions.toTypedArray())
                .withExpiresAt(Date(accessExpires))
                .sign(Algorithm.HMAC256(jwtSecret))

        val refreshToken =
            JWT.create()
                .withIssuer("vulpescloud_refresh")
                .withSubject(username)
                .withExpiresAt(Date(refreshExpires))
                .sign(Algorithm.HMAC256(jwtRefreshSecret))

        return AuthenticateResponse.newBuilder()
            .setToken(accessToken)
            .setExpiresAt(accessExpires / 1000)
            .setRefreshToken(refreshToken)
            .setRefreshExpiresAt(refreshExpires / 1000)
            .setError(AuthError.AUTH_ERROR_NONE_UNSPECIFIED)
            .build()
    }

    override suspend fun refreshToken(request: RefreshTokenRequest): RefreshTokenResponse {
        if (Node.instance.configProvider.config.auth.allowAuthentication.not()) {
            return RefreshTokenResponse.newBuilder().build()
        }
        val refreshToken = request.refreshToken

        val decoded =
            try {
                JWT.require(Algorithm.HMAC256(jwtRefreshSecret))
                    .withIssuer("vulpescloud_refresh")
                    .build()
                    .verify(refreshToken)
            } catch (_: Exception) {
                return RefreshTokenResponse.newBuilder()
                    .setError(AuthError.AUTH_ERROR_INVALID_CREDENTIALS)
                    .build()
            }

        val username =
            decoded.subject
                ?: return RefreshTokenResponse.newBuilder()
                    .setError(AuthError.AUTH_ERROR_INVALID_CREDENTIALS)
                    .build()

        val user =
            MongoUtils.getUserByName(username)
                ?: return RefreshTokenResponse.newBuilder()
                    .setError(AuthError.AUTH_ERROR_INVALID_CREDENTIALS)
                    .build()

        val permissions = PermissionHelper.getAllPermissionsOfUser(username)

        val now = System.currentTimeMillis()
        val accessExpires = now + 15 * 60_000
        val refreshExpires = now + 30L * 24 * 60 * 60_000

        val newAccessToken =
            JWT.create()
                .withIssuer("vulpescloud")
                .withSubject(username)
                .withArrayClaim("groups", user.groups.toTypedArray())
                .withArrayClaim("permissions", permissions.toTypedArray())
                .withExpiresAt(Date(accessExpires))
                .sign(Algorithm.HMAC256(jwtSecret))

        val newRefreshToken =
            JWT.create()
                .withIssuer("vulpescloud_refresh")
                .withSubject(username)
                .withExpiresAt(Date(refreshExpires))
                .sign(Algorithm.HMAC256(jwtRefreshSecret))

        return RefreshTokenResponse.newBuilder()
            .setToken(newAccessToken)
            .setExpiresAt(accessExpires / 1000)
            .setRefreshToken(newRefreshToken)
            .setRefreshExpiresAt(refreshExpires / 1000)
            .setError(AuthError.AUTH_ERROR_NONE_UNSPECIFIED)
            .build()
    }

    override suspend fun isTokenValid(request: IsTokenValidRequest): IsTokenValidResponse {
        if (Node.instance.configProvider.config.auth.allowAuthentication.not()) {
            return IsTokenValidResponse.newBuilder().setValid(false).build()
        }
        val token = request.token

        val verifier = JWT.require(Algorithm.HMAC256(jwtSecret)).withIssuer("vulpescloud").build()

        return try {
            verifier.verify(token)

            IsTokenValidResponse.newBuilder().setValid(true).build()
        } catch (_: com.auth0.jwt.exceptions.TokenExpiredException) {
            IsTokenValidResponse.newBuilder().setValid(false).build()
        } catch (_: Exception) {
            IsTokenValidResponse.newBuilder().setValid(false).build()
        }
    }

    @RequiresPermission("auth.getUserByName")
    override suspend fun getUserByName(request: GetUserByNameRequest): GetUserByNameResponse {
        val user =
            MongoUtils.getUserByName(request.username)
                ?: return GetUserByNameResponse.newBuilder()
                    .setError(UserError.USER_ERROR_NOT_FOUND)
                    .build()

        return GetUserByNameResponse.newBuilder()
            .setUser(
                protoUser {
                    this.name = user.name
                    this.groups.addAll(user.groups)
                    this.permissions.addAll(user.permissions)
                    this.extraData.putAll(user.extraData)
                }
            )
            .build()
    }

    @RequiresPermission("auth.getUserByExtraData")
    override suspend fun getUserByExtraData(
        request: GetUserByExtraDataRequest
    ): GetUserByExtraDataResponse {
        val users = MongoUtils.getAllUsers()
        val user = users.find {
            it.extraData.contains(request.key) && it.extraData[request.key] == request.value
        }
        if (user == null) {
            return GetUserByExtraDataResponse.newBuilder()
                .setError(UserError.USER_ERROR_NOT_FOUND)
                .build()
        }
        return GetUserByExtraDataResponse.newBuilder()
            .setUser(
                protoUser {
                    this.name = user.name
                    this.groups.addAll(user.groups)
                    this.permissions.addAll(user.permissions)
                    this.extraData.putAll(user.extraData)
                }
            )
            .build()
    }
}
