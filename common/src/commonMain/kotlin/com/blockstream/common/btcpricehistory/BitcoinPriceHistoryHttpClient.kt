package com.blockstream.common.btcpricehistory

import com.blockstream.green.data.config.AppInfo
import com.blockstream.green.network.AppHttpClient

class BitcoinPriceHistoryHttpClient(appInfo: AppInfo): AppHttpClient(enableLogging = appInfo.isDevelopmentOrDebug)