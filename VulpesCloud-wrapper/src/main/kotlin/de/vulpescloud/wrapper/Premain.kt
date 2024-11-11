package de.vulpescloud.wrapper

import java.lang.instrument.Instrumentation



object Premain {

    lateinit var INSTRUMENTATION: Instrumentation

    @JvmStatic
    fun premain(args: String?, instrumentation: Instrumentation) {
        println("premain yay")
        INSTRUMENTATION = instrumentation
    }

}