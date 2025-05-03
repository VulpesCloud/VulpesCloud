
fun getGitBranch(): String {
    val envBranch = System.getenv("GIT_BRANCH") ?: System.getenv("BRANCH_NAME")
    if (envBranch != null && envBranch.isNotBlank()) {
        return envBranch.removePrefix("origin/") // Jenkins often adds this
    }

    val process = Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "--abbrev-ref", "HEAD"))
    process.waitFor()
    val branch = process.inputStream.bufferedReader().readText().trim()
    return if (branch == "HEAD") {
        // fallback if detached
        val fallback = Runtime.getRuntime().exec(arrayOf("git", "name-rev", "--name-only", "HEAD"))
        fallback.waitFor()
        fallback.inputStream.bufferedReader().readText().trim().removePrefix("remotes/origin/").replace("/", "_")
    } else {
        branch.replace("/", "_")
    }
}

fun getGitCommit(): String {
    val process = Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "--short", "HEAD"))
    process.waitFor()
    val commit = process.inputStream.bufferedReader().readText().trim()
    return commit
}