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

package org.vulpesstudios.vulpescloud.node.utils

import at.favre.lib.crypto.bcrypt.BCrypt
import build.buf.gen.vulpescloud.virtualconfig.v1.VirtualConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.vulpesstudios.vulpescloud.api.services.Service
import org.vulpesstudios.vulpescloud.api.tasks.Task
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.grpc.security.model.GroupModel
import org.vulpesstudios.vulpescloud.node.grpc.security.model.UserModel

object MongoUtils {

    private val tasksDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("tasks")
    }
    private val groupsDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("groups")
    }
    private val usersDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("users")
    }
    private val servicesDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("services")
    }
    private val virtualConfigsDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("virtualconfigs")
    }

    suspend fun updateTask(task: Task) {
        tasksDatabase.upsert(task.name, Json.encodeToJsonElement(task))
    }

    suspend fun updateService(service: Service) {
        servicesDatabase.upsert(service.uuid.toString(), Json.encodeToJsonElement(service))
    }

    suspend fun deleteTask(task: Task) {
        tasksDatabase.delete(task.name)
    }

    suspend fun deleteService(service: Service) {
        servicesDatabase.delete(service.uuid.toString())
    }

    suspend fun updateOrInsertVirtualConfig(config: VirtualConfig) {
        virtualConfigsDatabase.upsert(
            config.name,
            Json.encodeToJsonElement(
                org.vulpesstudios.vulpescloud.api.virtualconfig.VirtualConfig.fromDefinition(config)
            ),
        )
    }

    suspend fun nothingOrInsertVirtualConfig(config: VirtualConfig) {
        virtualConfigsDatabase.insertIgnore(
            config.name,
            Json.encodeToJsonElement(
                org.vulpesstudios.vulpescloud.api.virtualconfig.VirtualConfig.fromDefinition(config)
            ),
        )
    }

    suspend fun deleteVirtualConfig(config: VirtualConfig) {
        virtualConfigsDatabase.delete(config.name)
    }

    suspend fun getUserByName(name: String): UserModel? {
        return Json.decodeFromJsonElement(
            UserModel.serializer(),
            usersDatabase.get(name) ?: return null,
        )
    }

    suspend fun getGroupByName(name: String): GroupModel? {
        return Json.decodeFromJsonElement(
            GroupModel.serializer(),
            groupsDatabase.get(name) ?: return null,
        )
    }

    suspend fun createGroup(name: String, permissions: List<String> = listOf()) {
        groupsDatabase.insert(name, Json.encodeToJsonElement(GroupModel(name, permissions)))
    }

    suspend fun createUser(name: String, password: String) {
        val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        usersDatabase.insert(name, Json.encodeToJsonElement(UserModel(name, hashedPassword)))
    }

    suspend fun deleteUser(name: String) {
        usersDatabase.delete(name)
    }

    suspend fun deleteGroup(name: String) {
        groupsDatabase.delete(name)
    }

    suspend fun updateUserPassword(name: String, password: String) {
        val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        usersDatabase.upsert(name, Json.encodeToJsonElement(UserModel(name, hashedPassword)))
    }

    suspend fun updateUser(name: String, user: UserModel) {
        usersDatabase.upsert(name, Json.encodeToJsonElement(user))
    }

    suspend fun addPermissionToUser(username: String, permission: String) {
        val user = getUserByName(username) ?: return
        val updatedPermissions =
            user.permissions.toMutableList().apply { add(permission) }.distinct()
        val updatedUser = user.copy(permissions = updatedPermissions)
        usersDatabase.upsert(username, Json.encodeToJsonElement(updatedUser))
    }

    suspend fun removePermissionFromUser(username: String, permission: String) {
        val user = getUserByName(username) ?: return
        val updatedPermissions = user.permissions.filter { it != permission }.distinct()
        val updatedUser = user.copy(permissions = updatedPermissions)
        usersDatabase.upsert(username, Json.encodeToJsonElement(updatedUser))
    }

    suspend fun addPermissionToGroup(groupName: String, permission: String) {
        val group = getGroupByName(groupName) ?: return
        val updatedPermissions =
            group.permissions.toMutableList().apply { add(permission) }.distinct()
        val updatedGroup = group.copy(permissions = updatedPermissions)
        groupsDatabase.upsert(groupName, Json.encodeToJsonElement(updatedGroup))
    }

    suspend fun removePermissionFromGroup(groupName: String, permission: String) {
        val group = getGroupByName(groupName) ?: return
        val updatedPermissions = group.permissions.filter { it != permission }.distinct()
        val updatedGroup = group.copy(permissions = updatedPermissions)
        groupsDatabase.upsert(groupName, Json.encodeToJsonElement(updatedGroup))
    }

    suspend fun checkUserPassword(username: String, password: String): Boolean {
        val user = getUserByName(username) ?: return false

        val result = BCrypt.verifyer().verify(password.toCharArray(), user.password)
        return result.verified
    }

    suspend fun getAllUsers(): List<UserModel> {
        return usersDatabase.getAll().map { Json.decodeFromJsonElement(UserModel.serializer(), it) }
    }

    suspend fun getAllGroups(): List<GroupModel> {
        return groupsDatabase.getAll().map {
            Json.decodeFromJsonElement(GroupModel.serializer(), it)
        }
    }

    suspend fun addUserToGroup(username: String, groupName: String) {
        val user = getUserByName(username) ?: return
        val updatedGroups = user.groups.toMutableList().apply { add(groupName) }.distinct()
        val updatedUser = user.copy(groups = updatedGroups)
        usersDatabase.upsert(username, Json.encodeToJsonElement(updatedUser))
    }

    suspend fun removeUserFromGroup(username: String, groupName: String) {
        val user = getUserByName(username) ?: return
        val updatedGroups = user.groups.filter { it != groupName }.distinct()
        val updatedUser = user.copy(groups = updatedGroups)
        usersDatabase.upsert(username, Json.encodeToJsonElement(updatedUser))
    }
}
