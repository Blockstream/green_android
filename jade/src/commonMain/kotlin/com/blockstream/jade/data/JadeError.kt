package com.blockstream.jade.data

class JadeError constructor(val code: Int, message: String, val data: Any? = null) : Exception(message) {
    companion object {
        // Local error codes
        const val UNSUPPORTED_FIRMWARE_VERSION: Int = -100
        const val JADE_RPC_MSG_TIMEOUT: Int = -200
        const val JADE_MSGS_OUT_OF_SYNC: Int = -300

        // RPC error codes
        const val CBOR_RPC_INVALID_REQUEST: Int = -32600
        const val CBOR_RPC_UNKNOWN_METHOD: Int = -32601
        const val CBOR_RPC_BAD_PARAMETERS: Int = -32602
        const val CBOR_RPC_INTERNAL_ERROR: Int = -32603

        const val CBOR_RPC_USER_CANCELLED: Int = -32000
        const val CBOR_RPC_PROTOCOL_ERROR: Int = -32001
        const val CBOR_RPC_HW_LOCKED: Int = -32002
        const val CBOR_RPC_NETWORK_MISMATCH: Int = -32003
    }
}
