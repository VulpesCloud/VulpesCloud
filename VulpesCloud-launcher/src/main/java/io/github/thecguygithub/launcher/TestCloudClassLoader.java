package io.github.thecguygithub.launcher;

import java.net.URL;
import java.net.URLClassLoader;

public final class TestCloudClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public TestCloudClassLoader() {
        super(new URL[0], ClassLoader.getSystemClassLoader());
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}
