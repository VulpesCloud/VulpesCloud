package io.github.thecguygithub.node.tasks

import io.github.thecguygithub.api.Reloadable
import io.github.thecguygithub.api.tasks.ClusterTask
import io.github.thecguygithub.api.tasks.ClusterTaskProvider
import java.util.*
import java.util.concurrent.CompletableFuture


class ClusterTaskProviderImpl() : ClusterTaskProvider(), Reloadable {

    val groups: MutableSet<ClusterTask> = ClusterGroupFactory.readGroups().toMutableSet()

    init {
        val channelTransmit = clusterProvider.localNode().transmit()

        channelTransmit.listen(GroupCreatePacket::class.java) { _, packet ->
            ClusterGroupFactory.createLocalStorageGroup(packet, this)
        }

        channelTransmit.listen(GroupDeletePacket::class.java) { _, packet ->
            ClusterGroupFactory.deleteLocalStorageGroup(packet.name, this)
        }

        channelTransmit.responder("group-delete") { property ->
            try {
                val name = property.getString("name")
                val deletionRequest = GroupDeletionRequest.request(clusterProvider, name).get().get()
                GroupDeletePacket(deletionRequest)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            } catch (e: ExecutionException) {
                throw RuntimeException(e)
            }
        }

        channelTransmit.responder("group-finding") { property ->
            val name = property.getString("name")
            SingleGroupPacket(find(name))
        }

        channelTransmit.responder("group-exists") { property ->
            val name = property.getString("name")
            GroupExistsResponsePacket(exists(name))
        }

        channelTransmit.responder("groups-all") {
            GroupCollectionPacket(groups())
        }

        channelTransmit.responder(GroupCreationRequest.TAG) { property ->
            GroupCreationResponder.handle(this, clusterProvider, property)
        }

        channelTransmit.responder(GroupDeletionRequest.TAG) { property ->
            GroupDeletionResponder.handle(this, clusterProvider, property)
        }

        channelTransmit.listen(GroupRequestUpdatePacket::class.java) { _, packet ->
            if (Node.instance().clusterProvider().localHead()) {
                Node.instance().clusterProvider().broadcastAll(GroupUpdatePacket(packet.group))
            } else {
                Node.instance().clusterProvider().headNode().transmit().sendPacket(packet)
            }
        }

        channelTransmit.listen(GroupUpdatePacket::class.java) { _, packet ->
            val currentGroup = find(packet.group.name)
            currentGroup.properties.pool().clear()
            currentGroup.properties.pool().putAll(packet.group.properties.pool())
            ClusterGroupFactory.updateLocalStorageGroup(currentGroup)
        }

        if (groups.isNotEmpty()) {
            log.info("Loading following groups: {}", groups.joinToString(", ") { it.name() })
        }
    }

    override fun groupsAsync(): CompletableFuture<Set<ClusterTask?>?> {
        return CompletableFuture.completedFuture(groups)
    }

    override fun existsAsync(group: String?): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(groups.any { it.name().equals(group, ignoreCase = true) })
    }

    override fun deleteAsync(group: String?): CompletableFuture<Optional<String?>?> {
        return GroupDeletionRequest.request(clusterProvider, group)
    }

    override fun createAsync(
        name: String,
        nodes: Array<String>,
        platform: PlatformGroupDisplay,
        maxMemory: Int,
        staticService: Boolean,
        minOnline: Int,
        maxOnline: Int
    ): CompletableFuture<Optional<String>> {
        return GroupCreationRequest.request(clusterProvider, name, nodes, platform, maxMemory, staticService, minOnline, maxOnline)
    }

    override fun findAsync(group: String): CompletableFuture<ClusterGroup> {
        return CompletableFuture.completedFuture(groups.firstOrNull { it.name().equals(group, ignoreCase = true) })
    }

    override fun reload() {
        groups.clear()
        if (Node.instance().clusterProvider().localHead()) {
            groups.addAll(ClusterGroupFactory.readGroups())
        } else {
            Node.instance().clusterProvider().headNode().transmit().request("groups-all", GroupCollectionPacket::class.java) {
                groups.addAll(it.groups)
                log.info("Successfully reload all group data.")
            }
            return
        }
        log.info("Successfully reload all group data.")
    }

    override fun read(buffer: PacketBuffer): ClusterGroup {
        val name = buffer.readString()
        val maxMemory = buffer.readInt()
        val maxPlayers = buffer.readInt()
        val minOnlineServerInstances = buffer.readInt()
        val maxOnlineServerInstances = buffer.readInt()
        val staticService = buffer.readBoolean()
        val platform = PlatformGroupDisplay(buffer.readString(), buffer.readString(), buffer.readEnum(PlatformType::class.java))
        val nodes = Array(buffer.readInt()) { buffer.readString() }
        val templates = Array(buffer.readInt()) { buffer.readString() }
        val propertiesPool = PropertiesPool().apply { PropertiesBuffer.read(buffer, this) }

        return ClusterGroupImpl(name, platform, templates, nodes, maxMemory, maxPlayers, staticService, minOnlineServerInstances, maxOnlineServerInstances, propertiesPool)
    }
}