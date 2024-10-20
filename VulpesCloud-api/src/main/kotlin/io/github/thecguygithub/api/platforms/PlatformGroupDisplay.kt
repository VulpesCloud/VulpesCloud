package io.github.thecguygithub.api.platforms

import io.github.thecguygithub.api.Detail
import org.jetbrains.annotations.Contract


class PlatformGroupDisplay(val platform: String, val version: String, val type: PlatformTypes) : Detail {
    @Contract(pure = true)
    override fun details(): String {
        return "$platform-$version"
    }

    @Contract(pure = true)
    fun platformJarName(): String {
        return details() + ".jar"
    }

}