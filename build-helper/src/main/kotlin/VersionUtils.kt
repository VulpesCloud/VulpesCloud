/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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