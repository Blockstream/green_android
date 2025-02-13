package com.blockstream.common.usecases

import com.blockstream.common.crypto.GreenKeystore
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.HwWatchOnlyCredentials
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

        val accounts = session.allAccounts.value

        if (accounts.all { it.isSinglesig }) {
            val hwWatchOnlyCredentials =
                accounts.filter { it.isSinglesig }.groupBy { it.network }.mapValues {
                    it.value.map {
                        session.getAccount(it).coreDescriptors ?: emptyList()
                    }.flatten()
                }.map {
                    it.key.id to WatchOnlyCredentials(coreDescriptors = it.value)
                }.toMap().let {
                    HwWatchOnlyCredentials(credentials = it)
                }

            if (hwWatchOnlyCredentials.credentials.isEmpty()) {
                logger.d { "Empty hwWatchOnlyCredentials" }
                return
            }

            val encryptedData = greenKeystore.encryptData(
                hwWatchOnlyCredentials.toJson().encodeToByteArray()
            )

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