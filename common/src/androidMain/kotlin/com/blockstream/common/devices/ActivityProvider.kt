package com.blockstream.common.devices

import android.app.Activity

interface ActivityProvider {
    fun getCurrentActivity(): Activity?
}