package io.github.thecguygithub.node.tasks

import io.github.thecguygithub.api.services.ClusterService
import io.github.thecguygithub.api.tasks.ClusterTask
import io.github.thecguygithub.api.tasks.ClusterTaskProvider
import io.github.thecguygithub.node.util.JsonUtils
import lombok.SneakyThrows
import java.nio.file.Files
import java.nio.file.Path
import java.util.*


object ClusterTaskFactory {

    val GROUP_DIR: Path = Path.of("local/groups")

    @SneakyThrows
    fun createLocalStorageGroup(packet: GroupCreatePacket, clusterTaskProvider: ClusterTaskProvider) {
        var group = ClusterGroupImpl(
            packet.name(),
            packet.platformGroupDisplay(),
            packet.templates(),
            packet.nodes(),
            packet.maxMemory(),
            100,
            packet.staticService(),
            packet.minOnline(),
            packet.maxOnline(),
            PropertiesPool()
        )

        if (packet.fallbackGroup()) {
            group = ClusterGroupFallbackImpl(group)
        }

        // check every creation, if directory exists
        GROUP_DIR.toFile().mkdirs()
        updateLocalStorageGroup(group)

        // Node.instance().templatesProvider().prepareTemplate(group.templates())

        clusterTaskProvider.groups()
    }

    @SneakyThrows
    fun deleteLocalStorageGroup(name: String, clusterGroupProvider: ClusterTaskProvider) {
        val clusterGroup: ClusterTask = clusterGroupProvider.find(name) ?: return

        val groupFile: Path = GROUP_DIR.resolve("$name.json")
        Files.deleteIfExists(groupFile)

        clusterGroup.services()?.forEach { obj: ClusterService? -> obj?.shutdown() }
        clusterGroupProvider.groups().removeIf { group -> group.name().equalsIgnoreCase(name) }
    }

    @SneakyThrows
    fun updateLocalStorageGroup(task: ClusterTask) {
        val taskFile: Path = GROUP_DIR.resolve(task.name() + ".json")
        Files.writeString(taskFile, JsonUtils.GSON.toJson(task))
    }

    @SneakyThrows
    fun readGroups(): Set<ClusterTask> {
        val groups: HashSet<ClusterTask> = HashSet<ClusterTask>()

        if (!Files.exists(GROUP_DIR)) {
            return groups
        }

        for (file in Objects.requireNonNull<T>(GROUP_DIR.toFile().listFiles())) {
            groups.add(
                JsonUtils.GSON.fromJson<T>(
                    Files.readString(file.toPath()),
                    ClusterTask::class.java
                )
            )
        }
        return groups
    }


}