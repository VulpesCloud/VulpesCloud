package de.vulpescloud.node

class NodeLauncher {
    companion object {
        @JvmStatic
        fun main(string: Array<String>) {
            try {
                Node()
            } catch (e: Exception) {
                println("Something went wrong: $e")
                e.printStackTrace()
            }
        }
    }
}