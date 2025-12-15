package com.blockstream.gms

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.blockstream.base.InstallReferrer
import com.blockstream.data.CountlyBase.Companion.GOOGLE_PLAY_ORGANIC_DEVELOPMENT
import com.blockstream.data.CountlyBase.Companion.GOOGLE_PLAY_ORGANIC_PRODUCTION
import com.blockstream.data.config.AppInfo
import com.blockstream.utils.Loggable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ly.count.android.sdk.ModuleAttribution
import java.net.URLDecoder

class InstallReferrerImpl(val context: Context, val appInfo: AppInfo) : InstallReferrer() {

    override fun handleReferrer(
        attribution: ModuleAttribution.Attribution,
        onComplete: (referrer: String) -> Unit
    ) {
        InstallReferrerClient.newBuilder(context).build().also { referrerClient ->
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            var cid: String? = null
                            var uid: String? = null
                            var referrer: String? = null

                            try {
                                // The string may be URL Encoded, so decode it just to be sure.
                                // eg. utm_source=google-play&utm_medium=organic
                                // eg. "cly_id=0eabe3eac38ff74556c69ed25a8275b19914ea9d&cly_uid=c27b33b16ac7947fae0ed9e60f3a5ceb96e0e545425dd431b791fe930fabafde4b96c69e0f63396202377a8025f008dfee2a9baf45fa30f7c80958bd5def6056"
                                referrer = URLDecoder.decode(
                                    referrerClient.installReferrer.installReferrer,
                                    "UTF-8"
                                )

                                logger.i { "Referrer: $referrer" }

                                val parts = referrer.split("&")

                                for (part in parts) {
                                    // Countly campaign
                                    if (part.startsWith("cly_id")) {
                                        cid = part.replace("cly_id=", "").trim()
                                    }
                                    if (part.startsWith("cly_uid")) {
                                        uid = part.replace("cly_uid=", "").trim()
                                    }

                                    // Google Play organic
                                    if (part.trim() == "utm_medium=organic") {
                                        cid =
                                            if (appInfo.isDevelopment) GOOGLE_PLAY_ORGANIC_DEVELOPMENT else GOOGLE_PLAY_ORGANIC_PRODUCTION
                                    }
                                }

                                attribution.recordDirectAttribution("countly", buildJsonObject {
                                    put("cid", cid)
                                    if (uid != null) {
                                        put("cuid", uid)
                                    }
                                }.toString())

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            onComplete.invoke(referrer ?: "")
                        }

                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                            // API not available on the current Play Store app.
                            // logger.info { "InstallReferrerService FEATURE_NOT_SUPPORTED" }
                            onComplete.invoke("")
                        }

                        InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                            // Connection couldn't be established.
                            // logger.info { "InstallReferrerService SERVICE_UNAVAILABLE" }
                        }
                    }

                    // Disconnect the client
                    referrerClient.endConnection()
                }

                override fun onInstallReferrerServiceDisconnected() {}
            })
        }
    }

    companion object : Loggable()
}