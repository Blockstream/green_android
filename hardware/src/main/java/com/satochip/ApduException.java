package com.satochip;

/**
 * Exception thrown when the response APDU from the card contains unexpected SW or data.
 */
public class ApduException extends Exception {
  public final int sw;

  /**
   * Creates an exception with SW and message.
   *
   * @param sw the status word
   * @param message a descriptive message of the error
   */
  public ApduException(int sw, String message) {
    super(message + ", 0x" + String.format("%04X", sw));
    this.sw = sw;
  }

  /**
   * Creates an exception with a message.
   *
   * @param message a descriptive message of the error
   */
  public ApduException(String message) {
    super(message);
    this.sw = 0;
  }
}
