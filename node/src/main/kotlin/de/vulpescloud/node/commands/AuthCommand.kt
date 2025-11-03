package de.vulpescloud.node.commands

import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.grpc.security.PermissionHelper
import de.vulpescloud.node.utils.MongoUtils
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.suggestion.Suggestions

@Suppress("UNUSED")
class AuthCommand {

    // ---------- User suggestions ----------
    @Suggestions("users")
    fun userSuggestions(): Stream<String> {
        return CompletableFuture.supplyAsync {
                runBlocking { MongoUtils.getAllUsers().map { it.name } }
            }
            .thenApply { it.stream() }
            .exceptionally { Stream.empty() }
            .get(5, TimeUnit.SECONDS)
    }

    // ---------- Group suggestions ----------
    @Suggestions("groups")
    fun groupSuggestions(): Stream<String> {
        return CompletableFuture.supplyAsync {
                runBlocking { MongoUtils.getAllGroups().map { it.name } }
            }
            .thenApply { it.stream() }
            .exceptionally { Stream.empty() }
            .get(5, TimeUnit.SECONDS)
    }

    // ---------- Users ----------

    @Command("auth user create <name> <password>")
    fun createUser(
        source: CommandSource,
        @Argument("name") name: String,
        @Argument("password") password: String,
    ) {
        NodeCoroutineScope.launch {
            MongoUtils.createUser(name, password)
            source.sendMessage("<green>Created user <yellow>$name<green>.")
        }
    }

    @Command("auth user set password <name> <password>")
    fun setPassword(
        source: CommandSource,
        @Argument("name") name: String,
        @Argument("password") password: String,
    ) {
        NodeCoroutineScope.launch {
            MongoUtils.updateUserPassword(name, password)
            source.sendMessage("<green>Updated password for <yellow>$name<green>.")
        }
    }

    @Command("auth user add permission <name> <permission>")
    fun addUserPermission(
        source: CommandSource,
        @Argument("name") name: String,
        @Argument("permission") permission: String,
    ) {
        NodeCoroutineScope.launch {
            MongoUtils.addPermissionToUser(name, permission)
            source.sendMessage(
                "<green>Added permission <gold>$permission <green>to user <yellow>$name<green>."
            )
        }
    }

    @Command("auth user remove permission <name> <permission>")
    fun removeUserPermission(
        source: CommandSource,
        @Argument("name") name: String,
        @Argument("permission") permission: String,
    ) {
        NodeCoroutineScope.launch {
            MongoUtils.removePermissionFromUser(name, permission)
            source.sendMessage(
                "<red>Removed permission <gold>$permission <red>from user <yellow>$name<red>."
            )
        }
    }

    @Command("auth user add group <name> <group>")
    fun addUserToGroup(
        source: CommandSource,
        @Argument("name") name: String,
        @Argument("group") group: String,
    ) {
        NodeCoroutineScope.launch {
            MongoUtils.addUserToGroup(name, group)
            source.sendMessage(
                "<green>Added user <yellow>$name <green>to group <gold>$group<green>."
            )
        }
    }

    @Command("auth user remove group <name> <group>")
    fun removeUserFromGroup(
        source: CommandSource,
        @Argument("name") name: String,
        @Argument("group") group: String,
    ) {
        NodeCoroutineScope.launch {
            MongoUtils.removeUserFromGroup(name, group)
            source.sendMessage("<red>Removed user <yellow>$name <red>from group <gold>$group<red>.")
        }
    }

    @Command("auth user list")
    fun listUsers(source: CommandSource) {
        NodeCoroutineScope.launch {
            val users = MongoUtils.getAllUsers()
            if (users.isEmpty()) {
                source.sendMessage("<red>No users found.")
                return@launch
            }
            source.sendMessage("<green>Registered users (<yellow>${users.size}<green>):")
            users.forEach {
                source.sendMessage(
                    "<dark_gray>- <yellow>${it.name} <dark_gray>(<gold>${it.groups.size}<dark_gray> groups, <gold>${it.permissions.size}<dark_gray> perms)"
                )
            }
        }
    }

    @Command("auth user check password <name> <password>")
    fun checkUserPassword(
        source: CommandSource,
        @Argument("name") name: String,
        @Argument("password") password: String,
    ) {
        NodeCoroutineScope.launch {
            val valid = MongoUtils.checkUserPassword(name, password)
            if (valid) source.sendMessage("<green>Password for <yellow>$name <green>is valid.")
            else source.sendMessage("<red>Invalid password for <yellow>$name<red>.")
        }
    }

    @Command("auth user check permission <name> <permission>")
    fun checkUserPermission(
        source: CommandSource,
        @Argument("name") name: String,
        @Argument("permission") permission: String,
    ) {
        NodeCoroutineScope.launch {
            val hasPerm = PermissionHelper.hasPermission(name, permission)
            if (hasPerm)
                source.sendMessage(
                    "<green>User <yellow>$name <green>has permission <gold>$permission<green>."
                )
            else
                source.sendMessage(
                    "<red>User <yellow>$name <red>does NOT have permission <gold>$permission<red>."
                )
        }
    }

    @Command("auth user list permissions <name>")
    fun listUserPermissions(source: CommandSource, @Argument("name") name: String) {
        NodeCoroutineScope.launch {
            val user = MongoUtils.getUserByName(name)
            if (user == null) {
                source.sendMessage("<red>User <yellow>$name <red>not found.")
                return@launch
            }
            val permissions = PermissionHelper.getAllPermissionsOfUser(name)
            if (permissions.isEmpty()) {
                source.sendMessage("<yellow>$name <dark_gray>has no permissions.")
            } else {
                source.sendMessage("<green>Permissions for <yellow>$name<green>:")
                permissions.forEach { perm -> source.sendMessage("<dark_gray>- <gold>$perm") }
            }
        }
    }

    // ---------- Groups ----------

    @Command("auth group create <name>")
    fun createGroup(source: CommandSource, @Argument("name") name: String) {
        NodeCoroutineScope.launch {
            MongoUtils.createGroup(name)
            source.sendMessage("<green>Created group <gold>$name<green>.")
        }
    }

    @Command("auth group add permission <name> <permission>")
    fun addGroupPermission(
        source: CommandSource,
        @Argument("name") name: String,
        @Argument("permission") permission: String,
    ) {
        NodeCoroutineScope.launch {
            MongoUtils.addPermissionToGroup(name, permission)
            source.sendMessage(
                "<green>Added permission <gold>$permission <green>to group <yellow>$name<green>."
            )
        }
    }

    @Command("auth group remove permission <name> <permission>")
    fun removeGroupPermission(
        source: CommandSource,
        @Argument("name") name: String,
        @Argument("permission") permission: String,
    ) {
        NodeCoroutineScope.launch {
            MongoUtils.removePermissionFromGroup(name, permission)
            source.sendMessage(
                "<red>Removed permission <gold>$permission <red>from group <yellow>$name<red>."
            )
        }
    }

    @Command("auth group list")
    fun listGroups(source: CommandSource) {
        NodeCoroutineScope.launch {
            val groups = MongoUtils.getAllGroups()
            if (groups.isEmpty()) {
                source.sendMessage("<red>No groups found.")
                return@launch
            }
            source.sendMessage("<green>Registered groups (<yellow>${groups.size}<green>):")
            groups.forEach {
                source.sendMessage(
                    "<dark_gray>- <gold>${it.name} <dark_gray>(<yellow>${it.permissions.size}<dark_gray> perms)"
                )
            }
        }
    }

    @Command("auth group list permissions <name>")
    fun listGroupPermissions(source: CommandSource, @Argument("name") name: String) {
        NodeCoroutineScope.launch {
            val group = MongoUtils.getGroupByName(name)
            if (group == null) {
                source.sendMessage("<red>Group <gold>$name <red>not found.")
                return@launch
            }
            if (group.permissions.isEmpty()) {
                source.sendMessage("<gold>$name <dark_gray>has no permissions.")
            } else {
                source.sendMessage("<green>Permissions for group <gold>$name<green>:")
                group.permissions.forEach { perm -> source.sendMessage("<dark_gray>- <gold>$perm") }
            }
        }
    }
}
