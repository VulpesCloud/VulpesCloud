package de.vulpescloud.node.modules

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
