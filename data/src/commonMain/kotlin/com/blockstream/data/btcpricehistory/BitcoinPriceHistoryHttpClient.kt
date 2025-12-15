package com.blockstream.data.btcpricehistory

import com.blockstream.data.config.AppInfo
import com.blockstream.network.AppHttpClient

class BitcoinPriceHistoryHttpClient(appInfo: AppInfo) : AppHttpClient(enableLogging = appInfo.isDevelopmentOrDebug)