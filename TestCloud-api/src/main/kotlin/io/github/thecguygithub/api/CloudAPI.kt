package io.github.thecguygithub.api

import io.github.thecguygithub.api.tasks.ClusterTaskProvider
import lombok.Getter
import lombok.experimental.Accessors


@Accessors(fluent = true)
abstract class CloudAPI {
    // abstract fun serviceProvider(): ClusterServiceProvider?

    abstract var taskProvider: ClusterTaskProvider?

    // abstract fun eventProvider(): EventProvider?

    // abstract fun playerProvider(): ClusterPlayerProvider?

    init {
        instance = this

        this.taskProvider = ClusterTaskProvider
    }

    companion object {
        @Getter
        lateinit var instance: CloudAPI
    }
}