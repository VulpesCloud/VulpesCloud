package io.github.thecguygithub.api.tasks;

import io.github.thecguygithub.api.JavaCloudAPI;
import io.github.thecguygithub.api.platforms.PlatformGroupDisplay;
import io.github.thecguygithub.api.services.ClusterService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public abstract class AbstractClusterTask implements ClusterTask {

    private final String name;
    private final PlatformGroupDisplay platform;

    private String[] templates;
    private String[] nodes;
    private int maxMemory;
    private int maxPlayers;
    private boolean staticService;
    private int minOnlineCount;
    private boolean maintenance;

    // private final PropertiesPool properties;

    @Override
    public @NotNull String details() {
        var defaultDetail = "platform&8=&7" + platform.details() + "&8, &7nodes&8=&7" + Arrays.toString(nodes) + ", &7maxMemory&8=&7" + maxMemory + "&8, &7static&8=&7" + staticService;

        if(this instanceof FallbackClusterTask) {
            defaultDetail += " &8--fallback";
        }

        return defaultDetail;
    }

    @Override
    public long serviceCount() {
        return Objects.requireNonNull(JavaCloudAPI.getInstance().serviceProvider().services()).stream().filter(it -> it.group().equals(this)).count();
    }

    @Override
    public List<ClusterService> services() {
        return Objects.requireNonNull(JavaCloudAPI.getInstance().serviceProvider().services()).stream().filter(it -> it.group().equals(this)).toList();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClusterTask clusterGroup && clusterGroup.name().equals(name);
    }
}
