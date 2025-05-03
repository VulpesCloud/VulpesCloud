package de.vulpescloud.node

object CloudVersion {

    fun getFullVersion(): String {
        return this::class.java.`package`.implementationVersion
    }

    fun getVersion(): String {
        return getFullVersion().split("_")[0]
    }

    fun getBuildNumber(): Int {
        return try {
            getFullVersion().split("_")[2].toInt()
        } catch (e: Exception) {
            -1
        }
    }

    fun getGitBranch(): String {
        return try {
            getFullVersion().split("_")[1].split("@")[0]
        } catch (e: Exception) {
            "unknown"
        }
    }

    fun getGitCommit(): String {
        return try {
            getFullVersion().split("_")[1].split("@")[1]
        } catch (e: Exception) {
            "unknown"
        }
    }

}
