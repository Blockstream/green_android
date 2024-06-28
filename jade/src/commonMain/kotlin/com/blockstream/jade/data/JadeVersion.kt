package com.blockstream.jade.data;


// A Jade version is three dotted (unsigned) integers, followed by an optional
// string pre-release label.  Number parts are numeric compared, pre-release label is lex-compared.
// The pre-release label may be separated by a dot (legacy/deprecated) or a dash (new, more semver-like).
// NOTE: no prerelease label is 'greater than' any prerelease label.  eg:
// 1.2.3 > 1.2.3-beta2 > 1.2.3-beta1 > 1.2.3-alpha2 > 1.2.3-alpha1 > 1.2.2[.<anything>]
//
// NOTE: we also need to support the fw testing case where there may be a number of commits and
// a commit-id - eg: 0.21.1-dirty, 0.21.1-3-gabcdef, 0.21.1-3-gabcdef-dirty, 0.21.1-alpha1-dirty, 0.21.1-alpha1-3-gabcdef-dirty ...
// In this case we collect any trailing information as a final string to compare on a best-efforts basis.
//
data class JadeVersion(val version: String) : Comparable<JadeVersion> {
    val major: Int
    val minor: Int
    val patch: Int
    var pre_release: String
    var trailer: String

    init {
        val parts = version.split("-".toRegex(), limit = 2).toTypedArray()

        val tag = parts[0].split("[\\.]".toRegex(), limit = 4).toTypedArray()
        if (tag.size < 3) {
            throw IllegalArgumentException("Can't parse Jade version tag: " + parts[0])
        }

        // Break out the three main numeric values which should come first
        major = tag[0].toInt()
        minor = tag[1].toInt()
        patch = tag[2].toInt()

        // Get any trailing pre-release label (alpha1, beta2, rc1, etc.)
        if (tag.size == 4) {
            // Legacy/deprecated format with pre-release label after a dot - eg. 1.2.3.beta1[-2-abcdef[-dirty]])
            pre_release = tag[3]
            trailer = if (parts.size > 1) parts[1] else ""
        } else if (parts.size > 1) {
            // New format (more semver-like) with '-' separator after a three-part dotted label - eg. 1.2.3[-beta1][-2-abcdef][-dirty]
            if (parts[1].matches("([0-9]+-.+)|(dirty$)".toRegex())) {
                // eg. 1.2.3-2-gabcdef[-dirty] or 1.2.3-dirty
                pre_release = ""
                trailer = parts[1]
            } else {
                // eg. 1.2.3-beta1[-2-abcdef][-dirty]
                val parts2 = parts[1].split("-".toRegex(), limit = 2).toTypedArray()
                pre_release = parts2[0]
                trailer = if (parts2.size > 1) parts2[1] else ""
            }
        } else {
            // No trailing info after the three-part dotted label - eg. 1.2.3
            pre_release = ""
            trailer = ""
        }
    }

    override fun toString(): String {
        val ver = major.toString() + "." + this.minor + "." + this.patch
        val prereleaseLabel = if (pre_release.isNotEmpty()) "-" + this.pre_release else ""
        val devTrailer = if (trailer.isNotEmpty()) "-" + this.trailer else ""
        return ver + prereleaseLabel + devTrailer
    }

    override fun compareTo(other: JadeVersion): Int {
        // Simple version check
        // eg. 1.2.3 < 1.2.4 < 1.3.1 < 2.0.0 etc.
        if (this.major != other.major) {
            return this.major - other.major
        }
        if (this.minor != other.minor) {
            return this.minor - other.minor
        }
        if (this.patch != other.patch) {
            return this.patch - other.patch
        }

        // Versions same - check label

        // Note: any pre-release label is 'less than' no pre_release label
        // (as a pre-release label represents alpha2, beta3, rc1, etc)
        // eg. 1.2.3-beta3 < 1.2.3 and 1.2.3-rc1 < 1.2.3 etc.
        if (pre_release.isNotEmpty() && other.pre_release.isEmpty()) {
            return -1
        }
        if (pre_release.isEmpty() && other.pre_release.isNotEmpty()) {
            return 1
        }

        // Both have a pre-release label (or neither do) - lexi-compare pre-release label
        // eg. 1.2.3-alpha3 < 1.2.3-beta2 < 1.2.3-rc1 < 1.2.3-rc2 etc.
        val lblcmp = pre_release.compareTo(other.pre_release)
        if (lblcmp != 0) {
            return lblcmp
        }

        // Finally compare the 'dev trailer' - this isn't very meaningful, but is a best-efforts
        // to provide some sort of deterministic ordering to dev builds.
        // It means that:
        // 'X-dirty' > 'X'
        // 'X-2-gabcdef' > X-1-gabcdef' > 'X'
        //
        // Questionable:
        // It ranks 'X-dirty' as > 'X-3-gabcdef'
        // It's arbitrary how 'X-2-<commit-id-1>' ranks against 'X-2-<commit-id-2>'
        // But these are pretty vague and meaningless comparisons anyway.
        // NOTE: we should only get to this when testing local dev jade fw builds.
        return trailer.compareTo(other.trailer)
    }

    fun isGreaterThan(that: JadeVersion): Boolean {
        return this > that
    }

    fun isLessThan(that: JadeVersion): Boolean {
        return this < that
    }
}