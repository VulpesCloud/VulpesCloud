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
import org.vulpesstudios.vulpescloud.api.tasks.Task
import org.vulpesstudios.vulpescloud.api.virtualconfig.VirtualConfig
import org.vulpesstudios.vulpescloud.node.db.AuthRepository
import org.vulpesstudios.vulpescloud.node.db.TaskRepository
import org.vulpesstudios.vulpescloud.node.db.VirtualConfigRepository
import org.vulpesstudios.vulpescloud.node.grpc.security.model.GroupModel
import org.vulpesstudios.vulpescloud.node.grpc.security.model.UserModel
import build.buf.gen.vulpescloud.virtualconfig.v1.VirtualConfig as ProtoVirtualConfig

@Suppress("UNUSED")
object PersistenceUtils {
    fun updateTask(task: Task) = TaskRepository.save(task)

    fun deleteTask(task: Task) = TaskRepository.delete(task.name)

    fun updateOrInsertVirtualConfig(config: ProtoVirtualConfig) =
        VirtualConfigRepository.save(VirtualConfig.fromDefinition(config))

    fun nothingOrInsertVirtualConfig(config: ProtoVirtualConfig) =
        VirtualConfigRepository.save(VirtualConfig.fromDefinition(config), createOnly = true)

    fun deleteVirtualConfig(config: ProtoVirtualConfig) =
        VirtualConfigRepository.delete(config.name)

    fun getUserByName(name: String): UserModel? = AuthRepository.getUser(name)

    fun getGroupByName(name: String): GroupModel? = AuthRepository.getGroup(name)

    fun createGroup(name: String, permissions: List<String> = emptyList()) =
        AuthRepository.saveGroup(GroupModel(name, permissions), createOnly = true)

    fun createUser(name: String, password: String) {
        val hashed = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        AuthRepository.saveUser(UserModel(name, hashed))
    }

    fun deleteUser(name: String) = AuthRepository.deleteUser(name)

    fun deleteGroup(name: String) = AuthRepository.deleteGroup(name)

    fun updateUserPassword(name: String, password: String) {
        val old = AuthRepository.getUser(name) ?: return
        AuthRepository.saveUser(
            old.copy(password = BCrypt.withDefaults().hashToString(12, password.toCharArray())),
        )
    }

    fun updateUser(name: String, user: UserModel) = AuthRepository.saveUser(user)

    fun addPermissionToUser(username: String, permission: String) {
        AuthRepository.getUser(username)?.let {
            AuthRepository.saveUser(it.copy(permissions = (it.permissions + permission).distinct()))
        }
    }

    fun removePermissionFromUser(username: String, permission: String) {
        AuthRepository.getUser(username)?.let {
            AuthRepository.saveUser(
                it.copy(permissions = it.permissions.filterNot { p -> p == permission }),
            )
        }
    }

    fun addPermissionToGroup(groupName: String, permission: String) {
        AuthRepository.getGroup(groupName)?.let {
            AuthRepository.saveGroup(
                it.copy(permissions = (it.permissions + permission).distinct()),
            )
        }
    }

    fun removePermissionFromGroup(groupName: String, permission: String) {
        AuthRepository.getGroup(groupName)?.let {
            AuthRepository.saveGroup(
                it.copy(permissions = it.permissions.filterNot { p -> p == permission }),
            )
        }
    }

    fun checkUserPassword(username: String, password: String): Boolean =
        AuthRepository.getUser(username)?.let {
            BCrypt.verifyer().verify(password.toCharArray(), it.password).verified
        } ?: false

    fun getAllUsers(): List<UserModel> = AuthRepository.users()

    fun getAllGroups(): List<GroupModel> = AuthRepository.groups()

    fun addUserToGroup(username: String, groupName: String) {
        AuthRepository.getUser(username)?.let {
            AuthRepository.saveUser(it.copy(groups = (it.groups + groupName).distinct()))
        }
    }

    fun removeUserFromGroup(username: String, groupName: String) {
        AuthRepository.getUser(username)?.let {
            AuthRepository.saveUser(it.copy(groups = it.groups.filterNot { g -> g == groupName }))
        }
    }
}
