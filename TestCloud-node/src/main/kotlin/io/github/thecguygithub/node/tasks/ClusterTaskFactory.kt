package io.github.thecguygithub.node.tasks

import io.github.thecguygithub.api.platforms.PlatformGroupDisplay
import io.github.thecguygithub.api.platforms.PlatformTypes
import io.github.thecguygithub.api.services.ClusterService
import io.github.thecguygithub.api.tasks.ClusterTask
import io.github.thecguygithub.api.tasks.ClusterTaskProvider
import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.util.JsonUtils
import lombok.SneakyThrows
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path
import java.util.*


object ClusterTaskFactory {

    private val GROUP_DIR: Path = Path.of("local/groups")
    private val logger = Logger()

    @SneakyThrows
    fun createLocalStorageGroup(grouInfo: JSONObject, clusterTaskProvider: ClusterTaskProvider) {

        val pGoupJson = grouInfo.getJSONObject("platformGroupDisplay")
        val pGroup = PlatformGroupDisplay(pGoupJson.getString("name"), pGoupJson.getString("version"), PlatformTypes.valueOf(pGoupJson.getString("platformType")))

        val nodes = grouInfo.getJSONArray("nodes")
        val nodesArray: Array<String?> = Array(nodes.length()) {
            if (nodes.isNull(it)) null else nodes.getString(it)
        }
        val templates = grouInfo.getJSONArray("nodes")
        val templatesArray: Array<String?> = Array(templates.length()) {
            if (templates.isNull(it)) null else templates.getString(it)
        }

        logger.debug("Setting group!")
        val group = ClusterGroupImpl(
            grouInfo.getString("name"),
            pGroup,
            nodesArray,
            templatesArray,
            grouInfo.getInt("maxMemory"),
            100,
            grouInfo.getBoolean("staticService"),
            grouInfo.getInt("minOnline"),
            grouInfo.getBoolean("maintenance"),
        )
        logger.debug("Making dirs and Updating local storage")

        // check every creation, if directory exists
        GROUP_DIR.toFile().mkdirs()
        updateLocalStorageGroup(group)
        logger.debug("Adding to Groups")

        // Node.instance().templatesProvider().prepareTemplate(group.templates())

        clusterTaskProvider.groups()?.add(group)
    }

    @SneakyThrows
    fun deleteLocalStorageGroup(name: String, clusterGroupProvider: ClusterTaskProvider) {
        val clusterGroup: ClusterTask = clusterGroupProvider.find(name) ?: return

        val groupFile: Path = GROUP_DIR.resolve("$name.json")
        Files.deleteIfExists(groupFile)

        clusterGroup.services()?.forEach { obj: ClusterService? -> obj?.shutdown() }
        clusterGroupProvider.groups()?.removeIf { it?.name().equals(name, true) }
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

        for (file in Objects.requireNonNull(GROUP_DIR.toFile().listFiles())) {
            groups.add(
                JsonUtils.GSON.fromJson(
                    Files.readString(file.toPath()),
                    ClusterTask::class.java
                )
            )
        }
        return groups
    }


}