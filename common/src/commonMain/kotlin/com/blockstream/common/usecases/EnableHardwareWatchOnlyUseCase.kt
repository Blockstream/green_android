package com.blockstream.common.usecases

import com.blockstream.common.crypto.GreenKeystore
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.MultipleWatchOnlyCredentials
import com.blockstream.common.data.WatchOnlyCredentials
import com.blockstream.common.database.Database
import com.blockstream.common.extensions.createLoginCredentials
import com.blockstream.common.gdk.GdkSession
import com.blockstream.green.utils.Loggable

class EnableHardwareWatchOnlyUseCase(
    private val database: Database,
    private val greenKeystore: GreenKeystore
) : Loggable() {

    suspend operator fun invoke(
        greenWallet: GreenWallet,
        session: GdkSession,
    ) {

        if (!session.isHardwareWallet) {
            throw Exception("Not a hardware wallet session")
        }

        if (session.isWatchOnlyValue) {
            throw Exception("Watch only session is not supported")
        }

        // Only if all accounts are singlesig you can enable
        if (session.allAccounts.value.all { it.isSinglesig }) {

            // Wait for setup to gets completed so that the active account is set
            session.setupDefaultAccounts().join()

            val multipleWatchOnlyCredentials =
                session.accounts.value.filter { it.isSinglesig && !it.hidden }.groupBy { it.network }.mapValues {
                    it.value.map {
                        session.getAccount(it).coreDescriptors ?: emptyList()
                    }.flatten()
                }.map {
                    it.key.id to WatchOnlyCredentials(coreDescriptors = it.value)
                }.toMap().let {
                    MultipleWatchOnlyCredentials(credentials = it)
                }

            if (multipleWatchOnlyCredentials.credentials.isEmpty()) {
                logger.d { "Empty hwWatchOnlyCredentials" }
                return
            }

            val encryptedData = greenKeystore.encryptData(
                multipleWatchOnlyCredentials.toJson().encodeToByteArray()
            )

            logger.d { "Creating HW Watch-only credentials" }

            val loginCredentials = createLoginCredentials(
                walletId = greenWallet.id,
                network = session.defaultNetwork.id,
                credentialType = CredentialType.KEYSTORE_HW_WATCHONLY_CREDENTIALS,
                encryptedData = encryptedData
            )

            database.replaceLoginCredential(loginCredentials)
        }
    }
}