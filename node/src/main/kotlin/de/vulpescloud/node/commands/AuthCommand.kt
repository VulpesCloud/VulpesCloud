package de.vulpescloud.node.commands

import build.buf.gen.vulpescloud.players.v1.getOfflinePlayersRequest
import de.vulpescloud.api.players.OfflinePlayer
import de.vulpescloud.api.players.toAPI
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.grpc.security.PermissionHelper
import de.vulpescloud.node.utils.MongoUtils
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput

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

    // ---------- Offline Player suggestions
    @Suggestions("offlinePlayers")
    fun offlinePlayerSuggestions(): Stream<String> {
        return NodeCoroutineScope.future {
                Node.instance.localGrpcClient.playerAPI
                    .getAllOfflinePlayers(getOfflinePlayersRequest {})
                    .offlinePlayersList
                    .stream()
                    .map { it.toAPI().name }
            }
            .exceptionally { Stream.empty() }
            .get(5, TimeUnit.SECONDS)
    }

    @Parser(suggestions = "offlinePlayers")
    fun offlinePlayerParser(input: CommandInput): OfflinePlayer {
        return NodeCoroutineScope.future {
                Node.instance.localGrpcClient.playerAPI
                    .getAllOfflinePlayers(getOfflinePlayersRequest {})
                    .offlinePlayersList
                    .find { it.name.equals(input.readString()) }!!
                    .toAPI()
            }
            .exceptionally { throw NullPointerException("Player not found!") }
            .get(5, TimeUnit.SECONDS)
    }

    // ---------- Users ----------

    @Permission("auth.user.create")
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

    @Permission("auth.user.setPassword")
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

    @Permission("auth.user.setMinecraftPlayer")
    @Command("auth user set minecraftPlayer <name> <player>")
    fun setMinecraftPlayer(
        source: CommandSource,
        @Argument("name") name: String,
        @Argument("player") player: OfflinePlayer,
    ) {
        NodeCoroutineScope.launch {
            val user = MongoUtils.getUserByName(name)
            if (user == null) {
                source.sendMessage("<red>User <yellow>$name <red>not found.")
                return@launch
            }
            MongoUtils.updateUser(
                name,
                user.copy(
                    extraData =
                        user.extraData.toMutableMap().also { it["minecraft-uuid"] = player.uuid }
                ),
            )
            source.sendMessage("<green>Updated minecraft player for <yellow>$name<green>.")
        }
    }

    @Permission("auth.user.addPermission")
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

    @Permission("auth.user.removePermission")
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

    @Permission("auth.user.addGroup")
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

    @Permission("auth.user.removeGroup")
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

    @Permission("auth.user.list")
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

    @Permission("auth.user.checkPassword")
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

    @Permission("auth.user.checkPermission")
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

    @Permission("auth.user.listPermissions")
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

    @Permission("auth.user.info")
    @Command("auth user info <name>")
    fun getUserInfo(source: CommandSource, @Argument("name") name: String) {
        NodeCoroutineScope.launch {
            val user = MongoUtils.getUserByName(name)
            if (user == null) {
                source.sendMessage("<red>User <yellow>$name <red>not found.")
                return@launch
            }
            source.sendMessage("<green>User info for <yellow>$name<green>:")
            source.sendMessage("<dark_gray>- <gold>Name: <yellow>${user.name}")
            source.sendMessage("<dark_gray>- <gold>Groups:")
            user.groups.forEach { group -> source.sendMessage("<dark_gray>- <gold>$group") }
            source.sendMessage("<dark_gray>- <gold>Permissions:")
            user.permissions.forEach { perm -> source.sendMessage("<dark_gray>- <gold>$perm") }
            source.sendMessage("<dark_gray>- <gold>Extra Data:")
            user.extraData.forEach { (key, value) ->
                source.sendMessage("<dark_gray>- <gold>$key: <yellow>$value")
            }
        }
    }

    // ---------- Groups ----------

    @Command("auth group create <name>")
    @Permission("auth.group.create")
    fun createGroup(source: CommandSource, @Argument("name") name: String) {
        NodeCoroutineScope.launch {
            MongoUtils.createGroup(name)
            source.sendMessage("<green>Created group <gold>$name<green>.")
        }
    }

    @Command("auth group add permission <name> <permission>")
    @Permission("auth.group.addPermission")
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
    @Permission("auth.group.removePermission")
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
    @Permission("auth.group.list")
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
    @Permission("auth.group.listPermissions")
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
