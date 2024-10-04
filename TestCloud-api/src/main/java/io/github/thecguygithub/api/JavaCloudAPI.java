package io.github.thecguygithub.api;

import io.github.thecguygithub.api.services.ClusterServiceProvider;
import io.github.thecguygithub.api.tasks.ClusterTaskProvider;
import lombok.Getter;
import lombok.experimental.Accessors;

// @Accessors(fluent = true)
public abstract class JavaCloudAPI {

    @Getter
    static JavaCloudAPI instance;

    public JavaCloudAPI() {
        instance = this;
    }

    public abstract ClusterServiceProvider serviceProvider();

    public abstract ClusterTaskProvider taskProvider();

    // public abstract EventProvider eventProvider();

    // public abstract ClusterPlayerProvider playerProvider();

}
