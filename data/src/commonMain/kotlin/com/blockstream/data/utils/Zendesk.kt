package com.blockstream.data.utils

import com.blockstream.data.config.AppInfo
import com.blockstream.data.data.SupportData
import com.mohamedrejeb.ksoup.entities.KsoupEntities

fun createNewTicketUrl(
    appInfo: AppInfo,
    subject: String? = null,
    supportData: SupportData? = null,
    isJade: Boolean = false,
): String {
    val product = if (isJade) "blockstream_jade" else "green"
    val hw = if (isJade) "jade" else ""

    val policy: String = supportData?.zendeskSecurityPolicy ?: ""

    // Temp solution
    val platform = if (appInfo.userAgent.contains("ios", ignoreCase = true)) "ios" else "android"

    return "https://help.blockstream.com/hc/en-us/requests/new?tf_900008231623=$platform&tf_subject=${
        subject?.let {
            KsoupEntities.encodeHtml(
                it
            )
        } ?: ""
    }&tf_900003758323=${product}&tf_900006375926=${hw}&tf_900009625166=${appInfo.version}&tf_6167739898649=${policy}"
}