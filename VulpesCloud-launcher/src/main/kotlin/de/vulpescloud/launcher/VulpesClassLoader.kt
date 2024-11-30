package de.vulpescloud.launcher

import java.net.URL
import java.net.URLClassLoader

class VulpesClassLoader : URLClassLoader(arrayOfNulls(0), getSystemClassLoader()) {

    public override fun addURL(url: URL) {
        super.addURL(url)
    }

    companion object {
        init {
            registerAsParallelCapable()
        }
    }
}