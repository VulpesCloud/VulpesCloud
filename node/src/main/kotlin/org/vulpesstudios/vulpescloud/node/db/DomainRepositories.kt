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

package org.vulpesstudios.vulpescloud.node.db

import com.google.protobuf.Duration
import com.google.protobuf.Timestamp
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.vulpesstudios.vulpescloud.api.cluster.NodeSnapshot
import org.vulpesstudios.vulpescloud.api.drain.NodeDrainProgress
import org.vulpesstudios.vulpescloud.api.drain.NodeDrainStatus
import org.vulpesstudios.vulpescloud.api.drain.NodeDrainStrategy
import org.vulpesstudios.vulpescloud.api.players.OfflinePlayer
import org.vulpesstudios.vulpescloud.api.rollout.RolloutOptions
import org.vulpesstudios.vulpescloud.api.rollout.RolloutProgress
import org.vulpesstudios.vulpescloud.api.rollout.RolloutStatus
import org.vulpesstudios.vulpescloud.api.rollout.RolloutStrategy
import org.vulpesstudios.vulpescloud.api.serversoftware.ServerSoftware
import org.vulpesstudios.vulpescloud.api.serversoftware.SoftwareType
import org.vulpesstudios.vulpescloud.api.tasks.Task
import org.vulpesstudios.vulpescloud.api.templates.*
import org.vulpesstudios.vulpescloud.api.virtualconfig.VirtualConfig
import org.vulpesstudios.vulpescloud.node.grpc.security.model.GroupModel
import org.vulpesstudios.vulpescloud.node.grpc.security.model.UserModel
import org.vulpesstudios.vulpescloud.node.templates.TemplateJobLock
import org.vulpesstudios.vulpescloud.node.templates.TemplateJobPhase
import org.vulpesstudios.vulpescloud.node.templates.TemplateJobRecord
import java.util.*
import org.vulpesstudios.vulpescloud.api.templates.TemplateJobPhase as ApiTemplateJobPhase

private object TasksTable : Table("vc_tasks") {
    val name = varchar("name", 255).uniqueIndex()
    val maxMemory = long("max_memory")
    val minMemory = long("min_memory")
    val startPort = long("start_port")
    val staticServices = bool("static_services")
    val minOnlineServices = integer("min_online_services")
    val maxOnlineServices = integer("max_online_services")
    val maintenance = bool("maintenance")
    val copyTemplatesToStatic = bool("copy_templates_to_static")
    val serviceFactoryName = varchar("service_factory_name", 255)
    val maxPlayers = integer("max_players")
    val softwareName = varchar("software_name", 255)
    val softwareVersion = varchar("software_version", 255)
    val softwareBuild = integer("software_build")
    val softwareUrl = text("software_url")
    val softwarePluginDir = text("software_plugin_dir")
    val softwareType = varchar("software_type", 32)
    val fallback = bool("fallback")
    val autoStart = bool("auto_start")
    override val primaryKey = PrimaryKey(name)
}

private object TaskPreferredNodesTable : Table("vc_task_preferred_nodes") {
    val task = varchar("task_name", 255)
    val position = integer("position")
    val node = varchar("node_name", 255)
    override val primaryKey = PrimaryKey(task, position)
}

private object TaskJvmArgsTable : Table("vc_task_jvm_args") {
    val task = varchar("task_name", 255)
    val position = integer("position")
    val value = text("arg_value")
    override val primaryKey = PrimaryKey(task, position)
}

private object TaskEnvVarsTable : Table("vc_task_env_vars") {
    val task = varchar("task_name", 255)
    val position = integer("position")
    val value = text("arg_value")
    override val primaryKey = PrimaryKey(task, position)
}

private object TaskAttributesTable : Table("vc_task_attributes") {
    val task = varchar("task_name", 255)
    val attribute = varchar("attribute", 255)
    val value = text("attribute_value")
    override val primaryKey = PrimaryKey(task, attribute)
}

private object TaskTemplatesTable : Table("vc_task_templates") {
    val task = varchar("task_name", 255)
    val position = integer("position")
    val id = varchar("template_id", 255)
    val name = varchar("template_name", 255)
    val weight = integer("weight")
    val storage = varchar("storage_name", 255)
    val node = varchar("node_name", 255).nullable()
    val version = varchar("version", 255)
    val enabled = bool("enabled")
    override val primaryKey = PrimaryKey(task, position)
}

private object UsersTable : Table("vc_users") {
    val name = varchar("username", 255)
    val password = varchar("password_hash", 255)
    val extra = text("extra_data_json")
    override val primaryKey = PrimaryKey(name)
}

private object GroupsTable : Table("vc_groups") {
    val name = varchar("name", 255)
    override val primaryKey = PrimaryKey(name)
}

private object UserGroupsTable : Table("vc_user_groups") {
    val user = varchar("username", 255)
    val group = varchar("group_name", 255)
    override val primaryKey = PrimaryKey(user, group)
}

private object UserPermissionsTable : Table("vc_user_permissions") {
    val user = varchar("username", 255)
    val permission = varchar("permission", 512)
    override val primaryKey = PrimaryKey(user, permission)
}

private object GroupPermissionsTable : Table("vc_group_permissions") {
    val group = varchar("group_name", 255)
    val permission = varchar("permission", 512)
    override val primaryKey = PrimaryKey(group, permission)
}

private object VirtualConfigsTable : Table("vc_virtual_configs") {
    val name = varchar("name", 255)
    val created = long("created_at")
    val updated = long("last_updated_at")
    val config = text("config_json")
    override val primaryKey = PrimaryKey(name)
}

private object TemplatesTable : Table("vc_templates") {
    val id = varchar("id", 255)
    val name = varchar("name", 255)
    val weight = integer("weight")
    val storage = varchar("storage_name", 255)
    val node = varchar("node_name", 255).nullable()
    val version = varchar("version", 255)
    val enabled = bool("enabled")
    override val primaryKey = PrimaryKey(id)
}

private object SnapshotsTable : Table("vc_node_snapshots") {
    val node = varchar("node_name", 255)
    val uuid = varchar("node_uuid", 36)
    val state = varchar("state", 32)
    val players = long("players_on_node")
    val timestamp = long("timestamp")
    val startup = long("startup_timestamp")
    val system = text("system_json")
    val services = text("services_json")
    val attributes = text("attributes_json")
    override val primaryKey = PrimaryKey(node)
}

private object OfflinePlayersTable : Table("vc_offline_players") {
    val uuid = varchar("uuid", 36)
    val name = varchar("name", 255)
    val first = long("first_seen")
    val last = long("last_seen")
    override val primaryKey = PrimaryKey(uuid)
}

private object ClusterTlsTable : Table("vc_cluster_tls_records") {
    val key = varchar("record_key", 255)
    val pem = text("ca_certificate_pem")
    val encrypted = text("encrypted_private_key_json")
    override val primaryKey = PrimaryKey(key)
}

private object RolloutsTable : Table("vc_rollouts") {
    val id = varchar("rollout_id", 255)
    val task = varchar("task_name", 255)
    val status = varchar("status", 32)
    val strategy = varchar("strategy", 32)
    val batch = integer("batch_size")
    val threshold = integer("drain_player_threshold").nullable()
    val drainSec = long("drain_timeout_seconds").nullable()
    val drainNano = integer("drain_timeout_nanos").nullable()
    val ready = long("readiness_timeout_seconds")
    val readyNano = integer("readiness_timeout_nanos")
    val bypass = bool("bypass_max_service_count")
    val fallback = bool("fallback_remaining_players")
    val total = integer("total_services_to_replace")
    val started = integer("services_started")
    val servicesReady = integer("services_ready")
    val stopped = integer("services_stopped")
    val pending = text("pending_stop_service_names_json")
    val newNames = text("new_service_names_json")
    val startSec = long("started_at_seconds").nullable()
    val startNano = integer("started_at_nanos").nullable()
    val completeSec = long("completed_at_seconds").nullable()
    val completeNano = integer("completed_at_nanos").nullable()
    val reason = text("failure_reason")
    val batchNumber = integer("current_batch_number")
    val batches = integer("total_batches")
    override val primaryKey = PrimaryKey(id)
}

private object DrainsTable : Table("vc_node_drains") {
    val node = varchar("node_name", 255)
    val strategy = varchar("strategy", 32)
    val status = varchar("status", 32)
    val startSec = long("started_at_seconds").nullable()
    val startNano = integer("started_at_nanos").nullable()
    val deadlineSec = long("deadline_seconds").nullable()
    val deadlineNano = integer("deadline_nanos").nullable()
    val completeSec = long("completed_at_seconds").nullable()
    val completeNano = integer("completed_at_nanos").nullable()
    val threshold = integer("player_threshold")
    val total = integer("services_total")
    val stopped = integer("services_stopped")
    val remaining = integer("services_remaining")
    val players = integer("players_remaining")
    val failure = text("failure_reason").nullable()
    val reason = text("drain_reason").nullable()
    override val primaryKey = PrimaryKey(node)
}

private object ChronyxLeasesTable : Table("vc_chronyx_leases") {
    val task = varchar("task_name", 255)
    val host = varchar("host_id", 255)
    val token = varchar("token", 255)
    val acquired = long("acquired_at")
    val expires = long("expires_at")
    override val primaryKey = PrimaryKey(task, token)
}

private object ChronyxGuardsTable : Table("vc_chronyx_task_guards") {
    val task = varchar("task_name", 255)
    val touched = long("touched_at")
    override val primaryKey = PrimaryKey(task)
}

private object TemplateJobsTable : Table("vc_template_job_records") {
    val id = varchar("job_id", 255)
    val type = varchar("type", 32)
    val status = varchar("status", 32)
    val phase = varchar("phase", 64)
    val progressPhase = varchar("progress_phase", 64)
    val sourceRef = text("source_json")
    val destination = text("destination_json")
    val conflict = varchar("conflict_mode", 32)
    val extra = varchar("extra_files_mode", 32)
    val total = integer("total_files")
    val processed = integer("files_processed")
    val bytes = long("bytes_processed")
    val totalBytes = long("total_bytes")
    val current = text("current_file")
    val copied = integer("files_copied")
    val skipped = integer("files_skipped")
    val overwritten = integer("files_overwritten")
    val purged = integer("files_purged")
    val started = long("started_at")
    val completed = long("completed_at")
    val failure = text("failure_reason").nullable()
    val worker = varchar("worker_token", 255)
    val inventory = text("source_inventory_json")
    val directories = text("source_directory_inventory_json")
    val destinationExisted = bool("destination_existed")
    val initialInventory = text("initial_destination_inventory_json")
    val expectedInventory = text("expected_destination_inventory_json")
    val completedPaths = text("completed_paths_json")
    val purgedPaths = text("purged_paths_json")
    val inFlight = text("in_flight_path").nullable()
    val inFlightPurge = text("in_flight_purge_path").nullable()
    val workerNode = varchar("worker_node_name", 255).nullable()
    val lease = long("worker_lease_until")
    val cancel = bool("cancel_requested")
    val locks = text("lock_keys_json")
    override val primaryKey = PrimaryKey(id)
}

private object TemplateLocksTable : Table("vc_template_job_locks") {
    val key = varchar("lock_key", 255)
    val job = varchar("job_id", 255)
    val locked = long("locked_at")
    val description = text("description")
    val lease = long("lease_until")
    override val primaryKey = PrimaryKey(key)
}

private val sqlDb
    get() = DatabaseProvider.getMainDatabaseProvider().sqlDatabase()
private val codec = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

object TaskRepository {
    fun get(name: String): Task? =
        transaction(sqlDb) {
            TasksTable.selectAll().where { TasksTable.name eq name }.firstOrNull()?.let(::decode)
        }

    fun getAll(): List<Task> = transaction(sqlDb) { TasksTable.selectAll().map(::decode) }

    fun exists(name: String): Boolean =
        transaction(sqlDb) { TasksTable.selectAll().where { TasksTable.name eq name }.count() > 0 }

    fun save(task: Task) =
        transaction(sqlDb) {
            val update =
                TasksTable.update({ TasksTable.name eq task.name }) {
                    it[maxMemory] = task.maxMemory
                    it[minMemory] = task.minMemory
                    it[startPort] = task.startPort
                    it[staticServices] = task.staticServices
                    it[minOnlineServices] = task.minOnlineServices
                    it[maxOnlineServices] = task.maxOnlineServices
                    it[maintenance] = task.maintenance
                    it[copyTemplatesToStatic] = task.copyTemplatesToStatic
                    it[serviceFactoryName] = task.serviceFactoryName
                    it[maxPlayers] = task.maxPlayers
                    it[softwareName] = task.software.name
                    it[softwareVersion] = task.software.version
                    it[softwareBuild] = task.software.build
                    it[softwareUrl] = task.software.url
                    it[softwarePluginDir] = task.software.pluginDir
                    it[softwareType] = task.software.type.name
                    it[fallback] = task.fallback
                    it[autoStart] = task.autoStart
                }
            if (update == 0)
                TasksTable.insert {
                    it[name] = task.name
                    it[maxMemory] = task.maxMemory
                    it[minMemory] = task.minMemory
                    it[startPort] = task.startPort
                    it[staticServices] = task.staticServices
                    it[minOnlineServices] = task.minOnlineServices
                    it[maxOnlineServices] = task.maxOnlineServices
                    it[maintenance] = task.maintenance
                    it[copyTemplatesToStatic] = task.copyTemplatesToStatic
                    it[serviceFactoryName] = task.serviceFactoryName
                    it[maxPlayers] = task.maxPlayers
                    it[softwareName] = task.software.name
                    it[softwareVersion] = task.software.version
                    it[softwareBuild] = task.software.build
                    it[softwareUrl] = task.software.url
                    it[softwarePluginDir] = task.software.pluginDir
                    it[softwareType] = task.software.type.name
                    it[fallback] = task.fallback
                    it[autoStart] = task.autoStart
                }
            TaskPreferredNodesTable.deleteWhere { TaskPreferredNodesTable.task eq task.name }
            task.preferredNodes.forEachIndexed { i, node ->
                TaskPreferredNodesTable.insert {
                    it[this.task] = task.name
                    it[position] = i
                    it[this.node] = node
                }
            }
            TaskJvmArgsTable.deleteWhere { TaskJvmArgsTable.task eq task.name }
            task.jvmArgs.forEachIndexed { i, arg ->
                TaskJvmArgsTable.insert {
                    it[this.task] = task.name
                    it[position] = i
                    it[value] = arg
                }
            }
            TaskEnvVarsTable.deleteWhere { TaskEnvVarsTable.task eq task.name }
            task.envVars.forEachIndexed { i, arg ->
                TaskEnvVarsTable.insert {
                    it[this.task] = task.name
                    it[position] = i
                    it[value] = arg
                }
            }
            TaskAttributesTable.deleteWhere { TaskAttributesTable.task eq task.name }
            task.attributes.forEach { (k, v) ->
                TaskAttributesTable.insert {
                    it[this.task] = task.name
                    it[attribute] = k
                    it[value] = v
                }
            }
            TaskTemplatesTable.deleteWhere { TaskTemplatesTable.task eq task.name }
            task.templates.forEachIndexed { i, t ->
                TaskTemplatesTable.insert {
                    it[this.task] = task.name
                    it[position] = i
                    it[id] = t.id
                    it[name] = t.name
                    it[weight] = t.weight
                    it[storage] = t.location.storageName
                    it[node] = t.location.nodeName
                    it[version] = t.version
                    it[enabled] = t.enabled
                }
            }
        }

    fun delete(name: String) =
        transaction(sqlDb) { TasksTable.deleteWhere { TasksTable.name eq name } }

    private fun decode(row: ResultRow): Task {
        val name = row[TasksTable.name]
        return Task(
            name,
            row[TasksTable.maxMemory],
            row[TasksTable.minMemory],
            row[TasksTable.startPort],
            TaskTemplatesTable.selectAll()
                .where { TaskTemplatesTable.task eq name }
                .orderBy(TaskTemplatesTable.position)
                .map {
                    Template(
                        it[TaskTemplatesTable.name],
                        it[TaskTemplatesTable.weight],
                        it[TaskTemplatesTable.id],
                        TemplateLocation(
                            it[TaskTemplatesTable.storage],
                            it[TaskTemplatesTable.node],
                        ),
                        it[TaskTemplatesTable.version],
                        it[TaskTemplatesTable.enabled],
                    )
                },
            row[TasksTable.staticServices],
            row[TasksTable.minOnlineServices],
            row[TasksTable.maxOnlineServices],
            row[TasksTable.maintenance],
            row[TasksTable.copyTemplatesToStatic],
            row[TasksTable.serviceFactoryName],
            TaskPreferredNodesTable.selectAll()
                .where { TaskPreferredNodesTable.task eq name }
                .orderBy(TaskPreferredNodesTable.position)
                .map { it[TaskPreferredNodesTable.node] },
            row[TasksTable.maxPlayers],
            ServerSoftware(
                row[TasksTable.softwareName],
                row[TasksTable.softwareVersion],
                row[TasksTable.softwareBuild],
                row[TasksTable.softwareUrl],
                row[TasksTable.softwarePluginDir],
                SoftwareType.valueOf(row[TasksTable.softwareType]),
            ),
            TaskAttributesTable.selectAll()
                .where { TaskAttributesTable.task eq name }
                .associate { it[TaskAttributesTable.attribute] to it[TaskAttributesTable.value] },
            TaskJvmArgsTable.selectAll()
                .where { TaskJvmArgsTable.task eq name }
                .orderBy(TaskJvmArgsTable.position)
                .map { it[TaskJvmArgsTable.value] },
            TaskEnvVarsTable.selectAll()
                .where { TaskEnvVarsTable.task eq name }
                .orderBy(TaskEnvVarsTable.position)
                .map { it[TaskEnvVarsTable.value] },
            row[TasksTable.fallback],
            row[TasksTable.autoStart],
        )
    }
}

object AuthRepository {
    fun getUser(name: String): UserModel? =
        transaction(sqlDb) {
            val row =
                UsersTable.selectAll().where { UsersTable.name eq name }.firstOrNull()
                    ?: return@transaction null
            UserModel(
                row[UsersTable.name],
                row[UsersTable.password],
                UserGroupsTable.selectAll()
                    .where { UserGroupsTable.user eq name }
                    .map { it[UserGroupsTable.group] },
                UserPermissionsTable.selectAll()
                    .where { UserPermissionsTable.user eq name }
                    .map { it[UserPermissionsTable.permission] },
                codec.decodeFromString(row[UsersTable.extra]),
            )
        }

    fun getGroup(name: String): GroupModel? =
        transaction(sqlDb) {
            GroupsTable.selectAll().where { GroupsTable.name eq name }.firstOrNull()
                ?: return@transaction null
            GroupModel(
                name,
                GroupPermissionsTable.selectAll()
                    .where { GroupPermissionsTable.group eq name }
                    .map { it[GroupPermissionsTable.permission] },
            )
        }

    fun users(): List<UserModel> =
        transaction(sqlDb) {
            UsersTable.selectAll().map { row ->
                val n = row[UsersTable.name]
                UserModel(
                    n,
                    row[UsersTable.password],
                    UserGroupsTable.selectAll()
                        .where { UserGroupsTable.user eq n }
                        .map { it[UserGroupsTable.group] },
                    UserPermissionsTable.selectAll()
                        .where { UserPermissionsTable.user eq n }
                        .map { it[UserPermissionsTable.permission] },
                    codec.decodeFromString(row[UsersTable.extra]),
                )
            }
        }

    fun groups(): List<GroupModel> =
        transaction(sqlDb) {
            GroupsTable.selectAll().map { row ->
                val n = row[GroupsTable.name]
                GroupModel(
                    n,
                    GroupPermissionsTable.selectAll()
                        .where { GroupPermissionsTable.group eq n }
                        .map { it[GroupPermissionsTable.permission] },
                )
            }
        }

    fun saveUser(user: UserModel) =
        transaction(sqlDb) {
            val old = UsersTable.selectAll().where { UsersTable.name eq user.name }.firstOrNull()
            if (old == null)
                UsersTable.insert {
                    it[name] = user.name
                    it[password] = user.password
                    it[extra] = codec.encodeToString(user.extraData)
                }
            else
                UsersTable.update({ UsersTable.name eq user.name }) {
                    it[password] = user.password
                    it[extra] = codec.encodeToString(user.extraData)
                }
            UserGroupsTable.deleteWhere { UserGroupsTable.user eq user.name }
            user.groups.distinct().forEach { g ->
                UserGroupsTable.insert {
                    it[this.user] = user.name
                    it[group] = g
                }
            }
            UserPermissionsTable.deleteWhere { UserPermissionsTable.user eq user.name }
            user.permissions.distinct().forEach { p ->
                UserPermissionsTable.insert {
                    it[this.user] = user.name
                    it[permission] = p
                }
            }
        }

    fun saveGroup(group: GroupModel, createOnly: Boolean = false) =
        transaction(sqlDb) {
            val exists =
                GroupsTable.selectAll().where { GroupsTable.name eq group.name }.firstOrNull() !=
                    null
            if (exists && createOnly) return@transaction
            if (!exists) GroupsTable.insert { it[name] = group.name }
            GroupPermissionsTable.deleteWhere { GroupPermissionsTable.group eq group.name }
            group.permissions.distinct().forEach { p ->
                GroupPermissionsTable.insert {
                    it[this.group] = group.name
                    it[permission] = p
                }
            }
        }

    fun deleteUser(name: String) =
        transaction(sqlDb) { UsersTable.deleteWhere { UsersTable.name eq name } }

    fun deleteGroup(name: String) =
        transaction(sqlDb) { GroupsTable.deleteWhere { GroupsTable.name eq name } }
}

object VirtualConfigRepository {
    fun get(name: String): VirtualConfig? =
        transaction(sqlDb) {
            VirtualConfigsTable.selectAll()
                .where { VirtualConfigsTable.name eq name }
                .firstOrNull()
                ?.let { r ->
                    VirtualConfig(
                        r[VirtualConfigsTable.name],
                        r[VirtualConfigsTable.created],
                        r[VirtualConfigsTable.updated],
                        org.json.JSONObject(r[VirtualConfigsTable.config]),
                    )
                }
        }

    fun getAll(): List<VirtualConfig> =
        transaction(sqlDb) {
            VirtualConfigsTable.selectAll().map { r ->
                VirtualConfig(
                    r[VirtualConfigsTable.name],
                    r[VirtualConfigsTable.created],
                    r[VirtualConfigsTable.updated],
                    org.json.JSONObject(r[VirtualConfigsTable.config]),
                )
            }
        }

    fun save(config: VirtualConfig, createOnly: Boolean = false) =
        transaction(sqlDb) {
            val old =
                VirtualConfigsTable.selectAll()
                    .where { VirtualConfigsTable.name eq config.name }
                    .firstOrNull()
            if (old == null)
                VirtualConfigsTable.insert {
                    it[name] = config.name
                    it[created] = config.createdAt
                    it[updated] = config.lastUpdatedAt
                    it[this.config] = config.config.toString()
                }
            else if (!createOnly)
                VirtualConfigsTable.update({ VirtualConfigsTable.name eq config.name }) {
                    it[updated] = config.lastUpdatedAt
                    it[this.config] = config.config.toString()
                }
        }

    fun delete(name: String) =
        transaction(sqlDb) { VirtualConfigsTable.deleteWhere { VirtualConfigsTable.name eq name } }
}

object TemplateRepository {
    fun get(id: String): Template? =
        transaction(sqlDb) {
            TemplatesTable.selectAll()
                .where { TemplatesTable.id eq id }
                .firstOrNull()
                ?.let(::decode)
        }

    fun getAll(): List<Template> = transaction(sqlDb) { TemplatesTable.selectAll().map(::decode) }

    fun save(t: Template, createOnly: Boolean = false): Boolean =
        transaction(sqlDb) {
            val q = TemplatesTable.selectAll().where { TemplatesTable.id eq t.id }.firstOrNull()
            if (q != null && createOnly) return@transaction false
            val update =
                TemplatesTable.update({ TemplatesTable.id eq t.id }) {
                    it[name] = t.name
                    it[weight] = t.weight
                    it[storage] = t.location.storageName
                    it[node] = t.location.nodeName
                    it[version] = t.version
                    it[enabled] = t.enabled
                }
            if (update == 0)
                TemplatesTable.insert {
                    it[id] = t.id
                    it[name] = t.name
                    it[weight] = t.weight
                    it[storage] = t.location.storageName
                    it[node] = t.location.nodeName
                    it[version] = t.version
                    it[enabled] = t.enabled
                }
            true
        }

    fun delete(id: String) =
        transaction(sqlDb) { TemplatesTable.deleteWhere { TemplatesTable.id eq id } }

    private fun decode(r: ResultRow) =
        Template(
            r[TemplatesTable.name],
            r[TemplatesTable.weight],
            r[TemplatesTable.id],
            TemplateLocation(
                r[TemplatesTable.storage],
                r[TemplatesTable.node],
            ),
            r[TemplatesTable.version],
            r[TemplatesTable.enabled],
        )
}

object NodeSnapshotRepository {
    fun get(name: String): NodeSnapshot? =
        transaction(sqlDb) {
            SnapshotsTable.selectAll()
                .where { SnapshotsTable.node eq name }
                .firstOrNull()
                ?.let(::decode)
        }

    fun getAll(): List<NodeSnapshot> =
        transaction(sqlDb) { SnapshotsTable.selectAll().map(::decode) }

    fun save(s: NodeSnapshot) =
        transaction(sqlDb) {
            val upd =
                SnapshotsTable.update({ SnapshotsTable.node eq s.name }) {
                    it[uuid] = s.uuid.toString()
                    it[state] = s.state.name
                    it[players] = s.playersOnNode
                    it[timestamp] = s.timestamp
                    it[startup] = s.startupTimestamp
                    it[system] = codec.encodeToString(s.system)
                    it[services] = codec.encodeToString(s.services)
                    it[attributes] = codec.encodeToString(s.attributes)
                }
            if (upd == 0)
                SnapshotsTable.insert {
                    it[node] = s.name
                    it[uuid] = s.uuid.toString()
                    it[state] = s.state.name
                    it[players] = s.playersOnNode
                    it[timestamp] = s.timestamp
                    it[startup] = s.startupTimestamp
                    it[system] = codec.encodeToString(s.system)
                    it[services] = codec.encodeToString(s.services)
                    it[attributes] = codec.encodeToString(s.attributes)
                }
        }

    private fun decode(r: ResultRow): NodeSnapshot =
        NodeSnapshot(
            r[SnapshotsTable.node],
            UUID.fromString(r[SnapshotsTable.uuid]),
            org.vulpesstudios.vulpescloud.api.cluster.NodeState.valueOf(r[SnapshotsTable.state]),
            r[SnapshotsTable.players],
            r[SnapshotsTable.timestamp],
            r[SnapshotsTable.startup],
            codec.decodeFromString(r[SnapshotsTable.system]),
            codec.decodeFromString(r[SnapshotsTable.services]),
            codec.decodeFromString(r[SnapshotsTable.attributes]),
        )
}

object OfflinePlayerRepository {
    fun get(uuid: String): OfflinePlayer? =
        transaction(sqlDb) {
            OfflinePlayersTable.selectAll()
                .where { OfflinePlayersTable.uuid eq uuid }
                .firstOrNull()
                ?.let {
                    OfflinePlayer(
                        it[OfflinePlayersTable.name],
                        it[OfflinePlayersTable.uuid],
                        it[OfflinePlayersTable.last],
                        it[OfflinePlayersTable.first],
                    )
                }
        }

    fun getAll(): List<OfflinePlayer> =
        transaction(sqlDb) {
            OfflinePlayersTable.selectAll().map {
                OfflinePlayer(
                    it[OfflinePlayersTable.name],
                    it[OfflinePlayersTable.uuid],
                    it[OfflinePlayersTable.last],
                    it[OfflinePlayersTable.first],
                )
            }
        }

    fun save(player: OfflinePlayer, insertOnly: Boolean = false) =
        transaction(sqlDb) {
            val updated =
                if (insertOnly) 0
                else
                    OfflinePlayersTable.update({ OfflinePlayersTable.uuid eq player.uuid }) {
                        it[name] = player.name
                        it[last] = player.lastSeen
                        it[first] = player.firstSeen
                    }
            if (updated == 0)
                OfflinePlayersTable.insertIgnore {
                    it[uuid] = player.uuid
                    it[name] = player.name
                    it[first] = player.firstSeen
                    it[last] = player.lastSeen
                }
        }
}

object ClusterTlsRepository {
    fun getCa(): org.vulpesstudios.vulpescloud.node.cluster.tls.StoredCa? =
        transaction(sqlDb) {
            ClusterTlsTable.selectAll()
                .where { ClusterTlsTable.key eq "ca" }
                .firstOrNull()
                ?.let { r ->
                    org.vulpesstudios.vulpescloud.node.cluster.tls.StoredCa(
                        r[ClusterTlsTable.pem],
                        codec.decodeFromString(r[ClusterTlsTable.encrypted]),
                    )
                }
        }

    fun saveCa(ca: org.vulpesstudios.vulpescloud.node.cluster.tls.StoredCa) =
        transaction(sqlDb) {
            val encoded = codec.encodeToString(ca.encryptedKey)
            if (
                ClusterTlsTable.update({ ClusterTlsTable.key eq "ca" }) {
                    it[pem] = ca.caCertPem
                    it[encrypted] = encoded
                } == 0
            )
                ClusterTlsTable.insert {
                    it[key] = "ca"
                    it[pem] = ca.caCertPem
                    it[encrypted] = encoded
                }
        }
}

object RolloutRepository {
    fun get(id: String): RolloutProgress? =
        transaction(sqlDb) {
            RolloutsTable.selectAll().where { RolloutsTable.id eq id }.firstOrNull()?.let(::decode)
        }

    fun getAll(): List<RolloutProgress> =
        transaction(sqlDb) { RolloutsTable.selectAll().map(::decode) }

    fun save(p: RolloutProgress, createOnly: Boolean = false) =
        transaction(sqlDb) {
            val existing =
                RolloutsTable.selectAll().where { RolloutsTable.id eq p.rolloutId }.firstOrNull()
            if (existing != null && createOnly) return@transaction false
            val o = p.options
            val updated =
                RolloutsTable.update({ RolloutsTable.id eq p.rolloutId }) {
                    it[task] = p.taskName
                    it[status] = p.status.name
                    it[strategy] = o.strategy.name
                    it[batch] = o.batchSize
                    it[threshold] = o.drainPlayerThreshold
                    it[drainSec] = o.drainTimeout?.seconds
                    it[drainNano] = o.drainTimeout?.nanos
                    it[ready] = o.readinessTimeout.seconds
                    it[readyNano] = o.readinessTimeout.nanos
                    it[bypass] = o.bypassMaxServiceCount
                    it[fallback] = o.fallbackRemainingPlayers
                    it[total] = p.totalServicesToReplace
                    it[started] = p.servicesStarted
                    it[servicesReady] = p.servicesReady
                    it[stopped] = p.servicesStopped
                    it[pending] = codec.encodeToString(p.pendingStopServiceNames)
                    it[newNames] = codec.encodeToString(p.newServiceNames)
                    it[startSec] = p.startedAt?.seconds
                    it[startNano] = p.startedAt?.nanos
                    it[completeSec] = p.completedAt?.seconds
                    it[completeNano] = p.completedAt?.nanos
                    it[reason] = p.failureReason
                    it[batchNumber] = p.currentBatchNumber
                    it[batches] = p.totalBatches
                }
            if (updated == 0)
                RolloutsTable.insert {
                    it[id] = p.rolloutId
                    it[task] = p.taskName
                    it[status] = p.status.name
                    it[strategy] = o.strategy.name
                    it[batch] = o.batchSize
                    it[threshold] = o.drainPlayerThreshold
                    it[drainSec] = o.drainTimeout?.seconds
                    it[drainNano] = o.drainTimeout?.nanos
                    it[ready] = o.readinessTimeout.seconds
                    it[readyNano] = o.readinessTimeout.nanos
                    it[bypass] = o.bypassMaxServiceCount
                    it[fallback] = o.fallbackRemainingPlayers
                    it[total] = p.totalServicesToReplace
                    it[started] = p.servicesStarted
                    it[servicesReady] = p.servicesReady
                    it[stopped] = p.servicesStopped
                    it[pending] = codec.encodeToString(p.pendingStopServiceNames)
                    it[newNames] = codec.encodeToString(p.newServiceNames)
                    it[startSec] = p.startedAt?.seconds
                    it[startNano] = p.startedAt?.nanos
                    it[completeSec] = p.completedAt?.seconds
                    it[completeNano] = p.completedAt?.nanos
                    it[reason] = p.failureReason
                    it[batchNumber] = p.currentBatchNumber
                    it[batches] = p.totalBatches
                }
            true
        }

    fun delete(id: String) =
        transaction(sqlDb) { RolloutsTable.deleteWhere { RolloutsTable.id eq id } }

    private fun decode(r: ResultRow): RolloutProgress {
        fun ts(sec: Long?, nano: Int?) = sec?.let {
            Timestamp.newBuilder().setSeconds(it).setNanos(nano ?: 0).build()
        }
        val drain =
            r[RolloutsTable.drainSec]?.let {
                Duration.newBuilder()
                    .setSeconds(it)
                    .setNanos(r[RolloutsTable.drainNano] ?: 0)
                    .build()
            }
        val ready =
            Duration.newBuilder()
                .setSeconds(r[RolloutsTable.ready])
                .setNanos(r[RolloutsTable.readyNano])
                .build()
        val options =
            RolloutOptions(
                RolloutStrategy.valueOf(r[RolloutsTable.strategy]),
                r[RolloutsTable.batch],
                r[RolloutsTable.threshold],
                drain,
                ready,
                r[RolloutsTable.bypass],
                r[RolloutsTable.fallback],
            )
        return RolloutProgress(
            r[RolloutsTable.id],
            r[RolloutsTable.task],
            RolloutStatus.valueOf(r[RolloutsTable.status]),
            options,
            r[RolloutsTable.total],
            r[RolloutsTable.started],
            r[RolloutsTable.servicesReady],
            r[RolloutsTable.stopped],
            codec.decodeFromString(r[RolloutsTable.pending]),
            codec.decodeFromString(r[RolloutsTable.newNames]),
            ts(r[RolloutsTable.startSec], r[RolloutsTable.startNano]),
            ts(r[RolloutsTable.completeSec], r[RolloutsTable.completeNano]),
            r[RolloutsTable.reason],
            r[RolloutsTable.batchNumber],
            r[RolloutsTable.batches],
        )
    }
}

object NodeDrainRepository {
    fun get(name: String): NodeDrainProgress? =
        transaction(sqlDb) {
            DrainsTable.selectAll().where { DrainsTable.node eq name }.firstOrNull()?.let(::decode)
        }

    fun getAll(): List<NodeDrainProgress> =
        transaction(sqlDb) { DrainsTable.selectAll().map(::decode) }

    fun save(p: NodeDrainProgress) =
        transaction(sqlDb) {
            fun ts(t: Timestamp?) = t?.seconds to t?.nanos
            val (ss, sn) = ts(p.startedAt)
            val (ds, dn) = ts(p.deadline)
            val (cs, cn) = ts(p.completedAt)
            val updated =
                DrainsTable.update({ DrainsTable.node eq p.nodeName }) {
                    it[strategy] = p.strategy.name
                    it[status] = p.status.name
                    it[startSec] = ss
                    it[startNano] = sn
                    it[deadlineSec] = ds
                    it[deadlineNano] = dn
                    it[completeSec] = cs
                    it[completeNano] = cn
                    it[threshold] = p.playerThreshold
                    it[total] = p.servicesTotal
                    it[stopped] = p.servicesStopped
                    it[remaining] = p.servicesRemaining
                    it[players] = p.playersRemaining
                    it[failure] = p.failureReason
                    it[reason] = p.drainReason
                }
            if (updated == 0)
                DrainsTable.insert {
                    it[node] = p.nodeName
                    it[strategy] = p.strategy.name
                    it[status] = p.status.name
                    it[startSec] = ss
                    it[startNano] = sn
                    it[deadlineSec] = ds
                    it[deadlineNano] = dn
                    it[completeSec] = cs
                    it[completeNano] = cn
                    it[threshold] = p.playerThreshold
                    it[total] = p.servicesTotal
                    it[stopped] = p.servicesStopped
                    it[remaining] = p.servicesRemaining
                    it[players] = p.playersRemaining
                    it[failure] = p.failureReason
                    it[reason] = p.drainReason
                }
        }

    fun delete(name: String) =
        transaction(sqlDb) { DrainsTable.deleteWhere { DrainsTable.node eq name } }

    private fun decode(r: ResultRow): NodeDrainProgress {
        fun ts(sec: Long?, nano: Int?) = sec?.let {
            Timestamp.newBuilder().setSeconds(it).setNanos(nano ?: 0).build()
        }
        return NodeDrainProgress(
            r[DrainsTable.node],
            NodeDrainStrategy.valueOf(r[DrainsTable.strategy]),
            NodeDrainStatus.valueOf(r[DrainsTable.status]),
            ts(r[DrainsTable.startSec], r[DrainsTable.startNano]),
            ts(r[DrainsTable.deadlineSec], r[DrainsTable.deadlineNano]),
            ts(r[DrainsTable.completeSec], r[DrainsTable.completeNano]),
            r[DrainsTable.threshold],
            r[DrainsTable.total],
            r[DrainsTable.stopped],
            r[DrainsTable.remaining],
            r[DrainsTable.players],
            r[DrainsTable.failure],
            r[DrainsTable.reason],
        )
    }
}

object ChronyxLeaseRepository {
    fun acquire(
        task: String,
        host: String,
        maxGlobal: Int,
        maxHost: Int,
        leaseMillis: Long,
    ): String? =
        transaction(sqlDb) {
            val now = System.currentTimeMillis()
            ChronyxGuardsTable.insertIgnore {
                it[ChronyxGuardsTable.task] = task
                it[touched] = now
            }
            // Updating the per-task guard row holds a database row lock until this transaction
            // commits.
            ChronyxGuardsTable.update({ ChronyxGuardsTable.task eq task }) { it[touched] = now }
            ChronyxLeasesTable.deleteWhere {
                (ChronyxLeasesTable.task eq task) and (ChronyxLeasesTable.expires lessEq now)
            }
            val leases =
                ChronyxLeasesTable.selectAll().where { ChronyxLeasesTable.task eq task }.toList()
            if (
                leases.size >= maxGlobal ||
                    leases.count { it[ChronyxLeasesTable.host] == host } >= maxHost
            )
                return@transaction null
            val token = UUID.randomUUID().toString()
            ChronyxLeasesTable.insert {
                it[ChronyxLeasesTable.task] = task
                it[ChronyxLeasesTable.host] = host
                it[ChronyxLeasesTable.token] = token
                it[ChronyxLeasesTable.acquired] = now
                it[ChronyxLeasesTable.expires] = now + leaseMillis
            }
            token
        }

    fun renew(task: String, host: String, token: String, leaseMillis: Long): Boolean =
        transaction(sqlDb) {
            val now = System.currentTimeMillis()
            ChronyxLeasesTable.update(
                {
                    (ChronyxLeasesTable.task eq task) and
                        (ChronyxLeasesTable.host eq host) and
                        (ChronyxLeasesTable.token eq token) and
                        (ChronyxLeasesTable.expires greater now)
                },
            ) {
                it[expires] = now + leaseMillis
            } > 0
        }

    fun release(task: String, host: String, token: String): Boolean =
        transaction(sqlDb) {
            ChronyxLeasesTable.deleteWhere {
                (ChronyxLeasesTable.task eq task) and
                    (ChronyxLeasesTable.host eq host) and
                    (ChronyxLeasesTable.token eq token)
            } > 0
        }

    fun globalCount(task: String): Int =
        transaction(sqlDb) {
            ChronyxLeasesTable.deleteWhere {
                (ChronyxLeasesTable.task eq task) and
                    (ChronyxLeasesTable.expires lessEq System.currentTimeMillis())
            }
            ChronyxLeasesTable.selectAll().where { ChronyxLeasesTable.task eq task }.count().toInt()
        }

    fun hostCount(task: String, host: String): Int =
        transaction(sqlDb) {
            ChronyxLeasesTable.deleteWhere {
                (ChronyxLeasesTable.task eq task) and
                    (ChronyxLeasesTable.expires lessEq System.currentTimeMillis())
            }
            ChronyxLeasesTable.selectAll()
                .where { (ChronyxLeasesTable.task eq task) and (ChronyxLeasesTable.host eq host) }
                .count()
                .toInt()
        }

    fun lockInfo(task: String): org.vulpesstudios.chronyx.LockInfo? =
        transaction(sqlDb) {
            val now = System.currentTimeMillis()
            ChronyxLeasesTable.deleteWhere {
                (ChronyxLeasesTable.task eq task) and (ChronyxLeasesTable.expires lessEq now)
            }
            ChronyxLeasesTable.selectAll()
                .where { ChronyxLeasesTable.task eq task }
                .minByOrNull { it[ChronyxLeasesTable.acquired] }
                ?.let {
                    org.vulpesstudios.chronyx.LockInfo(
                        task,
                        it[ChronyxLeasesTable.host],
                        it[ChronyxLeasesTable.token],
                        java.time.Instant.ofEpochMilli(it[ChronyxLeasesTable.acquired]),
                        java.time.Instant.ofEpochMilli(it[ChronyxLeasesTable.expires]),
                    )
                }
        }
}

object TemplateJobRepository {
    fun get(id: String): TemplateJobRecord? =
        transaction(sqlDb) {
            TemplateJobsTable.selectAll()
                .where { TemplateJobsTable.id eq id }
                .firstOrNull()
                ?.let(::decode)
        }

    fun getAll(): List<TemplateJobRecord> =
        transaction(sqlDb) { TemplateJobsTable.selectAll().map(::decode) }

    fun save(r: TemplateJobRecord, createOnly: Boolean = false) =
        transaction(sqlDb) {
            val p = r.progress
            val exists =
                TemplateJobsTable.selectAll()
                    .where { TemplateJobsTable.id eq p.jobId }
                    .firstOrNull() != null
            if (exists && createOnly) return@transaction false
            if (exists) TemplateJobsTable.deleteWhere { TemplateJobsTable.id eq p.jobId }
            TemplateJobsTable.insert {
                it[id] = p.jobId
                it[type] = p.type.name
                it[status] = p.status.name
                it[phase] = r.phase.name
                it[progressPhase] = p.phase.name
                it[sourceRef] = codec.encodeToString(p.source)
                it[destination] = codec.encodeToString(p.destination)
                it[conflict] = p.conflictMode.name
                it[extra] = p.extraFilesMode.name
                it[total] = p.totalFiles
                it[processed] = p.filesProcessed
                it[bytes] = p.bytesProcessed
                it[totalBytes] = p.totalBytes
                it[current] = p.currentFile
                it[copied] = p.filesCopied
                it[skipped] = p.filesSkipped
                it[overwritten] = p.filesOverwritten
                it[purged] = p.filesPurged
                it[started] = p.startedAt
                it[completed] = p.completedAt
                it[failure] = p.failureReason
                it[worker] = r.workerToken
                it[inventory] = codec.encodeToString(r.sourceInventory)
                it[directories] = codec.encodeToString(r.sourceDirectoryInventory)
                it[destinationExisted] = r.destinationExisted
                it[initialInventory] = codec.encodeToString(r.initialDestinationInventory)
                it[expectedInventory] = codec.encodeToString(r.expectedDestinationInventory)
                it[completedPaths] = codec.encodeToString(r.completedPaths)
                it[purgedPaths] = codec.encodeToString(r.purgedPaths)
                it[inFlight] = r.inFlightPath
                it[inFlightPurge] = r.inFlightPurgePath
                it[workerNode] = r.workerNodeName
                it[lease] = r.workerLeaseUntil
                it[cancel] = r.cancelRequested
                it[locks] = codec.encodeToString(r.lockKeys)
            }
            true
        }

    fun delete(id: String) =
        transaction(sqlDb) { TemplateJobsTable.deleteWhere { TemplateJobsTable.id eq id } }

    fun claim(id: String, node: String, now: Long, leaseMillis: Long): TemplateJobRecord? =
        transaction(sqlDb) {
            val current =
                TemplateJobsTable.selectAll().where { TemplateJobsTable.id eq id }.firstOrNull()
                    ?: return@transaction null
            if (
                !TemplateJobStatus.valueOf(current[TemplateJobsTable.status]).isActive ||
                    current[TemplateJobsTable.lease] > now
            )
                return@transaction null
            val token = UUID.randomUUID().toString()
            val changed =
                TemplateJobsTable.update(
                    {
                        (TemplateJobsTable.id eq id) and
                            (TemplateJobsTable.lease lessEq now) and
                            (TemplateJobsTable.status inList
                                listOf(
                                    TemplateJobStatus.PENDING.name,
                                    TemplateJobStatus.IN_PROGRESS.name,
                                ))
                    },
                ) {
                    it[worker] = token
                    it[workerNode] = node
                    it[lease] = now + leaseMillis
                }
            if (changed == 0) null
            else
                TemplateJobsTable.selectAll()
                    .where { TemplateJobsTable.id eq id }
                    .firstOrNull()
                    ?.let(::decode)
        }

    private fun decode(r: ResultRow): TemplateJobRecord {
        val p =
            TemplateJobProgress(
                r[TemplateJobsTable.id],
                TemplateJobType.valueOf(r[TemplateJobsTable.type]),
                TemplateJobStatus.valueOf(r[TemplateJobsTable.status]),
                ApiTemplateJobPhase.valueOf(r[TemplateJobsTable.progressPhase]),
                codec.decodeFromString(r[TemplateJobsTable.sourceRef]),
                codec.decodeFromString(r[TemplateJobsTable.destination]),
                TemplateJobConflictMode.valueOf(r[TemplateJobsTable.conflict]),
                TemplateJobExtraFilesMode.valueOf(r[TemplateJobsTable.extra]),
                r[TemplateJobsTable.total],
                r[TemplateJobsTable.processed],
                r[TemplateJobsTable.bytes],
                r[TemplateJobsTable.totalBytes],
                r[TemplateJobsTable.current],
                r[TemplateJobsTable.copied],
                r[TemplateJobsTable.skipped],
                r[TemplateJobsTable.overwritten],
                r[TemplateJobsTable.purged],
                r[TemplateJobsTable.started],
                r[TemplateJobsTable.completed],
                r[TemplateJobsTable.failure],
            )
        return TemplateJobRecord(
            p,
            r[TemplateJobsTable.worker],
            TemplateJobPhase.valueOf(r[TemplateJobsTable.phase]),
            codec.decodeFromString(r[TemplateJobsTable.inventory]),
            codec.decodeFromString(r[TemplateJobsTable.directories]),
            r[TemplateJobsTable.destinationExisted],
            codec.decodeFromString(r[TemplateJobsTable.initialInventory]),
            codec.decodeFromString(r[TemplateJobsTable.expectedInventory]),
            codec.decodeFromString(r[TemplateJobsTable.completedPaths]),
            codec.decodeFromString(r[TemplateJobsTable.purgedPaths]),
            r[TemplateJobsTable.inFlight],
            r[TemplateJobsTable.inFlightPurge],
            r[TemplateJobsTable.workerNode],
            r[TemplateJobsTable.lease],
            r[TemplateJobsTable.cancel],
            codec.decodeFromString(r[TemplateJobsTable.locks]),
        )
    }
}

object TemplateJobLockRepository {
    fun get(key: String): TemplateJobLock? =
        transaction(sqlDb) {
            TemplateLocksTable.selectAll()
                .where { TemplateLocksTable.key eq key }
                .firstOrNull()
                ?.let {
                    TemplateJobLock(
                        it[TemplateLocksTable.job],
                        it[TemplateLocksTable.locked],
                        it[TemplateLocksTable.description],
                        it[TemplateLocksTable.lease],
                    )
                }
        }

    fun getAll(): List<Pair<String, TemplateJobLock>> =
        transaction(sqlDb) {
            TemplateLocksTable.selectAll().map {
                it[TemplateLocksTable.key] to
                    TemplateJobLock(
                        it[TemplateLocksTable.job],
                        it[TemplateLocksTable.locked],
                        it[TemplateLocksTable.description],
                        it[TemplateLocksTable.lease],
                    )
            }
        }

    fun save(key: String, lock: TemplateJobLock) =
        transaction(sqlDb) {
            val updated =
                TemplateLocksTable.update({ TemplateLocksTable.key eq key }) {
                    it[job] = lock.jobId
                    it[locked] = lock.lockedAt
                    it[description] = lock.description
                    it[lease] = lock.leaseUntil
                }
            if (updated == 0)
                TemplateLocksTable.insert {
                    it[this.key] = key
                    it[job] = lock.jobId
                    it[locked] = lock.lockedAt
                    it[description] = lock.description
                    it[lease] = lock.leaseUntil
                }
        }

    fun tryAcquire(key: String, lock: TemplateJobLock, now: Long): Boolean =
        transaction(sqlDb) {
            val current =
                TemplateLocksTable.selectAll().where { TemplateLocksTable.key eq key }.firstOrNull()
            if (current == null) {
                TemplateLocksTable.insertIgnore {
                    it[this.key] = key
                    it[job] = lock.jobId
                    it[locked] = lock.lockedAt
                    it[description] = lock.description
                    it[lease] = lock.leaseUntil
                }
                val inserted =
                    TemplateLocksTable.selectAll()
                        .where { TemplateLocksTable.key eq key }
                        .firstOrNull() ?: return@transaction false
                return@transaction inserted[TemplateLocksTable.job] == lock.jobId
            }
            val owner = current[TemplateLocksTable.job]
            val deadline = current[TemplateLocksTable.lease]
            if (owner == lock.jobId) return@transaction true
            if (deadline == 0L || deadline > now) return@transaction false
            TemplateLocksTable.update(
                {
                    (TemplateLocksTable.key eq key) and
                        (TemplateLocksTable.job eq owner) and
                        (TemplateLocksTable.lease eq deadline)
                },
            ) {
                it[job] = lock.jobId
                it[locked] = lock.lockedAt
                it[description] = lock.description
                it[lease] = lock.leaseUntil
            } > 0
        }

    fun renewOwned(key: String, jobId: String, leaseUntil: Long): Boolean =
        transaction(sqlDb) {
            TemplateLocksTable.update(
                {
                    (TemplateLocksTable.key eq key) and (TemplateLocksTable.job eq jobId)
                },
            ) {
                it[lease] = leaseUntil
            } > 0
        }

    fun deleteOwned(key: String, jobId: String): Boolean =
        transaction(sqlDb) {
            TemplateLocksTable.deleteWhere {
                (TemplateLocksTable.key eq key) and (TemplateLocksTable.job eq jobId)
            } > 0
        }

    fun delete(key: String) =
        transaction(sqlDb) { TemplateLocksTable.deleteWhere { TemplateLocksTable.key eq key } }
}
