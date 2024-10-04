package io.github.thecguygithub.launcher;

import io.github.thecguygithub.launcher.dependency.Dependency;
import io.github.thecguygithub.launcher.dependency.DependencyDownloader;
import lombok.SneakyThrows;

import java.nio.file.Path;

public final class Launcher {

    public static TestCloudClassLoader CLASS_LOADER = new TestCloudClassLoader();

    @SneakyThrows
    public static void main(String[] args) {


        var boot = new Boot();
        var apiFile = Path.of("local/dependencies/testcloud-api.jar");

        Launcher.CLASS_LOADER.addURL(apiFile.toFile().toURI().toURL());

        var gsonDependency = new Dependency("com.google.code.gson", "gson", "2.11.0");
        var jLineDependency = new  Dependency("org.jline", "jline", "3.27.0");
        var kotlinSTD = new  Dependency("org.jetbrains.kotlin", "kotlin-stdlib", "2.0.0");

        // add boot file to the current classpath
        CLASS_LOADER.addURL(boot.bootFile().toURI().toURL());

        DependencyDownloader.download(jLineDependency, kotlinSTD, gsonDependency);

        Thread.currentThread().setContextClassLoader(CLASS_LOADER);
        Class.forName(boot.mainClass(), true, CLASS_LOADER).getMethod("main", String[].class).invoke(null, (Object) args);
    }
}