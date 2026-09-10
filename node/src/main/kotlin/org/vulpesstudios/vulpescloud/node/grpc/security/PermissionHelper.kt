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

package org.vulpesstudios.vulpescloud.node.grpc.security

import org.vulpesstudios.vulpescloud.node.utils.MongoUtils

object PermissionHelper {

    suspend fun hasPermission(username: String, permission: String): Boolean {
        val permissions = mutableListOf<String>()

        val user = MongoUtils.getUserByName(username) ?: return false
        val groups = user.groups.map { MongoUtils.getGroupByName(it) }

        groups.forEach { it?.permissions?.let(permissions::addAll) }
        permissions.addAll(user.permissions)

        return permissions.any { matchesPermission(it, permission) }
    }

    private fun matchesPermission(allowed: String, requested: String): Boolean {
        if (allowed == requested) return true

        if (allowed == "*") return true

        if (allowed.endsWith(".*")) {
            val prefix = allowed.removeSuffix(".*")
            return requested.startsWith("$prefix.")
        }

        return false
    }

    suspend fun getAllPermissionsOfUser(username: String): List<String> {
        val permissions = mutableListOf<String>()
        val user = MongoUtils.getUserByName(username) ?: return emptyList()
        val groups = user.groups.map { MongoUtils.getGroupByName(it) }
        groups.forEach { it?.permissions?.let(permissions::addAll) }
        permissions.addAll(user.permissions)
        return permissions.distinct()
    }
}
