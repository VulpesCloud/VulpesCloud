package io.github.thecguygithub.launcher.dependency;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public enum Repository {

    MAVEN_CENTRAL("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar");


    private final String repository;

}