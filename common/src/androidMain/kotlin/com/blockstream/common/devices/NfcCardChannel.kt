package com.blockstream.common.devices

import android.nfc.tech.IsoDep
import android.util.Log

/**
 * Implementation of the CardChannel interface using the Android NFC API.
 */
class NfcCardChannel(private val isoDep: IsoDep) : CardChannel {
    companion object {
        private const val TAG = "CardChannel"
    }

    override fun send(cmd: ApduCommand): ApduResponse {
        val apdu = cmd.serialize()
        Log.d(TAG, "COMMAND CLA: %02X INS: %02X P1: %02X P2: %02X LC: %02X".format(
            cmd.cla, cmd.ins, cmd.p1, cmd.p2, cmd.data.size
        ))

        val resp = isoDep.transceive(apdu)
        val response = ApduResponse(resp)
        Log.d(TAG, "RESPONSE LEN: %02X, SW: %04X %n-----------------------".format(
            response.getData().size, response.getSw()
        ))

        return response
    }

    override fun isConnected(): Boolean {
        return isoDep.isConnected
    }
}