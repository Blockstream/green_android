package com.btchip.comm;

/**
 * \brief Exception returned when communicating with a Ledger device
 */
public class LedgerException extends RuntimeException {

	public enum ExceptionReason {
		INVALID_PARAMETER, /** Returned if a parameter passed to a function is invalid */
		IO_ERROR, /** Returned if the communication with the device fails */
		APPLICATION_ERROR, /** Returned if an unexpected message is received from the device */
		INTERNAL_ERROR /** Returned if an unexpected protocol error occurs when communicating with the device */
	};

	private ExceptionReason reason;

	public LedgerException(ExceptionReason reason) {
		this.reason = reason;
	}

	public LedgerException(ExceptionReason reason, String details) {
		super(details);
		this.reason = reason;
	}

	public LedgerException(ExceptionReason reason, Throwable cause) {
		super(cause);
		this.reason = reason;
	}

	public ExceptionReason getReason() {
		return reason;
	}

	public String toString() {
		return reason.toString() + " " + super.toString();
	}

}
