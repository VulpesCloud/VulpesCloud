package io.github.thecguygithub.node.command

import lombok.NonNull

fun interface PermissionFunction {

    fun alwaysTrue(): @NonNull PermissionFunction {
        return PermissionFunction { permission -> true }
    }

    fun hasPermission(permission: String): Boolean

}