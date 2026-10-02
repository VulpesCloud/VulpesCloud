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

package org.vulpesstudios.vulpescloud.node.command

import kotlinx.coroutines.runBlocking
import org.vulpesstudios.vulpescloud.api.services.Service
import org.vulpesstudios.vulpescloud.api.tasks.Task
import org.vulpesstudios.vulpescloud.node.commands.ServiceCache
import org.vulpesstudios.vulpescloud.node.commands.TaskCache
import org.vulpesstudios.vulpescloud.node.grpc.security.PermissionHelper
import org.vulpesstudios.vulpescloud.node.Node

object ScopedCommandPermissions {
    suspend fun hasPermission(
        source: CommandSource,
        permission: String,
        resources: Map<String, Set<String>>,
    ): Boolean =
        when (source) {
            is ConsoleCommandSource -> true
            is InternalPlayerCommandSource ->
                PermissionHelper.hasPermission(source.user.name, permission, resources)
            else -> false
        }

    fun inferResources(input: String): Map<String, Set<String>> {
        val tokens = input.trim().removePrefix("/").split(Regex("\\s+")).filter(String::isNotBlank)
        if (tokens.isEmpty()) return emptyMap()
        val resources = mutableMapOf<String, MutableSet<String>>()
        fun matches(pattern: String, value: String): Boolean {
            val regex = "^" + pattern.split("*").joinToString(".*") { Regex.escape(it) } + "$"
            return runCatching { Regex(regex, RegexOption.IGNORE_CASE).matches(value) }
                .getOrDefault(false)
        }
        fun collect(scope: String, candidates: List<String>, pattern: String) {
            candidates
                .filter { matches(pattern, it) }
                .forEach { resources.getOrPut(scope) { mutableSetOf() }.add(it) }
        }

        val nodeIndex = tokens.indexOfFirst { it.equals("node", true) }
        val flagNodeIndex = tokens.indexOfFirst { it.equals("--node", true) }
        val selectedNodeIndex = if (flagNodeIndex >= 0) flagNodeIndex else nodeIndex
        if (selectedNodeIndex >= 0 && selectedNodeIndex + 1 < tokens.size) {
            val requested = tokens[selectedNodeIndex + 1]
            val knownNodes = runCatching {
                runBlocking {
                    org.vulpesstudios.vulpescloud.node.cluster.ClusterHelper.getAllNodeSnapshots()
                        .map { it.name }
                }
            }.getOrDefault(emptyList())
            collect("node", knownNodes, requested)
        }

        val taskNames = if (tokens.any { it.equals("task", true) } || tokens.firstOrNull()?.equals("rollout", true) == true) {
            runCatching {
                TaskCache.getTasks().map { Task.fromDefinition(it).name }
            }
                .getOrDefault(emptyList())
        } else emptyList()
        if (tokens.any { it.equals("task", true) }) {
            tokens.indices
                .filter { tokens[it].equals("task", true) && it + 1 < tokens.size }
                .forEach { collect("task", taskNames, tokens[it + 1]) }
        }
        if (tokens.firstOrNull()?.equals("rollout", true) == true) {
            when (tokens.getOrNull(1)?.lowercase()) {
                "restart" -> tokens.getOrNull(2)?.let { collect("task", taskNames, it) }
                "status" -> tokens.getOrNull(2)?.let { target ->
                    if (taskNames.any { it.equals(target, true) }) collect("task", taskNames, target)
                    else resources.getOrPut("rollout") { mutableSetOf() }.add(target)
                }
                "cancel" -> tokens.getOrNull(2)?.let { resources.getOrPut("rollout") { mutableSetOf() }.add(it) }
            }
        }
        mapOf(
                "user" to setOf("user", "username"),
                "group" to setOf("group"),
                "template" to setOf("template"),
                "player" to setOf("player"),
            )
            .forEach { (scope, keywords) ->
                tokens.indices
                    .filter { tokens[it].lowercase() in keywords && it + 1 < tokens.size }
                    .forEach { resources.getOrPut(scope) { mutableSetOf() }.add(tokens[it + 1]) }
            }
        if (tokens.firstOrNull()?.equals("module", true) == true) {
            val action = tokens.getOrNull(1)?.lowercase()
            val targetIndex = if (action == "info") 3 else 2
            if (action in setOf("load", "start", "stop", "unload", "update", "restart", "info")) {
                tokens.getOrNull(targetIndex)?.let { resources.getOrPut("module") { mutableSetOf() }.add(it) }
                resources.getOrPut("node") { mutableSetOf() }.add(Node.instance.configProvider.config.nodeName)
            }
        }
        if (tokens.firstOrNull()?.lowercase() in setOf("virtualconfigs", "virtualconfig")) {
            val configIndex = tokens.indexOfFirst { it.equals("config", true) }
            if (configIndex >= 0 && configIndex + 1 < tokens.size) {
                resources.getOrPut("virtualconfig") { mutableSetOf() }.add(tokens[configIndex + 1])
            }
        }
        tokens
            .firstOrNull { it.startsWith("--node=", true) }
            ?.substringAfter('=')
            ?.let { requested ->
                val knownNodes = runCatching {
                    runBlocking {
                        org.vulpesstudios.vulpescloud.node.cluster.ClusterHelper
                            .getAllNodeSnapshots()
                            .map { it.name }
                    }
                }.getOrDefault(emptyList())
                collect("node", knownNodes, requested)
            }

        if (tokens.first().equals("services", true) || tokens.first().equals("ser", true)) {
            val requested = tokens.getOrNull(1)
            if (
                requested != null && requested.lowercase() !in setOf("list", "stopall", "deleteall")
            ) {
                val services = runCatching {
                    ServiceCache.getTasks().map { Service.fromDefinition(it) }
                }.getOrDefault(emptyList())
                val pattern =
                    "^" + requested.split("*").joinToString(".*") { Regex.escape(it) } + "$"
                services
                    .filter {
                        runCatching { Regex(pattern, RegexOption.IGNORE_CASE).matches(it.name()) }
                            .getOrDefault(false)
                    }
                    .forEach { service ->
                        resources.getOrPut("service") { mutableSetOf() }.add(service.name())
                        resources.getOrPut("task") { mutableSetOf() }.add(service.task.name)
                        resources.getOrPut("node") { mutableSetOf() }.add(service.node)
                    }
            }
        }
        if (tokens.firstOrNull()?.lowercase() in setOf("player", "players")) {
            val onlineIndex = tokens.indexOfFirst { it.equals("online", true) }
            if (onlineIndex >= 0 && onlineIndex + 2 < tokens.size) {
                val action = tokens[onlineIndex + 1].lowercase()
                if (action in setOf("message", "kick", "title", "actionbar", "connect")) {
                    val requested = tokens[onlineIndex + 2]
                    runCatching {
                        runBlocking {
                            Node.instance.localGrpcClient.playerAPI
                                .getAllOnlinePlayers(build.buf.gen.vulpescloud.players.v1.getAllOnlinePlayersRequest {})
                                .onlinePlayersList
                                .filter { matches(requested, it.name) }
                                .forEach { resources.getOrPut("player") { mutableSetOf() }.add(it.uuid) }
                        }
                    }
                    if (action == "connect") {
                        tokens.getOrNull(onlineIndex + 3)?.let { serverName ->
                            val services = runCatching { ServiceCache.getTasks().map { Service.fromDefinition(it) } }.getOrDefault(emptyList())
                            services.firstOrNull { it.name().equals(serverName, true) }?.let { service ->
                                resources.getOrPut("service") { mutableSetOf() }.add(service.name())
                                resources.getOrPut("task") { mutableSetOf() }.add(service.task.name)
                                resources.getOrPut("node") { mutableSetOf() }.add(service.node)
                            }
                        }
                    }
                }
            }
        }
        return resources.mapValues { it.value.toSet() }
    }
}
