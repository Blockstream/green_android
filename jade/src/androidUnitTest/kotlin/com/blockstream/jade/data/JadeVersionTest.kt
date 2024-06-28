package com.blockstream.jade.data

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class JadeVersionTest {

    @Test
    fun test_ctor_parse() {
        // Normal version number
        val ver0 = JadeVersion("0.1.21")
        Assert.assertEquals(0, ver0.major.toLong())
        Assert.assertEquals(1, ver0.minor.toLong())
        Assert.assertEquals(21, ver0.patch.toLong())
        Assert.assertEquals("", ver0.pre_release)
        Assert.assertEquals("", ver0.trailer)

        // Version number, dirty
        val ver1 = JadeVersion("3.5.4-dirty")
        Assert.assertEquals(3, ver1.major.toLong())
        Assert.assertEquals(5, ver1.minor.toLong())
        Assert.assertEquals(4, ver1.patch.toLong())
        Assert.assertEquals("", ver1.pre_release)
        Assert.assertEquals("dirty", ver1.trailer)

        // Version number with pre-release label (new/semver/dash)
        val ver2 = JadeVersion("0.1.22-beta4")
        Assert.assertEquals(0, ver2.major.toLong())
        Assert.assertEquals(1, ver2.minor.toLong())
        Assert.assertEquals(22, ver2.patch.toLong())
        Assert.assertEquals("beta4", ver2.pre_release)
        Assert.assertEquals("", ver2.trailer)

        // Version number with pre-release label  (legacy/deprecated/dotted)
        val ver2a = JadeVersion("0.2.11.rc2")
        Assert.assertEquals(0, ver2a.major.toLong())
        Assert.assertEquals(2, ver2a.minor.toLong())
        Assert.assertEquals(11, ver2a.patch.toLong())
        Assert.assertEquals("rc2", ver2a.pre_release)
        Assert.assertEquals("", ver2a.trailer)

        // Version number with pre-release label new/semver/dash)
        val ver3 = JadeVersion("0.1.22-beta4-dirty")
        Assert.assertEquals(0, ver3.major.toLong())
        Assert.assertEquals(1, ver3.minor.toLong())
        Assert.assertEquals(22, ver3.patch.toLong())
        Assert.assertEquals("beta4", ver3.pre_release)
        Assert.assertEquals("dirty", ver3.trailer)

        // Version number with pre-release label (legacy/deprecated/dotted)
        val ver3a = JadeVersion("0.2.11.rc2-dirty")
        Assert.assertEquals(0, ver3a.major.toLong())
        Assert.assertEquals(2, ver3a.minor.toLong())
        Assert.assertEquals(11, ver3a.patch.toLong())
        Assert.assertEquals("rc2", ver3a.pre_release)
        Assert.assertEquals("dirty", ver3a.trailer)

        // Version number with additional commits
        val ver4 = JadeVersion("1.3.2-7-gabcdef")
        Assert.assertEquals(1, ver4.major.toLong())
        Assert.assertEquals(3, ver4.minor.toLong())
        Assert.assertEquals(2, ver4.patch.toLong())
        Assert.assertEquals("", ver4.pre_release)
        Assert.assertEquals("7-gabcdef", ver4.trailer)

        // Version number with pre-release label and additional commits (new/semver/dash)
        val ver5 = JadeVersion("1.5.23.rc1-4-gabcdef")
        Assert.assertEquals(1, ver5.major.toLong())
        Assert.assertEquals(5, ver5.minor.toLong())
        Assert.assertEquals(23, ver5.patch.toLong())
        Assert.assertEquals("rc1", ver5.pre_release)
        Assert.assertEquals("4-gabcdef", ver5.trailer)

        // Version number with pre-release label and additional commits (legacy/deprecated/dotted)
        val ver5a = JadeVersion("3.7.16.rc2-3-gabcdef")
        Assert.assertEquals(3, ver5a.major.toLong())
        Assert.assertEquals(7, ver5a.minor.toLong())
        Assert.assertEquals(16, ver5a.patch.toLong())
        Assert.assertEquals("rc2", ver5a.pre_release)
        Assert.assertEquals("3-gabcdef", ver5a.trailer)

        // Version number with pre-release label and additional commits (new/semver/dash)
        val ver6 = JadeVersion("1.5.23.rc1-4-gabcdef-dirty")
        Assert.assertEquals(1, ver6.major.toLong())
        Assert.assertEquals(5, ver6.minor.toLong())
        Assert.assertEquals(23, ver6.patch.toLong())
        Assert.assertEquals("rc1", ver6.pre_release)
        Assert.assertEquals("4-gabcdef-dirty", ver6.trailer)

        // Version number with pre-release label and additional commits (legacy/deprecated/dotted)
        val ver6a = JadeVersion("3.7.16.rc2-3-gabcdef-dirty")
        Assert.assertEquals(3, ver6a.major.toLong())
        Assert.assertEquals(7, ver6a.minor.toLong())
        Assert.assertEquals(16, ver6a.patch.toLong())
        Assert.assertEquals("rc2", ver6a.pre_release)
        Assert.assertEquals("3-gabcdef-dirty", ver6a.trailer)
    }

    @Test(expected = IllegalArgumentException::class)
    fun test_bad_tag_parse() {
        JadeVersion("1.5")
    }

    @Test(expected = NumberFormatException::class)
    fun test_bad_number_parse_1() {
        JadeVersion("x.1.2")
    }

    @Test(expected = NumberFormatException::class)
    fun test_bad_number_parse_2() {
        JadeVersion("1.x.2")
    }

    @Test(expected = NumberFormatException::class)
    fun test_bad_number_parse_3() {
        JadeVersion("1.2.x")
    }

    @Test
    fun check_string_representation() {
        val versions = arrayOf(
            "0.1.21",
            "0.1.21-dirty",
            "1.3.8-13-gabcdef",
            "1.3.8-13-gabcdef-dirty",
            "2.7.11-alpha2",
            "2.7.11-alpha2-dirty",
            "4.1.3-beta3-43-gabcdef",
            "3.17.34-rc2-143-gabcdef-dirty",
        )

        for (version in versions) {
            // Check converting to/from string gets the same version back
            val ver = JadeVersion(version)
            val str = ver.toString()

            // Generated string should be same as original string
            Assert.assertEquals(version, str)

            // Should be able to re-generate same version from string representation
            val same = JadeVersion(str)
            Assert.assertEquals(0, ver.compareTo(same).toLong())
            Assert.assertEquals(0, same.compareTo(ver).toLong())
            Assert.assertEquals(str, same.toString())
            Assert.assertEquals(ver, same)
        }
    }

    @Test
    fun check_legacy_string_representation() {
        // This format is what we used originally, but is now deprecated in favour
        // of the dash-separator, as this is more semver-like.
        // NOTE: all these have the 'offending' dot at ver[7] for simplicity
        val versions = arrayOf(
            "2.17.23.alpha2",
            "2.17.23.beta1-dirty",
            "11.1.34.beta12-43-gabcdef",
            "7.10.18.rc1-8-gabcdef-dirty"
        )

        for (version in versions) {
            // Check converting to/from string gets the same version back
            val ver = JadeVersion(version)
            val str = ver.toString()

            // Generated string should be same as original string, *BUT* the dot before
            // the pre-release label would have been changed to a dash (at pos 7)
            val expected = version.substring(0, 7) + '-' + version.substring(8)
            Assert.assertEquals(expected, str)

            // Should be able to re-generate same version from string representation
            val same = JadeVersion(str)
            Assert.assertEquals(0, ver.compareTo(same).toLong())
            Assert.assertEquals(0, same.compareTo(ver).toLong())
            Assert.assertEquals(str, same.toString())
            Assert.assertFalse(ver == same)
        }
    }

    // For all versions in the passed array, check the compareTo() method reflects the
    // array order and check the equals() method is consistent.
    private fun check_versions_array(versions: Array<JadeVersion>) {
        for (i in versions.indices) {
            // For all preceding versions, check they are 'less than' this
            for (j in 0 until i) {
                Assert.assertTrue(versions[j].compareTo(versions[i]) < 0)
                Assert.assertTrue(versions[i].compareTo(versions[j]) > 0)
                Assert.assertNotEquals(versions[i], versions[j])
            }

            // For the same version check equality
            Assert.assertEquals(0, versions[i].compareTo(versions[i]).toLong())
            Assert.assertEquals(versions[i], versions[i])

            // For all later versions, check they are 'greater than' this
            for (j in i + 1 until versions.size) {
                Assert.assertTrue("$i/$j", versions[j].compareTo(versions[i]) > 0)
                Assert.assertTrue(versions[i].compareTo(versions[j]) < 0)
                Assert.assertNotEquals(versions[i], versions[j])
            }
        }
    }

    @Test
    fun test_version_number_compare() {
        val versions = arrayOf(
            JadeVersion("0.1.5"),
            JadeVersion("0.1.9"),
            JadeVersion("0.1.9-dirty"),
            JadeVersion("0.1.10"),
            JadeVersion("0.1.11"),
            JadeVersion("0.9.1"),
            JadeVersion("0.10.0"),
            JadeVersion("1.0.0"),
            JadeVersion("1.0.0-dirty"),
            JadeVersion("1.0.1"),
            JadeVersion("1.1.0")
        )
        check_versions_array(versions)
    }

    @Test
    fun test_pre_release_labels_compare() {
        val versions = arrayOf(
            JadeVersion("0.1.20"),
            JadeVersion("0.1.20-dirty"),
            JadeVersion("0.1.21-alpha1"),
            JadeVersion("0.1.21-alpha1-dirty"),
            JadeVersion("0.1.21.alpha2"),
            JadeVersion("0.1.21.beta1"),
            JadeVersion("0.1.21-beta2"),
            JadeVersion("0.1.21.beta2-dirty"),
            JadeVersion("0.1.21.rc1"),
            JadeVersion("0.1.21-rc2"),
            JadeVersion("0.1.21.rc3"),
            JadeVersion("0.1.21-rc4"),
            JadeVersion("0.1.21")
        )
        check_versions_array(versions)
    }

    @Test
    fun test_commits_compare() {
        val versions = arrayOf(
            JadeVersion("0.1.21"),
            JadeVersion("0.1.21-1-gabcdef-dirty"),
            JadeVersion("0.1.21-3-gabcdef"),
            JadeVersion("0.1.21-3-gabcdef-dirty"),
            JadeVersion("0.1.21-7-gabcdef"),
            JadeVersion("0.1.22"),
            JadeVersion("0.1.22-1-gabcdef"),
            JadeVersion("0.1.22-dirty")
        )
        check_versions_array(versions)
    }

    @Test
    fun test_questionable_dev_compare() {
        // These cases are of questionable value and may not be ideal - this
        // test case is just here to demonstrate/confirm these comparisons.
        val versions = arrayOf(
            JadeVersion("0.2.12"),  // Same number number of commits - comparison works on commit-id string/hex,
            // which is pretty meaningless, but at least consistent/deterministic.

            JadeVersion("0.2.12-1-gabcdef"),
            JadeVersion("0.2.12-1-gdefabc"),
            JadeVersion("0.2.12-2-gabcdef"),
            JadeVersion("0.2.12-2-gdefabc"),
            JadeVersion("0.2.13-beta1-1-gabcdef"),
            JadeVersion("0.2.13-beta1-1-gdefabc"),
            JadeVersion("0.2.13-rc1-1-gabcdef-dirty"),
            JadeVersion("0.2.13-rc1-1-gdefabc"),  // '-dirty' is seen as 'greater-than' '-1-gabcdef' which is perhaps suboptimal,
            // but really also pretty meaningless.  Again we just settle for consistent ordering.

            JadeVersion("0.2.13"),
            JadeVersion("0.2.13-3-gbcdefa"),
            JadeVersion("0.2.13-dirty"),
            JadeVersion("0.2.14-rc1-3-gbcdefa"),
            JadeVersion("0.2.14-rc1-dirty"),
            JadeVersion("0.2.14"),
            JadeVersion("0.2.14-dirty")
        )
        check_versions_array(versions)
    }

    @Test
    fun test_all_devcycle_compare() {
        val versions = arrayOf(
            JadeVersion("0.1.21"),
            JadeVersion("0.1.21-1-gabcdef"),
            JadeVersion("0.1.21-13-gabcdef"),
            JadeVersion("0.1.21-dirty"),
            JadeVersion("0.1.22-alpha1"),
            JadeVersion("0.1.22-alpha1-1-gabcdef"),
            JadeVersion("0.1.22-alpha1-1-gabcdef-dirty"),
            JadeVersion("0.1.22-alpha1-dirty"),
            JadeVersion("0.1.22.alpha2"),
            JadeVersion("0.1.22-beta1"),
            JadeVersion("0.1.22-beta1-1-gabcdef"),
            JadeVersion("0.1.22-beta1-1-gdefabc"),
            JadeVersion("0.1.22-beta1-1-gdefabc-dirty"),
            JadeVersion("0.1.22-beta1-dirty"),
            JadeVersion("0.1.22.rc1"),
            JadeVersion("0.1.22-rc1-1-gabcdef"),
            JadeVersion("0.1.22.rc1-dirty"),
            JadeVersion("0.1.22")
        )
        check_versions_array(versions)
    }
}