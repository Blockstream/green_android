package com.greenaddress.jade.entities;

import androidx.annotation.NonNull;

import java.util.Objects;

// A Jade version is three dotted (unsigned) integers, followed by an optional
// string label.  Number parts are numeric compared, label is lex-compared.
// NOTE: no-string-label is 'greater than' any label.  eg:
// 1.2.3 > 1.2.3.beta2 > 1.2.3.beta1 > 1.2.3.alpha2 > 1.2.3.alpha1 > 1.2.2[.<anything>]
//
// NOTE: we also need to support the fw testing case where there may be a number of commits and
// a commit-id - eg: 0.21.1-3-gabcdef, 0.21.1-3-gabcdef-dirty, or even 0.21.1.alpha1-3-gabcdef-dirty !
// In this case we collect the number of commits but discard anything following that.
//
public class JadeVersion implements Comparable<JadeVersion> {
    final int major;
    final int minor;
    final int patch;
    final String label;
    final int extra_commits;

    public JadeVersion(final String version) {
        final String[] parts = version.split("-", 3);

        final String[] tag = parts[0].split("[\\.]", 4);
        if (tag.length < 3) {
            throw new IllegalArgumentException("Can't parse Jade version tag: " + parts[0]);
        }

        // Break out the three main numeric values which should come first
        this.major = Integer.parseInt(tag[0]);
        this.minor = Integer.parseInt(tag[1]);
        this.patch = Integer.parseInt(tag[2]);

        // Get any trailing label (alpha1, beta2, rc1, etc.)
        this.label = tag.length == 4 ? tag[3] : "";

        // See if there were any trailing commits - if so grab those also (and discard commit-id etc)
        this.extra_commits = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
    }

    @NonNull
    @Override
    public String toString() {
        final String ver = major + "." + minor + "." + patch;
        final String trailing = label.length() > 0 ? "." + label : "";
        final String commits = extra_commits > 0 ? "-" + extra_commits : "";
        return ver + trailing + commits;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final JadeVersion that = (JadeVersion) o;
        return major == that.major &&
                minor == that.minor &&
                patch == that.patch &&
                extra_commits == that.extra_commits &&
                label.equals(that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, extra_commits, label);
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

        // Note: any-label is 'less than' no-label
        // (as a label represents alpha2, beta3, rc1, etc)
        // eg. 1.2.3.beta3 < 1.2.3 and 1.2.3.rc1 < 1.2.3 etc.
        if (this.label.length() > 0 && other.label.length() == 0) {
            return -1;
        }
        if (this.label.length() == 0 && other.label.length() > 0) {
            return 1;
        }

        // Both have a label (or both have no label) - lexi-compare label
        // eg. 1.2.3.alpha3 < 1.2.3.beta2 < 1.2.3.rc1 < 1.2.3.rc2 etc.
        final int lblcmp = this.label.compareTo(other.label);
        if (lblcmp != 0) {
            return lblcmp;
        }

        // Finally compare the number of additional commits
        // NOTE: we should only get to this when testing local dev jade fw builds.
        return this.extra_commits - other.extra_commits;
    }
}