package io.github.thecguygithub.node.terminal.setup.impl;

import io.github.thecguygithub.api.platforms.PlatformTypes;
import io.github.thecguygithub.node.Node;
import io.github.thecguygithub.node.platforms.Platform;
import io.github.thecguygithub.node.platforms.versions.PlatformVersion;
import io.github.thecguygithub.node.tasks.TaskJson;
import io.github.thecguygithub.node.terminal.setup.Setup;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TaskSetup extends Setup {

    public TaskSetup() {
        super("Task-Setup");

        // todo custom error messages

        question("name", "What is the name of the group&8?", s -> !Objects.requireNonNull(Node.Companion.getTaskProvider()).exists(s.first()));

        question("platforms", "Which platform do you like to use&8?",
                it -> Objects.requireNonNull(Node.Companion.getPlatformService()).getPlatforms().stream().map(Platform::getId).toList(),
                rawInput -> Objects.requireNonNull(Node.Companion.getPlatformService()).find(rawInput.first()) != null);

        question("version", "Select a version&8",
                it -> Objects.requireNonNull(Objects.requireNonNull(Node.Companion.getPlatformService()).find(it.get("platforms")))
                        .getVersions()
                        .stream().map(PlatformVersion::getVersion).toList(),
                context -> {
                    var platform = Objects.requireNonNull(Node.Companion.getPlatformService()).find(context.second().get("platforms"));
                    assert platform != null;
                    var proof = platform.getVersions().stream().anyMatch(it -> Objects.equals(it.getVersion(), context.first()));

                    // add the fallback question
                    if (proof && platform.getType() == PlatformTypes.SERVER) {
                        question("fallback", "Is the given group a fallback group?", it -> List.of("false", "true"), it -> it.first().equalsIgnoreCase("true") || it.first().equalsIgnoreCase("false"));
                    }
                    return proof;
                });
        question("startPort", "At what Port are Services of this Task supposed to start?", it -> isNumber(it.first()));

        question("maxMemory", "Select the value of the maximum memory of one service (mb)", it -> List.of("512", "1024", "2048", "4096"), it -> isNumber(it.first()));
        question("staticService", "Is the service a static Service? (Static services are not be reset on a restart)", it -> List.of("true", "false"), it -> it.first().equalsIgnoreCase("true") || it.first().equalsIgnoreCase("false"));
        question("minOnlineServices", "How many service should be minimal online?", it -> List.of(), it -> isNumber(it.first()));
        question("maintenance", "Should this Task be in maintenance? This prevents auto start of Services!", it -> List.of(), it -> isNumber(it.first()));
    }

    public boolean isNumber(String number) {
        try {
            Integer.parseInt(number);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void complete(@NotNull Map<String, String> context) {
        var name = context.get("name");
        var platform = Objects.requireNonNull(Node.Companion.getPlatformService()).find(context.get("platforms"));
        assert platform != null;
        var version = platform.getVersions().stream().filter(it -> Objects.requireNonNull(it.getVersion()).equalsIgnoreCase(context.get("version"))).findFirst().orElseThrow();
        var maxMemory = Integer.parseInt(context.get("maxMemory"));
        var staticService = Boolean.parseBoolean(context.get("staticService"));
        var minOnlineServices = Integer.parseInt(context.get("minOnlineServices"));
        var maintenance = Boolean.parseBoolean(context.get("maintenance"));
        var fallbackGroup = context.containsKey("fallback") && (Boolean.parseBoolean(context.get("fallback")));
        var startPort = Integer.parseInt(context.get("startPort"));

        var json = TaskJson.INSTANCE.createGroupJson(name, platform, "version", maxMemory, staticService, minOnlineServices, fallbackGroup, maintenance, startPort);

        Objects.requireNonNull(Node.Companion.getRedisController()).sendMessage(json.toString(), "testcloud-events-group-create");

        Node.Companion.getLogger().info("You successfully created the group &8'&f" + name + "&8'");
    }
}
