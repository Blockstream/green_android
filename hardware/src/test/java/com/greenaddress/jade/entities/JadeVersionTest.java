package com.greenaddress.jade.entities;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class JadeVersionTest {

    @Test
    public void test_ctor_parse() {
        // Normal version number
        final JadeVersion ver0 = new JadeVersion("0.1.21");
        assertEquals(0, ver0.major);
        assertEquals(1, ver0.minor);
        assertEquals(21, ver0.patch);
        assertEquals("", ver0.pre_release);
        assertEquals("", ver0.trailer);

        // Version number, dirty
        final JadeVersion ver1 = new JadeVersion("3.5.4-dirty");
        assertEquals(3, ver1.major);
        assertEquals(5, ver1.minor);
        assertEquals(4, ver1.patch);
        assertEquals("", ver1.pre_release);
        assertEquals("dirty", ver1.trailer);

        // Version number with pre-release label (new/semver/dash)
        final JadeVersion ver2 = new JadeVersion("0.1.22-beta4");
        assertEquals(0, ver2.major);
        assertEquals(1, ver2.minor);
        assertEquals(22, ver2.patch);
        assertEquals("beta4", ver2.pre_release);
        assertEquals("", ver2.trailer);

        // Version number with pre-release label  (legacy/deprecated/dotted)
        final JadeVersion ver2a = new JadeVersion("0.2.11.rc2");
        assertEquals(0, ver2a.major);
        assertEquals(2, ver2a.minor);
        assertEquals(11, ver2a.patch);
        assertEquals("rc2", ver2a.pre_release);
        assertEquals("", ver2a.trailer);

        // Version number with pre-release label new/semver/dash)
        final JadeVersion ver3 = new JadeVersion("0.1.22-beta4-dirty");
        assertEquals(0, ver3.major);
        assertEquals(1, ver3.minor);
        assertEquals(22, ver3.patch);
        assertEquals("beta4", ver3.pre_release);
        assertEquals("dirty", ver3.trailer);

        // Version number with pre-release label (legacy/deprecated/dotted)
        final JadeVersion ver3a = new JadeVersion("0.2.11.rc2-dirty");
        assertEquals(0, ver3a.major);
        assertEquals(2, ver3a.minor);
        assertEquals(11, ver3a.patch);
        assertEquals("rc2", ver3a.pre_release);
        assertEquals("dirty", ver3a.trailer);

        // Version number with additional commits
        final JadeVersion ver4 = new JadeVersion("1.3.2-7-gabcdef");
        assertEquals(1, ver4.major);
        assertEquals(3, ver4.minor);
        assertEquals(2, ver4.patch);
        assertEquals("", ver4.pre_release);
        assertEquals("7-gabcdef", ver4.trailer);

        // Version number with pre-release label and additional commits (new/semver/dash)
        final JadeVersion ver5 = new JadeVersion("1.5.23.rc1-4-gabcdef");
        assertEquals(1, ver5.major);
        assertEquals(5, ver5.minor);
        assertEquals(23, ver5.patch);
        assertEquals("rc1", ver5.pre_release);
        assertEquals("4-gabcdef", ver5.trailer);

        // Version number with pre-release label and additional commits (legacy/deprecated/dotted)
        final JadeVersion ver5a = new JadeVersion("3.7.16.rc2-3-gabcdef");
        assertEquals(3, ver5a.major);
        assertEquals(7, ver5a.minor);
        assertEquals(16, ver5a.patch);
        assertEquals("rc2", ver5a.pre_release);
        assertEquals("3-gabcdef", ver5a.trailer);

        // Version number with pre-release label and additional commits (new/semver/dash)
        final JadeVersion ver6 = new JadeVersion("1.5.23.rc1-4-gabcdef-dirty");
        assertEquals(1, ver6.major);
        assertEquals(5, ver6.minor);
        assertEquals(23, ver6.patch);
        assertEquals("rc1", ver6.pre_release);
        assertEquals("4-gabcdef-dirty", ver6.trailer);

        // Version number with pre-release label and additional commits (legacy/deprecated/dotted)
        final JadeVersion ver6a = new JadeVersion("3.7.16.rc2-3-gabcdef-dirty");
        assertEquals(3, ver6a.major);
        assertEquals(7, ver6a.minor);
        assertEquals(16, ver6a.patch);
        assertEquals("rc2", ver6a.pre_release);
        assertEquals("3-gabcdef-dirty", ver6a.trailer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_bad_tag_parse() {
        new JadeVersion("1.5");
    }

    @Test(expected = NumberFormatException.class)
    public void test_bad_number_parse_1() {
        new JadeVersion("x.1.2");
    }

    @Test(expected = NumberFormatException.class)
    public void test_bad_number_parse_2() {
        new JadeVersion("1.x.2");
    }

    @Test(expected = NumberFormatException.class)
    public void test_bad_number_parse_3() {
        new JadeVersion("1.2.x");
    }

    @Test
    public void check_string_representation() {
        final String[] versions = new String[]{
                "0.1.21",
                "0.1.21-dirty",
                "1.3.8-13-gabcdef",
                "1.3.8-13-gabcdef-dirty",
                "2.7.11-alpha2",
                "2.7.11-alpha2-dirty",
                "4.1.3-beta3-43-gabcdef",
                "3.17.34-rc2-143-gabcdef-dirty",
        };

        for (final String version : versions) {
            // Check converting to/from string gets the same version back
            final JadeVersion ver = new JadeVersion(version);
            final String str = ver.toString();

            // Generated string should be same as original string
            assertEquals(version, str);

            // Should be able to re-generate same version from string representation
            final JadeVersion same = new JadeVersion(str);
            assertEquals(0, ver.compareTo(same));
            assertEquals(0, same.compareTo(ver));
            assertEquals(str, same.toString());
            assertEquals(ver, same);
        }
    }

    @Test
    public void check_legacy_string_representation() {
        // This format is what we used originally, but is now deprecated in favour
        // of the dash-separator, as this is more semver-like.
        // NOTE: all these have the 'offending' dot at ver[7] for simplicity
        final String[] versions = new String[]{
                "2.17.23.alpha2",
                "2.17.23.beta1-dirty",
                "11.1.34.beta12-43-gabcdef",
                "7.10.18.rc1-8-gabcdef-dirty"
        };

        for (final String version : versions) {
            // Check converting to/from string gets the same version back
            final JadeVersion ver = new JadeVersion(version);
            final String str = ver.toString();

            // Generated string should be same as original string, *BUT* the dot before
            // the pre-release label would have been changed to a dash (at pos 7)
            final String expected = version.substring(0, 7) + '-' + version.substring(8);
            assertEquals(expected, str);

            // Should be able to re-generate same version from string representation
            final JadeVersion same = new JadeVersion(str);
            assertEquals(0, ver.compareTo(same));
            assertEquals(0, same.compareTo(ver));
            assertEquals(str, same.toString());
            assertEquals(ver, same);
        }
    }

    // For all versions in the passed array, check the compareTo() method reflects the
    // array order and check the equals() method is consistent.
    private void check_versions_array(final JadeVersion[] versions) {
        for ( int i = 0; i < versions.length; ++i) {
            // For all preceding versions, check they are 'less than' this
            for (int j = 0; j < i; ++j) {
                assertTrue(versions[j].compareTo(versions[i]) < 0);
                assertTrue(versions[i].compareTo(versions[j]) > 0);
                assertNotEquals(versions[i], versions[j]);
            }

            // For the same version check equality
            assertEquals(0, versions[i].compareTo(versions[i]));
            assertEquals(versions[i], versions[i]);

            // For all later versions, check they are 'greater than' this
            for (int j = i+1; j < versions.length; ++j) {
                assertTrue(""+i+"/"+j, versions[j].compareTo(versions[i]) > 0);
                assertTrue(versions[i].compareTo(versions[j]) < 0);
                assertNotEquals(versions[i], versions[j]);
            }
        }
    }

    @Test
    public void test_version_number_compare() {
        final JadeVersion[] versions = new JadeVersion[]{
                new JadeVersion("0.1.5"),
                new JadeVersion("0.1.9"),
                new JadeVersion("0.1.9-dirty"),
                new JadeVersion("0.1.10"),
                new JadeVersion("0.1.11"),
                new JadeVersion("0.9.1"),
                new JadeVersion("0.10.0"),
                new JadeVersion("1.0.0"),
                new JadeVersion("1.0.0-dirty"),
                new JadeVersion("1.0.1"),
                new JadeVersion("1.1.0")
        };
        check_versions_array(versions);
    }

    @Test
    public void test_pre_release_labels_compare() {
        final JadeVersion[] versions = new JadeVersion[]{
                new JadeVersion("0.1.20"),
                new JadeVersion("0.1.20-dirty"),
                new JadeVersion("0.1.21-alpha1"),
                new JadeVersion("0.1.21-alpha1-dirty"),
                new JadeVersion("0.1.21.alpha2"),
                new JadeVersion("0.1.21.beta1"),
                new JadeVersion("0.1.21-beta2"),
                new JadeVersion("0.1.21.beta2-dirty"),
                new JadeVersion("0.1.21.rc1"),
                new JadeVersion("0.1.21-rc2"),
                new JadeVersion("0.1.21.rc3"),
                new JadeVersion("0.1.21-rc4"),
                new JadeVersion("0.1.21")
        };
        check_versions_array(versions);
    }

    @Test
    public void test_commits_compare() {
        final JadeVersion[] versions = new JadeVersion[]{
                new JadeVersion("0.1.21"),
                new JadeVersion("0.1.21-1-gabcdef-dirty"),
                new JadeVersion("0.1.21-3-gabcdef"),
                new JadeVersion("0.1.21-3-gabcdef-dirty"),
                new JadeVersion("0.1.21-7-gabcdef"),
                new JadeVersion("0.1.22"),
                new JadeVersion("0.1.22-1-gabcdef"),
                new JadeVersion("0.1.22-dirty")
        };
        check_versions_array(versions);
    }

    @Test
    public void test_questionable_dev_compare() {
        // These cases are of questionable value and may not be ideal - this
        // test case is just here to demonstrate/confirm these comparisons.
        final JadeVersion[] versions = new JadeVersion[]{
                new JadeVersion("0.2.12"),

                // Same number number of commits - comparison works on commit-id string/hex,
                // which is pretty meaningless, but at least consistent/deterministic.
                new JadeVersion("0.2.12-1-gabcdef"),
                new JadeVersion("0.2.12-1-gdefabc"),
                new JadeVersion("0.2.12-2-gabcdef"),
                new JadeVersion("0.2.12-2-gdefabc"),
                new JadeVersion("0.2.13-beta1-1-gabcdef"),
                new JadeVersion("0.2.13-beta1-1-gdefabc"),
                new JadeVersion("0.2.13-rc1-1-gabcdef-dirty"),
                new JadeVersion("0.2.13-rc1-1-gdefabc"),

                // '-dirty' is seen as 'greater-than' '-1-gabcdef' which is perhaps suboptimal,
                // but really also pretty meaningless.  Again we just settle for consistent ordering.
                new JadeVersion("0.2.13"),
                new JadeVersion("0.2.13-3-gbcdefa"),
                new JadeVersion("0.2.13-dirty"),
                new JadeVersion("0.2.14-rc1-3-gbcdefa"),
                new JadeVersion("0.2.14-rc1-dirty"),
                new JadeVersion("0.2.14"),
                new JadeVersion("0.2.14-dirty")
        };
        check_versions_array(versions);
    }

    @Test
    public void test_all_devcycle_compare() {
        final JadeVersion[] versions = new JadeVersion[]{
                new JadeVersion("0.1.21"),
                new JadeVersion("0.1.21-1-gabcdef"),
                new JadeVersion("0.1.21-13-gabcdef"),
                new JadeVersion("0.1.21-dirty"),
                new JadeVersion("0.1.22-alpha1"),
                new JadeVersion("0.1.22-alpha1-1-gabcdef"),
                new JadeVersion("0.1.22-alpha1-1-gabcdef-dirty"),
                new JadeVersion("0.1.22-alpha1-dirty"),
                new JadeVersion("0.1.22.alpha2"),
                new JadeVersion("0.1.22-beta1"),
                new JadeVersion("0.1.22-beta1-1-gabcdef"),
                new JadeVersion("0.1.22-beta1-1-gdefabc"),
                new JadeVersion("0.1.22-beta1-1-gdefabc-dirty"),
                new JadeVersion("0.1.22-beta1-dirty"),
                new JadeVersion("0.1.22.rc1"),
                new JadeVersion("0.1.22-rc1-1-gabcdef"),
                new JadeVersion("0.1.22.rc1-dirty"),
                new JadeVersion("0.1.22")
        };
        check_versions_array(versions);
    }

}