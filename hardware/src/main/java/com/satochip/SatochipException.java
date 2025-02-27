package com.satochip;

import com.btchip.comm.LedgerException;

public class SatochipException extends RuntimeException {


    public enum ExceptionReason {
        PIN_UNDEFINED, /** Returned if no pin is defined */
        INVALID_PARAMETER, /** Returned if a parameter passed to a function is invalid */
        IO_ERROR, /** Returned if the communication with the device fails */
        APPLICATION_ERROR, /** Returned if an unexpected message is received from the device */
        INTERNAL_ERROR, /** Returned if an unexpected protocol error occurs when communicating with the device */
        OTHER
    };

    private SatochipException.ExceptionReason reason;
    private int sw;

    public SatochipException(SatochipException.ExceptionReason reason) {
        this.reason = reason;
    }

    public SatochipException(SatochipException.ExceptionReason reason, String details) {
        super(details);
        this.reason = reason;
    }

    public SatochipException(String message) {
        super(message);
        this.reason = SatochipException.ExceptionReason.OTHER;
    }

    public SatochipException.ExceptionReason getReason() {
        return reason;
    }

    public String toString() {
        return reason.toString() + " " + super.toString();
    }


}
