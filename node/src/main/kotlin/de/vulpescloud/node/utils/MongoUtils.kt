package de.vulpescloud.node.utils

import at.favre.lib.crypto.bcrypt.BCrypt
import build.buf.gen.vulpescloud.virtualconfig.v1.VirtualConfig
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import de.vulpescloud.api.services.Service
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.grpc.security.model.GroupModel
import de.vulpescloud.node.grpc.security.model.UserModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.BsonDocument
import org.bson.BsonInt64
import org.bson.BsonString

object MongoUtils {

    suspend fun updateTask(task: Task) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "tasks"
                )

        val filter = BsonDocument("name", BsonString(task.name))
        val existingTaskDoc = collection.find(filter).firstOrNull()
        val existingTask = existingTaskDoc?.let { Task.fromDocument(it) }

        if (existingTask != null) {
            val updateDoc = BsonDocument($$"$set", task.toDocument())
            collection.updateOne(filter, updateDoc)
        } else {
            collection.insertOne(task.toDocument())
        }
    }

    suspend fun updateService(service: Service) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "services"
                )
        val filter = BsonDocument("uuid", BsonString(service.uuid.toString()))
        val existingServiceDoc = collection.find(filter).firstOrNull()
        val existingService = existingServiceDoc?.let { Service.fromDocument(it) }

        if (existingService != null) {
            val updateDoc = BsonDocument($$"$set", service.toDocument())
            collection.updateOne(filter, updateDoc)
        } else {
            collection.insertOne(service.toDocument())
        }
    }

    suspend fun deleteTask(task: Task) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "tasks"
                )
        val filter = BsonDocument("name", BsonString(task.name))
        val existingTaskDoc = collection.find(filter).firstOrNull()
        val existingTask = existingTaskDoc?.let { Task.fromDocument(it) }
        if (existingTask == null) {
            return
        }
        collection.deleteOne(filter)
    }

    suspend fun deleteService(service: Service) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "services"
                )
        val filter = BsonDocument("uuid", BsonString(service.uuid.toString()))
        val existingServiceDoc = collection.find(filter).firstOrNull()
        val existingService = existingServiceDoc?.let { Service.fromDocument(it) }
        if (existingService == null) {
            return
        }
        collection.deleteOne(filter)
    }

    suspend fun updateOrInsertVirtualConfig(config: VirtualConfig) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "virtualconfigs"
                )

        val filter = BsonDocument("name", BsonString(config.name))
        val existingDoc = collection.find(filter).firstOrNull()
        if (existingDoc == null) {
            collection.insertOne(
                BsonDocument().apply {
                    put("name", BsonString(config.name))
                    put("createdAt", BsonInt64(config.createdAt))
                    put("lastUpdatedAt", BsonInt64(config.lastUpdatedAt))
                    put("config", BsonString(config.config))
                }
            )
        } else {
            val updateDoc =
                BsonDocument(
                    $$"$set",
                    BsonDocument().apply {
                        put("name", BsonString(config.name))
                        put("createdAt", BsonInt64(config.createdAt))
                        put("lastUpdatedAt", BsonInt64(config.lastUpdatedAt))
                        put("config", BsonString(config.config))
                    },
                )
            collection.updateOne(filter, updateDoc)
        }
    }

    suspend fun nothingOrInsertVirtualConfig(config: VirtualConfig) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "virtualconfigs"
                )

        val filter = BsonDocument("name", BsonString(config.name))
        val existingDoc = collection.find(filter).firstOrNull()
        if (existingDoc == null) {
            collection.insertOne(
                BsonDocument().apply {
                    put("name", BsonString(config.name))
                    put("createdAt", BsonInt64(config.createdAt))
                    put("lastUpdatedAt", BsonInt64(config.lastUpdatedAt))
                    put("config", BsonString(config.config))
                }
            )
        }
    }

    suspend fun deleteVirtualConfig(config: VirtualConfig) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "virtualconfigs"
                )
        val filter = BsonDocument("name", BsonString(config.name))
        val existingDoc = collection.find(filter).firstOrNull() ?: return
        collection.deleteOne(filter)
    }

    suspend fun getUserByName(name: String): UserModel? {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<UserModel>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "users"
                )
        val filter = BsonDocument("name", BsonString(name))
        val existingDoc = collection.find(filter).firstOrNull() ?: return null
        return existingDoc
    }

    suspend fun getGroupByName(name: String): GroupModel? {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<GroupModel>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "groups"
                )
        val filter = BsonDocument("name", BsonString(name))
        val existingDoc = collection.find(filter).firstOrNull() ?: return null
        return existingDoc
    }

    suspend fun createGroup(name: String, permissions: List<String> = listOf()) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<GroupModel>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "groups"
                )
        val filter = BsonDocument("name", BsonString(name))
        val existingDoc = collection.find(filter).firstOrNull()
        if (existingDoc != null) {
            return
        }
        collection.insertOne(GroupModel(name, permissions))
    }

    suspend fun createUser(name: String, password: String) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<UserModel>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "users"
                )
        val filter = BsonDocument("name", BsonString(name))
        val existingDoc = collection.find(filter).firstOrNull()
        if (existingDoc != null) {
            return
        }
        val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        collection.insertOne(UserModel(name, hashedPassword))
    }

    suspend fun deleteUser(name: String) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<UserModel>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "users"
                )
        val filter = BsonDocument("name", BsonString(name))
        collection.deleteOne(filter)
    }

    suspend fun deleteGroup(name: String) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<GroupModel>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "groups"
                )
        val filter = BsonDocument("name", BsonString(name))
        collection.deleteOne(filter)
    }

    suspend fun updateUserPassword(name: String, password: String) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<UserModel>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "users"
                )

        val filter = BsonDocument("name", BsonString(name))
        val existingDoc = collection.find(filter).firstOrNull() ?: return

        val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray())

        collection.updateOne(filter, Updates.set("password", hashedPassword))
    }

    suspend fun addPermissionToUser(username: String, permission: String) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<UserModel>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "users"
                )
        val filter = Filters.eq("name", username)
        collection.updateOne(filter, Updates.addToSet("permissions", permission))
    }

    suspend fun removePermissionFromUser(username: String, permission: String) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<UserModel>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "users"
                )
        val filter = Filters.eq("name", username)
        collection.updateOne(filter, Updates.pull("permissions", permission))
    }

    suspend fun addPermissionToGroup(groupName: String, permission: String) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<GroupModel>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "groups"
                )
        val filter = Filters.eq("name", groupName)
        collection.updateOne(filter, Updates.addToSet("permissions", permission))
    }

    suspend fun removePermissionFromGroup(groupName: String, permission: String) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<GroupModel>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "groups"
                )
        val filter = Filters.eq("name", groupName)
        collection.updateOne(filter, Updates.pull("permissions", permission))
    }

    suspend fun checkUserPassword(username: String, password: String): Boolean {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<UserModel>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "users"
                )
        val user = collection.find(Filters.eq("name", username)).firstOrNull() ?: return false

        val result = BCrypt.verifyer().verify(password.toCharArray(), user.password)
        return result.verified
    }

    suspend fun getAllUsers(): List<UserModel> {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<UserModel>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "users"
                )
        return collection.find().toList()
    }

    suspend fun getAllGroups(): List<GroupModel> {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<GroupModel>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "groups"
                )
        return collection.find().toList()
    }

    suspend fun addUserToGroup(username: String, groupName: String) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<UserModel>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "users"
                )
        val filter = Filters.eq("name", username)
        collection.updateOne(filter, Updates.addToSet("groups", groupName))
    }

    suspend fun removeUserFromGroup(username: String, groupName: String) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<UserModel>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "users"
                )
        val filter = Filters.eq("name", username)
        collection.updateOne(filter, Updates.pull("groups", groupName))
    }
}
