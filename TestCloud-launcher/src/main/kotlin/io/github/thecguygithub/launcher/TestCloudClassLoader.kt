package io.github.thecguygithub.launcher

import java.net.URL
import java.net.URLClassLoader

class TestCloudClassLoader : URLClassLoader(emptyArray(), ClassLoader.getSystemClassLoader()) {

    companion object {
        init {
            ClassLoader.registerAsParallelCapable()
        }
    }

    public override fun addURL(url: URL) {
        super.addURL(url)
    }


}