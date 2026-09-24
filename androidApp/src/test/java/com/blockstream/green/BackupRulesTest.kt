/*
 * Guards the Android Auto Backup configuration under androidApp/src/main/res/xml*.
 *
 * The wallet database (green.sqlite) holds PIN-server credentials in plaintext, so it may only
 * reach Google's cloud backup when the backup is client-side encrypted. Android reads a different
 * config per API level (full_backup_content.xml on API 30 and below, data_extraction_rules.xml on
 * API 31+), and nothing else keeps those files in agreement, so these tests do.
 */
package com.blockstream.green

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class BackupRulesTest {

    private val resDir = File("src/main/res")
    private val walletDatabase = "green.sqlite"

    /** Read on API 24-27, where Android never client-side encrypts cloud backups. */
    private val legacyConfig = File(resDir, "xml/full_backup_content.xml")

    /** Read on API 28-30, where requireFlags can demand client-side encryption. */
    private val legacyConfigApi28 = File(resDir, "xml-v28/full_backup_content.xml")

    /** Read on API 31+. */
    private val dataExtractionRules = File(resDir, "xml/data_extraction_rules.xml")

    @Test
    fun legacyConfigForApi24to27ExcludesWalletDatabase() {
        val includes = includes(legacyConfig)

        // An empty include list means "back up everything", so the config must keep at least one.
        assertTrue("legacy config must list explicit includes", includes.isNotEmpty())
        assertFalse(
            "$walletDatabase must not be backed up where client-side encryption is unavailable",
            includes.any { it.getAttribute("path") == walletDatabase }
        )
    }

    @Test
    fun legacyConfigForApi28to30RequiresClientSideEncryption() {
        val includes = includes(legacyConfigApi28)

        assertTrue("API 28 config must list explicit includes", includes.isNotEmpty())
        includes.forEach {
            assertEquals(
                "include of ${it.getAttribute("path")} must require client-side encryption",
                "clientSideEncryption",
                it.getAttribute("requireFlags")
            )
        }
    }

    @Test
    fun cloudBackupDisabledWithoutEncryptionCapabilities() {
        val cloudBackup = elements(dataExtractionRules, "cloud-backup").single()

        assertEquals("true", cloudBackup.getAttribute("disableIfNoEncryptionCapabilities"))
    }

    @Test
    fun cloudBackupPathsAgreeAcrossApiLevels() {
        val cloudBackup = elements(dataExtractionRules, "cloud-backup").single()
        val api31Paths = elements(cloudBackup, "include").map { it.getAttribute("path") }.toSet()
        val api28Paths = includes(legacyConfigApi28).map { it.getAttribute("path") }.toSet()

        assertEquals(api31Paths, api28Paths)
    }

    private fun includes(file: File): List<Element> = elements(file, "include")

    private fun elements(file: File, tag: String): List<Element> {
        assertTrue("${file.path} must exist", file.isFile)
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        return elements(document.documentElement, tag)
    }

    private fun elements(parent: Element, tag: String): List<Element> {
        val nodes = parent.getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }
}
