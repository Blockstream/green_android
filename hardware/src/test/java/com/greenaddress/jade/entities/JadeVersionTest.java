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
        final JadeVersion ver1 = new JadeVersion("0.1.21");
        assertEquals(0, ver1.major);
        assertEquals(1, ver1.minor);
        assertEquals(21, ver1.patch);
        assertEquals("", ver1.label);
        assertEquals(0, ver1.extra_commits);

        // Version number with label
        final JadeVersion ver2 = new JadeVersion("0.1.22.beta4");
        assertEquals(0, ver2.major);
        assertEquals(1, ver2.minor);
        assertEquals(22, ver2.patch);
        assertEquals("beta4", ver2.label);
        assertEquals(0, ver2.extra_commits);

        // Version number with additional commits
        final JadeVersion ver3 = new JadeVersion("1.3.2-7-gabcdef");
        assertEquals(1, ver3.major);
        assertEquals(3, ver3.minor);
        assertEquals(2, ver3.patch);
        assertEquals("", ver3.label);
        assertEquals(7, ver3.extra_commits);

        // Version number with label and additional commits
        final JadeVersion ver4 = new JadeVersion("1.5.23.rc1-4-gabcdef-dirty");
        assertEquals(1, ver4.major);
        assertEquals(5, ver4.minor);
        assertEquals(23, ver4.patch);
        assertEquals("rc1", ver4.label);
        assertEquals(4, ver4.extra_commits);
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

    @Test(expected = NumberFormatException.class)
    public void test_bad_commits_parse() {
        new JadeVersion("1.2.3-x-gabcdef");
    }

    @Test
    public void check_string_representation() {
        final String[] versions = new String[]{
                "0.1.21",
                "1.3.8-13-gabcdef",
                "2.7.11.alpha2",
                "3.17.34.beta12-43-gabcdef",
        };

        for (final String version : versions) {
            // Check converting to/from string gets the same version back
            final JadeVersion ver = new JadeVersion(version);
            final String str = ver.toString();

            // Generated string should be prefix part of original string
            assertEquals(version.substring(0,str.length()), str);

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
                assertTrue(versions[j].compareTo(versions[i]) > 0);
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
                new JadeVersion("0.1.10"),
                new JadeVersion("0.1.11"),
                new JadeVersion("0.9.1"),
                new JadeVersion("0.10.0"),
                new JadeVersion("1.0.0"),
                new JadeVersion("1.0.1"),
                new JadeVersion("1.1.0")
        };
        check_versions_array(versions);
    }

    @Test
    public void test_labels_compare() {
        final JadeVersion[] versions = new JadeVersion[]{
                new JadeVersion("0.1.20"),
                new JadeVersion("0.1.21.alpha1"),
                new JadeVersion("0.1.21.alpha2"),
                new JadeVersion("0.1.21.beta1"),
                new JadeVersion("0.1.21.beta2"),
                new JadeVersion("0.1.21.rc1"),
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
                new JadeVersion("0.1.21-7-gabcdef-dirty"),
                new JadeVersion("0.1.22"),
                new JadeVersion("0.1.22-1-gabcdef")
        };
        check_versions_array(versions);
    }

    @Test
    public void test_all_devcycle_compare() {
        final JadeVersion[] versions = new JadeVersion[]{
                new JadeVersion("0.1.21"),
                new JadeVersion("0.1.21-1-gabcdef"),
                new JadeVersion("0.1.21-13-gabcdef"),
                new JadeVersion("0.1.22.alpha1"),
                new JadeVersion("0.1.22.alpha1-1-gabcdef-dirty"),
                new JadeVersion("0.1.22.alpha2"),
                new JadeVersion("0.1.22.beta1"),
                new JadeVersion("0.1.22.beta1-1-gabcdef-dirty"),
                new JadeVersion("0.1.22.rc1"),
                new JadeVersion("0.1.22.rc1-1-gabcdef"),
                new JadeVersion("0.1.22")
        };
        check_versions_array(versions);
    }
}