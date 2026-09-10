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

package org.vulpesstudios.vulpescloud.node.commands

import build.buf.gen.vulpescloud.players.v1.getOfflinePlayersRequest
import kotlinx.coroutines.future.future
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import org.vulpesstudios.vulpescloud.api.players.OfflinePlayer
import org.vulpesstudios.vulpescloud.api.players.toAPI
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.NodeCoroutineScope
import org.vulpesstudios.vulpescloud.node.command.CommandSource
import org.vulpesstudios.vulpescloud.node.grpc.security.PermissionHelper
import org.vulpesstudios.vulpescloud.node.grpc.security.model.GroupModel
import org.vulpesstudios.vulpescloud.node.grpc.security.model.UserModel
import org.vulpesstudios.vulpescloud.node.utils.MongoUtils
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

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

    @Parser(suggestions = "users")
    fun users(input: CommandInput): UserModel {
        val userName = input.readString()
        return runBlocking {
            val user = MongoUtils.getUserByName(userName)
            if (user == null) {
                throw IllegalArgumentException("User '$userName' not found!")
            }
            user
        }
    }

    // ---------- Group suggestions ----------
    @Suggestions("groupParser")
    fun groupSuggestions(): Stream<String> {
        return CompletableFuture.supplyAsync {
                runBlocking { MongoUtils.getAllGroups().map { it.name } }
            }
            .thenApply { it.stream() }
            .exceptionally { Stream.empty() }
            .get(5, TimeUnit.SECONDS)
    }

    @Parser(suggestions = "groupParser")
    fun groupParser(input: CommandInput): GroupModel {
        val groupName = input.readString()
        return runBlocking {
            val group = MongoUtils.getGroupByName(groupName)
            if (group == null) {
                throw IllegalArgumentException("Group '$groupName' not found!")
            }
            group
        }
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
    @Command("auth create user <name> <password>")
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
    @Command("auth user <user> set password <password>")
    fun setPassword(
        source: CommandSource,
        @Argument("user") user: UserModel,
        @Argument("password") password: String,
    ) {
        runBlocking {
            MongoUtils.updateUserPassword(user.name, password)
            source.sendMessage(
                "<green>Updated password for</green> <white>${user.name}</white><green>.</green>"
            )
        }
    }

    @Command("auth user <user> set extraData <key> <value>")
    @Permission("auth.user.setExtraData")
    fun setExtraData(
        source: CommandSource,
        @Argument("user") user: UserModel,
        @Argument("key") key: String,
        @Argument("value") value: String,
    ) {
        runBlocking {
            MongoUtils.updateUser(
                user.name,
                MongoUtils.getUserByName(user.name)!!.copy(
                    extraData =
                        MongoUtils.getUserByName(user.name)!!.extraData.toMutableMap().also {
                            it[key] = value
                        }
                ),
            )
            source.sendMessage(
                "<green>Updated extraData for</green> <white>${user.name}</white><green>.</green>"
            )
        }
    }

    @Command("auth user <user> get extraData <key>")
    @Permission("auth.user.getExtraData")
    fun getExtraData(
        source: CommandSource,
        @Argument("user") user: UserModel,
        @Argument("key") key: String,
    ) {
        runBlocking {
            val userObj = MongoUtils.getUserByName(user.name)!!
            if (userObj.extraData.containsKey(key)) {
                source.sendMessage(
                    "<green>ExtraData for</green> <white>${user.name}</white> <green>has key</green> <gold>$key</gold> <green>with value</green> <white>${userObj.extraData[key]}</white><green>.</green>"
                )
            } else {
                source.sendMessage(
                    "<red>ExtraData for</red> <white>${user.name}</white> <red>does NOT have key</red> <gold>$key</gold><red>.</red>"
                )
            }
        }
    }

    @Command("auth user <user> remove extraData <key>")
    @Permission("auth.user.removeExtraData")
    fun removeExtraData(
        source: CommandSource,
        @Argument("user") user: UserModel,
        @Argument("key") key: String,
    ) {
        runBlocking {
            val userObj = MongoUtils.getUserByName(user.name)!!
            if (userObj.extraData.containsKey(key)) {
                MongoUtils.updateUser(
                    user.name,
                    MongoUtils.getUserByName(user.name)!!.copy(
                        extraData =
                            MongoUtils.getUserByName(user.name)!!.extraData.toMutableMap().also {
                                it.remove(key)
                            }
                    ),
                )
            }
        }
    }

    @Permission("auth.user.setAvatarURL")
    @Command("auth user <user> set avatarURL <url>")
    fun setAvatarURL(
        source: CommandSource,
        @Argument("user") user: UserModel,
        @Argument("url") url: String,
    ) {
        runBlocking {
            val userObj = MongoUtils.getUserByName(user.name)!!
            MongoUtils.updateUser(
                user.name,
                userObj.copy(
                    extraData = userObj.extraData.toMutableMap().also { it["avatarURL"] = url }
                ),
            )
        }
    }

    @Permission("auth.user.setDisplayName")
    @Command("auth user <user> set displayName <displayName>")
    fun setDisplayName(
        source: CommandSource,
        @Argument("user") user: UserModel,
        @Argument("displayName") displayName: String,
    ) {
        runBlocking {
            val userObj = MongoUtils.getUserByName(user.name)!!
            MongoUtils.updateUser(
                user.name,
                userObj.copy(
                    extraData =
                        userObj.extraData.toMutableMap().also { it["displayName"] = displayName }
                ),
            )
        }
    }

    @Permission("auth.user.setMinecraftPlayer")
    @Command("auth user <user> set minecraftPlayer <player>")
    fun setMinecraftPlayer(
        source: CommandSource,
        @Argument("user") user: UserModel,
        @Argument("player") player: OfflinePlayer,
    ) {
        runBlocking {
            val userObj = MongoUtils.getUserByName(user.name)!!
            MongoUtils.updateUser(
                user.name,
                userObj.copy(
                    extraData =
                        userObj.extraData.toMutableMap().also { it["minecraft-uuid"] = player.uuid }
                ),
            )
            source.sendMessage(
                "<green>Updated minecraft player for</green> <white>${user.name}</white><green>.</green>"
            )
        }
    }

    @Permission("auth.user.addPermission")
    @Command("auth user <user> add permission <permission>")
    fun addUserPermission(
        source: CommandSource,
        @Argument("user") user: UserModel,
        @Argument("permission") permission: String,
    ) {
        runBlocking {
            MongoUtils.addPermissionToUser(user.name, permission)
            source.sendMessage(
                "<green>Added permission</green> <gold>$permission</gold> <green>to user</green> <white>${user.name}</white><green>.</green>"
            )
        }
    }

    @Permission("auth.user.removePermission")
    @Command("auth user <user> remove permission <permission>")
    fun removeUserPermission(
        source: CommandSource,
        @Argument("user") user: UserModel,
        @Argument("permission") permission: String,
    ) {
        runBlocking {
            MongoUtils.removePermissionFromUser(user.name, permission)
            source.sendMessage(
                "<red>Removed permission</red> <gold>$permission</gold> <red>from user</red> <white>${user.name}</white><red>.</red>"
            )
        }
    }

    @Permission("auth.user.addGroup")
    @Command("auth user <user> add group <group>")
    fun addUserToGroup(
        source: CommandSource,
        @Argument("user") user: UserModel,
        @Argument("group") group: GroupModel,
    ) {
        runBlocking {
            MongoUtils.addUserToGroup(user.name, group.name)
            source.sendMessage(
                "<green>Added user</green> <white>${user.name}</white> <green>to group</green> <gold>${group.name}</gold><green>.</green>"
            )
        }
    }

    @Permission("auth.user.removeGroup")
    @Command("auth user <user> remove group <group>")
    fun removeUserFromGroup(
        source: CommandSource,
        @Argument("user") user: UserModel,
        @Argument("group") group: GroupModel,
    ) {
        runBlocking {
            MongoUtils.removeUserFromGroup(user.name, group.name)
            source.sendMessage(
                "<red>Removed user</red> <white>${user.name}</white> <red>from group</red> <gold>${group.name}</gold><red>.</red>"
            )
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
    @Command("auth user <user> check password <password>")
    fun checkUserPassword(
        source: CommandSource,
        @Argument("user") user: UserModel,
        @Argument("password") password: String,
    ) {
        runBlocking {
            val valid = MongoUtils.checkUserPassword(user.name, password)
            if (valid)
                source.sendMessage(
                    "<green>Password for</green> <white>${user.name}</white> <green>is valid.</green>"
                )
            else
                source.sendMessage(
                    "<red>Invalid password for</red> <white>${user.name}</white><red>.</red>"
                )
        }
    }

    @Permission("auth.user.checkPermission")
    @Command("auth user <user> check permission <permission>")
    fun checkUserPermission(
        source: CommandSource,
        @Argument("user") user: UserModel,
        @Argument("permission") permission: String,
    ) {
        runBlocking {
            val hasPerm = PermissionHelper.hasPermission(user.name, permission)
            if (hasPerm)
                source.sendMessage(
                    "<green>User</green> <white>${user.name}</white> <green>has permission</green> <gold>$permission</gold><green>.</green>"
                )
            else
                source.sendMessage(
                    "<red>User</red> <white>${user.name}</white> <red>does NOT have permission</red> <gold>$permission</gold><red>.</red>"
                )
        }
    }

    @Permission("auth.user.listPermissions")
    @Command("auth user <user> list permissions")
    fun listUserPermissions(source: CommandSource, @Argument("user") user: UserModel) {
        runBlocking {
            val permissions = PermissionHelper.getAllPermissionsOfUser(user.name)
            if (permissions.isEmpty()) {
                source.sendMessage("<white>${user.name}</white> <gray>has no permissions.</gray>")
            } else {
                source.sendMessage(
                    "<gray>Permissions for</gray> <white>${user.name}</white><dark_gray>:</dark_gray>"
                )
                permissions.forEach { perm ->
                    source.sendMessage(" <dark_gray>»</dark_gray> <gold>$perm</gold>")
                }
            }
        }
    }

    @Permission("auth.user.info")
    @Command("auth user <user> info")
    fun getUserInfo(source: CommandSource, @Argument("user") user: UserModel) {
        runBlocking {
            val userObj = MongoUtils.getUserByName(user.name)!!
            source.sendMessage(
                "<gold>---------</gold> <white>${userObj.name}</white> <gold>---------</gold>"
            )
            source.sendMessage("<gray>Groups<dark_gray>:</dark_gray>")
            userObj.groups.forEach { group ->
                source.sendMessage(" <dark_gray>»</dark_gray> <gold>$group</gold>")
            }
            source.sendMessage("<gray>Permissions<dark_gray>:</dark_gray>")
            userObj.permissions.forEach { perm ->
                source.sendMessage(" <dark_gray>»</dark_gray> <gold>$perm</gold>")
            }
            source.sendMessage("<gray>Extra Data<dark_gray>:</dark_gray>")
            userObj.extraData.forEach { (key, value) ->
                source.sendMessage(
                    " <dark_gray>»</dark_gray> <gray>$key<dark_gray>:</dark_gray> <white>$value</white>"
                )
            }
        }
    }

    // ---------- Groups ----------

    @Command("auth create group <name>")
    @Permission("auth.group.create")
    fun createGroup(source: CommandSource, @Argument("name") name: String) {
        runBlocking {
            MongoUtils.createGroup(name)
            source.sendMessage("<green>Created group</green> <gold>$name</gold><green>.</green>")
        }
    }

    @Command("auth group <group> add permission <permission>")
    @Permission("auth.group.addPermission")
    fun addGroupPermission(
        source: CommandSource,
        @Argument("group") group: GroupModel,
        @Argument("permission") permission: String,
    ) {
        runBlocking {
            MongoUtils.addPermissionToGroup(group.name, permission)
            source.sendMessage(
                "<green>Added permission</green> <gold>$permission</gold> <green>to group</green> <white>${group.name}</white><green>.</green>"
            )
        }
    }

    @Command("auth group <group> remove permission <permission>")
    @Permission("auth.group.removePermission")
    fun removeGroupPermission(
        source: CommandSource,
        @Argument("group") group: GroupModel,
        @Argument("permission") permission: String,
    ) {
        runBlocking {
            MongoUtils.removePermissionFromGroup(group.name, permission)
            source.sendMessage(
                "<red>Removed permission</red> <gold>$permission</gold> <red>from group</red> <white>${group.name}</white><red>.</red>"
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

    @Command("auth group <group> list permissions")
    @Permission("auth.group.listPermissions")
    fun listGroupPermissions(
        source: CommandSource,
        @Argument("group") group: GroupModel,
    ) {
        runBlocking {
            val groupObj = MongoUtils.getGroupByName(group.name)!!
            if (groupObj.permissions.isEmpty()) {
                source.sendMessage("<gold>${group.name}</gold> <gray>has no permissions.</gray>")
            } else {
                source.sendMessage(
                    "<gray>Permissions for group</gray> <gold>${group.name}</gold><dark_gray>:</dark_gray>"
                )
                groupObj.permissions.forEach { perm ->
                    source.sendMessage(" <dark_gray>»</dark_gray> <gold>$perm</gold>")
                }
            }
        }
    }
}
