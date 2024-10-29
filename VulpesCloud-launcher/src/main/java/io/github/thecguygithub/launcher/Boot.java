package io.github.thecguygithub.launcher;

import io.github.thecguygithub.launcher.util.FileSystemUtil;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public final class Boot {


    private static final Path DEPENDENCY_DIR = Path.of("local/dependencies");

    @SneakyThrows
    public @NotNull File bootFile() {

        this.copyBootFiles("api",  "node", "wrapper");

        return DEPENDENCY_DIR.resolve("vulpescloud-node.jar").toFile();
    }

    public String mainClass() {
        try (var jarFile = new JarFile(bootFile())) {
            var manifest = jarFile.getManifest();
            if (manifest != null) {
                var mainAttributes = manifest.getMainAttributes();
                return mainAttributes.getValue(Attributes.Name.MAIN_CLASS);
            } else {
                throw new RuntimeException(new NullPointerException("No main class detectable!"));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void copyBootFile(String name) {
        FileSystemUtil.copyClassPathFile(ClassLoader.getSystemClassLoader(), "vulpescloud-" + name + ".jar", Path.of("local/dependencies/vulpescloud-" + name + ".jar").toString());
    }

    private void copyBootFiles(String @NotNull ... names) {
        for (var name : names) {
            this.copyBootFile(name);
        }
    }
}