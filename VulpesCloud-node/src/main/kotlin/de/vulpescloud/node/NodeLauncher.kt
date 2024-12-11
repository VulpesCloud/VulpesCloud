package de.vulpescloud.node

class NodeLauncher {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                Node()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}