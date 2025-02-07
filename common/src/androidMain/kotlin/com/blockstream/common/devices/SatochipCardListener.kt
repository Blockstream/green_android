package com.blockstream.common.devices

//import android.util.Log

//import org.satochip.client.SatochipCommandSet
//import org.satochip.io.CardChannel
//import org.satochip.io.CardListener
//import org.satochip.satodimeapp.data.NfcResultCode

private const val TAG = "SatochipCardListener"

// todo remove deprecated
object SatochipCardListenerForAction : CardListener {

    override fun onConnected(channel: CardChannel) {

        println("SATODEBUG onConnected: Card is connected")
        try {
            val cmdSet = SatochipCommandSet(channel)
            // start to interact with card
            //NfcCardService.initialize(cmdSet)

            val rapduSelect = cmdSet.cardSelect("satochip").checkOK()
            // cardStatus
            val rapduStatus = cmdSet.cardGetStatus()//To update status if it's not the first reading
            val cardStatus = cmdSet.getApplicationStatus() //applicationStatus ?: return
            println("SATODEBUG readCard cardStatus: $cardStatus")
            println("SATODEBUG readCard cardStatus: ${cardStatus.toString()}")

            // TODO: disconnect?
            println("onConnected: trigger disconnection!")
            onDisconnected()

            // disable scanning once finished
            //Thread.sleep(100) // delay to let resultCodeLive update (avoid race condition?)

        } catch (e: Exception) {
            println("onConnected: an exception has been thrown during card init.")
            //Log.e(TAG, Log.getStackTraceString(e))
            onDisconnected()
        }
    }

    override fun onDisconnected() {
        //NfcCardService.isConnected.postValue(false)
        println("onDisconnected: Card disconnected!")
    }
}