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
        runBlocking {
            MongoUtils.createUser(name, password)
            source.sendMessage("<green>Created user</green> <white>$name</white><green>.</green>")
        }
    }

    @Permission("auth.user.setPassword")
    @Command("auth user set password <name> <password>")
    fun setPassword(
        source: CommandSource,
        @Argument("name") name: String,
        @Argument("password") password: String,
    ) {
        runBlocking {
            MongoUtils.updateUserPassword(name, password)
            source.sendMessage("<green>Updated password for</green> <white>$name</white><green>.</green>")
        }
    }

    @Permission("auth.user.setMinecraftPlayer")
    @Command("auth user set minecraftPlayer <name> <player>")
    fun setMinecraftPlayer(
        source: CommandSource,
        @Argument("name") name: String,
        @Argument("player") player: OfflinePlayer,
    ) {
        runBlocking {
            val user = MongoUtils.getUserByName(name)
            if (user == null) {
                source.sendMessage("<red>User</red> <white>$name</white> <red>not found.</red>")
                return@runBlocking
            }
            MongoUtils.updateUser(
                name,
                user.copy(
                    extraData =
                        user.extraData.toMutableMap().also { it["minecraft-uuid"] = player.uuid }
                ),
            )
            source.sendMessage("<green>Updated minecraft player for</green> <white>$name</white><green>.</green>")
        }
    }

    @Permission("auth.user.addPermission")
    @Command("auth user add permission <name> <permission>")
    fun addUserPermission(
        source: CommandSource,
        @Argument("name") name: String,
        @Argument("permission") permission: String,
    ) {
        runBlocking {
            MongoUtils.addPermissionToUser(name, permission)
            source.sendMessage(
                "<green>Added permission</green> <gold>$permission</gold> <green>to user</green> <white>$name</white><green>.</green>"
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
        runBlocking {
            MongoUtils.removePermissionFromUser(name, permission)
            source.sendMessage(
                "<red>Removed permission</red> <gold>$permission</gold> <red>from user</red> <white>$name</white><red>.</red>"
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
        runBlocking {
            MongoUtils.addUserToGroup(name, group)
            source.sendMessage(
                "<green>Added user</green> <white>$name</white> <green>to group</green> <gold>$group</gold><green>.</green>"
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
        runBlocking {
            MongoUtils.removeUserFromGroup(name, group)
            source.sendMessage("<red>Removed user</red> <white>$name</white> <red>from group</red> <gold>$group</gold><red>.</red>")
        }
    }

    @Permission("auth.user.list")
    @Command("auth user list")
    fun listUsers(source: CommandSource) {
        runBlocking {
            val users = MongoUtils.getAllUsers()
            if (users.isEmpty()) {
                source.sendMessage("<red>No users found.</red>")
                return@runBlocking
            }
            source.sendMessage("<gray>Registered users (<gold>${users.size}</gold>):</gray>")
            users.forEach {
                source.sendMessage(
                    " <dark_gray>»</dark_gray> <white>${it.name}</white> <dark_gray>| <gray>Groups:</gray> <gold>${it.groups.size}</gold> <dark_gray>| <gray>Permissions:</gray> <gold>${it.permissions.size}</gold>"
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
        runBlocking {
            val valid = MongoUtils.checkUserPassword(name, password)
            if (valid) source.sendMessage("<green>Password for</green> <white>$name</white> <green>is valid.</green>")
            else source.sendMessage("<red>Invalid password for</red> <white>$name</white><red>.</red>")
        }
    }

    @Permission("auth.user.checkPermission")
    @Command("auth user check permission <name> <permission>")
    fun checkUserPermission(
        source: CommandSource,
        @Argument("name") name: String,
        @Argument("permission") permission: String,
    ) {
        runBlocking {
            val hasPerm = PermissionHelper.hasPermission(name, permission)
            if (hasPerm)
                source.sendMessage(
                    "<green>User</green> <white>$name</white> <green>has permission</green> <gold>$permission</gold><green>.</green>"
                )
            else
                source.sendMessage(
                    "<red>User</red> <white>$name</white> <red>does NOT have permission</red> <gold>$permission</gold><red>.</red>"
                )
        }
    }

    @Permission("auth.user.listPermissions")
    @Command("auth user list permissions <name>")
    fun listUserPermissions(source: CommandSource, @Argument("name") name: String) {
        runBlocking {
            val user = MongoUtils.getUserByName(name)
            if (user == null) {
                source.sendMessage("<red>User</red> <white>$name</white> <red>not found.</red>")
                return@runBlocking
            }
            val permissions = PermissionHelper.getAllPermissionsOfUser(name)
            if (permissions.isEmpty()) {
                source.sendMessage("<white>$name</white> <gray>has no permissions.</gray>")
            } else {
                source.sendMessage("<gray>Permissions for</gray> <white>$name</white><dark_gray>:</dark_gray>")
                permissions.forEach { perm -> source.sendMessage(" <dark_gray>»</dark_gray> <gold>$perm</gold>") }
            }
        }
    }

    @Permission("auth.user.info")
    @Command("auth user info <name>")
    fun getUserInfo(source: CommandSource, @Argument("name") name: String) {
        runBlocking {
            val user = MongoUtils.getUserByName(name)
            if (user == null) {
                source.sendMessage("<red>User</red> <white>$name</white> <red>not found.</red>")
                return@runBlocking
            }
            source.sendMessage("<gold>---------</gold> <white>${user.name}</white> <gold>---------</gold>")
            source.sendMessage("<gray>Groups<dark_gray>:</dark_gray>")
            user.groups.forEach { group -> source.sendMessage(" <dark_gray>»</dark_gray> <gold>$group</gold>") }
            source.sendMessage("<gray>Permissions<dark_gray>:</dark_gray>")
            user.permissions.forEach { perm -> source.sendMessage(" <dark_gray>»</dark_gray> <gold>$perm</gold>") }
            source.sendMessage("<gray>Extra Data<dark_gray>:</dark_gray>")
            user.extraData.forEach { (key, value) ->
                source.sendMessage(" <dark_gray>»</dark_gray> <gray>$key<dark_gray>:</dark_gray> <white>$value</white>")
            }
        }
    }

    // ---------- Groups ----------

    @Command("auth group create <name>")
    @Permission("auth.group.create")
    fun createGroup(source: CommandSource, @Argument("name") name: String) {
        runBlocking {
            MongoUtils.createGroup(name)
            source.sendMessage("<green>Created group</green> <gold>$name</gold><green>.</green>")
        }
    }

    @Command("auth group add permission <name> <permission>")
    @Permission("auth.group.addPermission")
    fun addGroupPermission(
        source: CommandSource,
        @Argument("name") name: String,
        @Argument("permission") permission: String,
    ) {
        runBlocking {
            MongoUtils.addPermissionToGroup(name, permission)
            source.sendMessage(
                "<green>Added permission</green> <gold>$permission</gold> <green>to group</green> <white>$name</white><green>.</green>"
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
        runBlocking {
            MongoUtils.removePermissionFromGroup(name, permission)
            source.sendMessage(
                "<red>Removed permission</red> <gold>$permission</gold> <red>from group</red> <white>$name</white><red>.</red>"
            )
        }
    }

    @Command("auth group list")
    @Permission("auth.group.list")
    fun listGroups(source: CommandSource) {
        runBlocking {
            val groups = MongoUtils.getAllGroups()
            if (groups.isEmpty()) {
                source.sendMessage("<red>No groups found.</red>")
                return@runBlocking
            }
            source.sendMessage("<gray>Registered groups (<gold>${groups.size}</gold>):</gray>")
            groups.forEach {
                source.sendMessage(
                    " <dark_gray>»</dark_gray> <gold>${it.name}</gold> <dark_gray>| <gray>Permissions:</gray> <gold>${it.permissions.size}</gold>"
                )
            }
        }
    }

    @Command("auth group list permissions <name>")
    @Permission("auth.group.listPermissions")
    fun listGroupPermissions(source: CommandSource, @Argument("name") name: String) {
        runBlocking {
            val group = MongoUtils.getGroupByName(name)
            if (group == null) {
                source.sendMessage("<red>Group</red> <gold>$name</gold> <red>not found.</red>")
                return@runBlocking
            }
            if (group.permissions.isEmpty()) {
                source.sendMessage("<gold>$name</gold> <gray>has no permissions.</gray>")
            } else {
                source.sendMessage("<gray>Permissions for group</gray> <gold>$name</gold><dark_gray>:</dark_gray>")
                group.permissions.forEach { perm -> source.sendMessage(" <dark_gray>»</dark_gray> <gold>$perm</gold>") }
            }
        }
    }
}
