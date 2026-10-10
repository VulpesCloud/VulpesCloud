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

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

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

fun ProviderFactory.gitOutput(vararg args: String): Provider<String> =
    exec {
        commandLine("git", *args)
        isIgnoreExitValue = true
    }.standardOutput.asText.map { it.trim() }

@Suppress("UnstableApiUsage")
fun ProviderFactory.gitBranch(): Provider<String> {
    val envBranch = environmentVariable("GIT_BRANCH")
        .orElse(environmentVariable("BRANCH_NAME"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val gitBranch = gitOutput("rev-parse", "--abbrev-ref", "HEAD").flatMap { branch ->
        if (branch == "HEAD") {
            // detached HEAD fallback
            gitOutput("name-rev", "--name-only", "HEAD")
                .map { it.removePrefix("remotes/origin/") }
        } else {
            provider { branch }
        }
    }

    return envBranch
        .map { it.removePrefix("origin/") }
        .orElse(gitBranch)
        .map { it.replace("/", "_") }
}

fun ProviderFactory.gitCommit(): Provider<String> =
    gitOutput("rev-parse", "--short", "HEAD")