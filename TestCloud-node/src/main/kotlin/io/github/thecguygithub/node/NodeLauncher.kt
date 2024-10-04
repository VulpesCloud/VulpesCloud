package io.github.thecguygithub.node




class NodeLauncher {



    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                println("setting startup")
                System.setProperty("startup", System.currentTimeMillis().toString())
                println("Starting Node!")
                Node.instance?.init()
            } catch (exception: Exception) {
                // check for remove
                exception.printStackTrace()
            }
        }
    }

}