package com.greenaddress.jade.entities;

import androidx.annotation.NonNull;

import java.util.Objects;

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
public class JadeVersion implements Comparable<JadeVersion> {
    final int major;
    final int minor;
    final int patch;
    final String pre_release;
    final String trailer;

    public JadeVersion(final String version) {
        final String[] parts = version.split("-", 2);

        final String[] tag = parts[0].split("[\\.]", 4);
        if (tag.length < 3) {
            throw new IllegalArgumentException("Can't parse Jade version tag: " + parts[0]);
        }

        // Break out the three main numeric values which should come first
        this.major = Integer.parseInt(tag[0]);
        this.minor = Integer.parseInt(tag[1]);
        this.patch = Integer.parseInt(tag[2]);

        // Get any trailing pre-release label (alpha1, beta2, rc1, etc.)
        if (tag.length == 4) {
            // Legacy/deprecated format with pre-release label after a dot - eg. 1.2.3.beta1[-2-abcdef[-dirty]])
            this.pre_release = tag[3];
            this.trailer = parts.length > 1 ? parts[1] : "";
        }
        else if (parts.length > 1) {
            // New format (more semver-like) with '-' separator after a three-part dotted label - eg. 1.2.3[-beta1][-2-abcdef][-dirty]
            if (parts[1].matches("([0-9]+-.+)|(dirty$)")) {
                // eg. 1.2.3-2-gabcdef[-dirty] or 1.2.3-dirty
                this.pre_release = "";
                this.trailer = parts[1];
            } else {
                // eg. 1.2.3-beta1[-2-abcdef][-dirty]
                final String[] parts2 = parts[1].split("-", 2);
                this.pre_release = parts2[0];
                this.trailer = parts2.length > 1 ? parts2[1] : "";
            }
        } else {
            // No trailing info after the three-part dotted label - eg. 1.2.3
            this.pre_release = "";
            this.trailer = "";
        }
    }

    @NonNull
    @Override
    public String toString() {
        final String ver = this.major + "." + this.minor + "." + this.patch;
        final String prerelease_label = this.pre_release.length() > 0 ? "-" + this.pre_release : "";
        final String dev_trailer = this.trailer.length() > 0 ? "-" + this.trailer : "";
        return ver + prerelease_label + dev_trailer;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final JadeVersion that = (JadeVersion) o;
        return this.major == that.major &&
                this.minor == that.minor &&
                this.patch == that.patch &&
                this.pre_release.equals(that.pre_release) &&
                this.trailer.equals(that.trailer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.major, this.minor, this.patch, this.pre_release, this.trailer);
    }

    @Override
    public int compareTo(final JadeVersion other) {
        // Simple version check
        // eg. 1.2.3 < 1.2.4 < 1.3.1 < 2.0.0 etc.
        if (this.major != other.major) {
            return this.major - other.major;
        }
        if (this.minor != other.minor) {
            return this.minor - other.minor;
        }
        if (this.patch != other.patch) {
            return this.patch - other.patch;
        }

        // Versions same - check label

        // Note: any pre-release label is 'less than' no pre_release label
        // (as a pre-release label represents alpha2, beta3, rc1, etc)
        // eg. 1.2.3-beta3 < 1.2.3 and 1.2.3-rc1 < 1.2.3 etc.
        if (this.pre_release.length() > 0 && other.pre_release.length() == 0) {
            return -1;
        }
        if (this.pre_release.length() == 0 && other.pre_release.length() > 0) {
            return 1;
        }

        // Both have a pre-release label (or neither do) - lexi-compare pre-release label
        // eg. 1.2.3-alpha3 < 1.2.3-beta2 < 1.2.3-rc1 < 1.2.3-rc2 etc.
        final int lblcmp = this.pre_release.compareTo(other.pre_release);
        if (lblcmp != 0) {
            return lblcmp;
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
        return this.trailer.compareTo(other.trailer);
    }
}