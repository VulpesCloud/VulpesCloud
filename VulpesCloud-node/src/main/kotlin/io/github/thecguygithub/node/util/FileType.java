package io.github.thecguygithub.node.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.function.Function;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public enum FileType {

    TOML("toml", it -> it.first() + " = \"" + it.second() + "\""),
    YAML("yml", it -> it.first() + ": " + it.second()),
    PROPERTIES("properties", it -> it.first() + "=" + it.second()),
    JSON("json", it -> it.first() + ": " + it.second());

    final String ending;
    public final Function<Pair<String, String>, String> replacer; // Change this to public

    public static FileType define(String file) {
        return Arrays.stream(values()).filter(it -> file.toLowerCase().endsWith("." + it.ending)).findFirst().orElse(null);
    }
}
