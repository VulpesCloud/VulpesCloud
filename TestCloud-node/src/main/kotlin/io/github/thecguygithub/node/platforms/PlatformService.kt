package io.github.thecguygithub.node.platforms

import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.util.JsonUtils
import io.github.thecguygithub.node.util.StringUtils
import io.github.thecguygithub.node.platforms.Platform

import lombok.SneakyThrows
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path


class PlatformService @SneakyThrows constructor() {
    val logger = Logger()
    var platforms: List<Platform>

    init {

        logger.debug("Writing VERSION_PATH.json")

        if (!Files.exists(VERISON_PATH)) {
            Files.writeString(VERISON_PATH, String(URL(VERSIONS_URL).openStream().readAllBytes()))
        }
        logger.debug("Reading Json")


        this.platforms = JsonUtils.GSON.fromJson(
            Files.readString(VERISON_PATH),
            PlatformConfig::class.java
        ).platforms!!

        logger.debug("Read Json Successfully! " )
        logger.info("Loading ${platforms.size} platforms with ${versionsAmount()} versions.")
    }

    fun find(platformId: String?): Platform? {
        return platforms.stream().filter { it: Platform? -> it?.id.equals(platformId, true) }.findFirst()
            .orElse(null)
    }

    fun versionsAmount(): Int {
        return platforms.stream().mapToInt { it: Platform? -> it?.versions?.size!! }.sum()
    }

    @SneakyThrows
    fun update() {
        Files.writeString(
            VERISON_PATH, JsonUtils.GSON.toJson(
                PlatformConfig(
                    this.platforms
                )
            )
        )
    }

    companion object {
        public var FORWARDING_SECRET: String = StringUtils.randomString(8)

        private const val VERSIONS_URL = "https://raw.githubusercontent.com/HttpMarco/polocloud/master/versions.json"
        private val VERISON_PATH: Path = Path.of("local/versions.json")
    }
}