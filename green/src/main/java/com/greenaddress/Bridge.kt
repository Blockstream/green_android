package com.greenaddress

import android.content.Context
import android.widget.Toast
import com.blockstream.libgreenaddress.GASession

object Bridge {

    val usePrototype: Boolean = true

    fun v4Implementation(context: Context){
        Toast.makeText(context, "v4 Implementation", Toast.LENGTH_SHORT).show()
    }

    fun v3Implementation(context: Context){
        Toast.makeText(context, "v3 Implementation", Toast.LENGTH_SHORT).show()
    }

    fun bridgeSession(gaSession: GASession, network: String, watchOnlyUsername: String?) {
        // Implement if needed
    }
}