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
        var apiFile = Path.of("local/dependencies/vulpescloud-api.jar");

        Launcher.CLASS_LOADER.addURL(apiFile.toFile().toURI().toURL());

        var gsonDependency = new Dependency("com.google.code.gson", "gson", "2.11.0");
        var jLineDependency = new  Dependency("org.jline", "jline", "3.27.0");
        var kotlinSTD = new  Dependency("org.jetbrains.kotlin", "kotlin-stdlib", "2.0.0");
        var jsonDependency = new Dependency("org.json", "json", "20240303");
        var jedisDependency = new Dependency("redis.clients", "jedis", "5.2.0");
        var slf4jDependency = new Dependency("org.slf4j", "slf4j-api", "2.0.16");
        var logbackCoreDependency = new Dependency("ch.qos.logback", "logback-core", "1.5.11");
        var logbackClassicDependency = new Dependency("ch.qos.logback", "logback-classic", "1.5.11");
        var hikariCP = new Dependency("com.zaxxer", "HikariCP", "5.1.0");
        var mariaDB = new Dependency("org.mariadb.jdbc", "mariadb-java-client", "3.4.0");
        var cloud = new Dependency("org.incendo", "cloud-core", "2.0.0");
        var cloudExtension = new Dependency("org.incendo", "cloud-kotlin-extensions", "2.0.0");
        var cloudAnnotations = new Dependency("org.incendo", "cloud-annotations", "2.0.0");
        var cloudKotlinCoroutines = new Dependency("org.incendo", "cloud-kotlin-coroutines", "2.0.0");
        var cloudKotlinCoroutinesAnnotations = new Dependency("org.incendo", "cloud-kotlin-coroutines-annotations", "2.0.0");


        // add boot file to the current classpath
        CLASS_LOADER.addURL(boot.bootFile().toURI().toURL());

        DependencyDownloader.download(cloud, cloudExtension, cloudAnnotations, cloudKotlinCoroutines, cloudKotlinCoroutinesAnnotations, mariaDB, hikariCP, jLineDependency, kotlinSTD, gsonDependency, jsonDependency, jedisDependency, slf4jDependency, logbackClassicDependency, logbackCoreDependency);

        Thread.currentThread().setContextClassLoader(CLASS_LOADER);
        Class.forName(boot.mainClass(), true, CLASS_LOADER).getMethod("main", String[].class).invoke(null, (Object) args);
    }
}
