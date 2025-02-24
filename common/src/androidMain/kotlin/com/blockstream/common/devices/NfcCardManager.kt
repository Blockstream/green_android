package com.blockstream.common.devices

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.SystemClock
import android.util.Log
import kotlinx.io.IOException

/**
 * Manages connection of NFC-based cards. Extends Thread and must be started using the start() method. The thread has
 * a runloop which monitors the connection and from which CardListener callbacks are called.
 */
class NfcCardManager @JvmOverloads constructor(
    private val loopSleepMS: Long = DEFAULT_LOOP_SLEEP_MS
) : Thread(), NfcAdapter.ReaderCallback {

    private var isoDep: IsoDep? = null
    private var isRunning = false
    private var cardListener: CardListener? = null

    /**
     * True if connected, false otherwise.
     * @return if connected, false otherwise
     */
    val isConnected: Boolean
        get() = try {
            isoDep?.isConnected == true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }

    override fun onTagDiscovered(tag: Tag) {
        isoDep = IsoDep.get(tag)?.apply {
            try {
                connect()
                timeout = 120000
            } catch (e: IOException) {
                Log.e(TAG, "error connecting to tag")
            }
        }
    }

    /**
     * Runloop. Do NOT invoke directly. Use start() instead.
     */
    override fun run() {
        var connected = isConnected

        while (true) {
            val newConnected = isConnected
            if (newConnected != connected) {
                connected = newConnected
                Log.i(TAG, "tag ${if (connected) "connected" else "disconnected"}")

                if (connected && !isRunning) {
                    onCardConnected()
                } else {
                    onCardDisconnected()
                }
            }

            SystemClock.sleep(loopSleepMS)
        }
    }

    /**
     * Reacts on card connected by calling the callback of the registered listener.
     */
    private fun onCardConnected() {
        isRunning = true

        cardListener?.onConnected(NfcCardChannel(isoDep!!))

        isRunning = false
    }

    /**
     * Reacts on card disconnected by calling the callback of the registered listener.
     */
    private fun onCardDisconnected() {
        isRunning = false
        isoDep = null
        cardListener?.onDisconnected()
    }

    /**
     * Sets the card listener.
     *
     * @param listener the new listener
     */
    fun setCardListener(listener: CardListener) {
        cardListener = listener
    }

    companion object {
        private const val TAG = "NFCCardManager"
        private const val DEFAULT_LOOP_SLEEP_MS: Long = 50
    }
}