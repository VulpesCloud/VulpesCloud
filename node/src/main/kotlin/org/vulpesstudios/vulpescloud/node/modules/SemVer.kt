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

package org.vulpesstudios.vulpescloud.node.modules

data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int =
        compareValuesBy(this, other, SemVer::major, SemVer::minor, SemVer::patch)

    override fun toString() = "$major.$minor.$patch"

    companion object {
        // Returns null for non-SemVer strings like "unspecified" or "1.0-SNAPSHOT"
        fun parseOrNull(raw: String): SemVer? {
            val clean = raw.removeSuffix("-SNAPSHOT")
            val parts = clean.split(".")
            if (parts.size != 3) return null
            return runCatching { SemVer(parts[0].toInt(), parts[1].toInt(), parts[2].toInt()) }
                .getOrNull()
        }
    }
}
