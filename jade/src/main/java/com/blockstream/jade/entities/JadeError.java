package com.blockstream.jade.entities;

public class JadeError extends RuntimeException {
    final int code;
    final Object data;

    // Local error codes
    public static final int UNSUPPORTED_FIRMWARE_VERSION = -100;
    public static final int JADE_RPC_MSG_TIMEOUT = -200;
    public static final int JADE_MSGS_OUT_OF_SYNC = -300;

    // RPC error codes
    public static final int CBOR_RPC_INVALID_REQUEST = -32600;
    public static final int CBOR_RPC_UNKNOWN_METHOD = -32601;
    public static final int CBOR_RPC_BAD_PARAMETERS = -32602;
    public static final int CBOR_RPC_INTERNAL_ERROR = -32603;

    public static final int CBOR_RPC_USER_CANCELLED = -32000;
    public static final int CBOR_RPC_PROTOCOL_ERROR = -32001;
    public static final int CBOR_RPC_HW_LOCKED = -32002;
    public static final int CBOR_RPC_NETWORK_MISMATCH = -32003;

    public JadeError(final int code, final String message, final Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public Object getData() {
        return data;
    }
}
