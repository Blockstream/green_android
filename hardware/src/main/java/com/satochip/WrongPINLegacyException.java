package com.satochip;

/**
 * Exception thrown when checking PIN/PUK
 */
public class WrongPINLegacyException extends ApduException {
  
  /**
   * Construct an exception with the given number of retry attempts.
   */
  public WrongPINLegacyException() {
    super("Wrong PIN Legacy");
  }
}
