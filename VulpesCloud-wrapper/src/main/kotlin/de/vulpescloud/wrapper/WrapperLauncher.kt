package de.vulpescloud.wrapper

class WrapperLauncher {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.out.println("Uhm yhea idk")
            println("WrapperLauncher is running Main")
            try {
                Wrapper(args)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}