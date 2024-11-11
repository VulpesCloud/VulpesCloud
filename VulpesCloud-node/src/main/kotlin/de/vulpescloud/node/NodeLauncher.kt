package de.vulpescloud.node




class NodeLauncher {



    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                println("setting startup")
                System.setProperty("startup", System.currentTimeMillis().toString())
                println("Starting Node!")
                Node()
            } catch (exception: Exception) {
                // check for remove
                exception.printStackTrace()
            }
        }
    }

}