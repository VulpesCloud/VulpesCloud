package de.vulpescloud.node.utils

import de.vulpescloud.api.services.Service
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import kotlinx.coroutines.flow.firstOrNull
import org.bson.BsonDocument
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
            // Verwende $set Operator für Updates
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
}
