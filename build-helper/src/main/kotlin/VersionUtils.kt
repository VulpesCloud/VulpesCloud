
fun getGitBranch(): String {
    val process = Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "--abbrev-ref", "HEAD"))
    process.waitFor()
    val branch = process.inputStream.bufferedReader().readText().trim()
    return branch
}

fun getGitCommit(): String {
    val process = Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "--short", "HEAD"))
    process.waitFor()
    val commit = process.inputStream.bufferedReader().readText().trim()
    return commit
}