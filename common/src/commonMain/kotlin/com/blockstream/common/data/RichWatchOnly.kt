package com.blockstream.common.data

import com.blockstream.common.crypto.GreenKeystore
import com.blockstream.common.extensions.createLoginCredentials
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.gdk.params.LoginCredentialsParams
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RichWatchOnly(
    @SerialName("network") val network: String,
    @SerialName("username") val username: String,
    @SerialName("password") val password: String,
    @SerialName("watch_only_data") val watchOnlyData: String? = null,
) : GreenJson<RichWatchOnly>() {
    override fun kSerializer() = serializer()

    fun toLoginCredentialsParams() : LoginCredentialsParams{
        return LoginCredentialsParams(
            username = username,
            password = password,
            watchOnlyData = watchOnlyData
        )
    }

    companion object {
        fun fromString(jsonString: String): List<RichWatchOnly> {
            return try {
                json.decodeFromString(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                listOf()
            }
        }
    }
}

fun String.toRichWatchOnly(): List<RichWatchOnly> = RichWatchOnly.fromString(this)

fun List<RichWatchOnly>.toJson() : String = GreenJson.json.encodeToString(this)

fun List<RichWatchOnly>.toLoginCredentials(
    session: GdkSession,
    greenWallet: GreenWallet,
    greenKeystore: GreenKeystore
) = createLoginCredentials(
    walletId = greenWallet.id,
    network = session.defaultNetwork.id,
    credentialType = CredentialType.RICH_WATCH_ONLY,
    encryptedData = greenKeystore.encryptData(toJson().encodeToByteArray())
)