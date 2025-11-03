package de.vulpescloud.node.grpc.security

import de.vulpescloud.node.utils.MongoUtils

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
