package com.satochip;

/**
 * Exception thrown when checking PIN/PUK
 */
public class BlockedPINException extends ApduException {
  
  /**
   * Construct an exception with the given number of retry attempts.
   */
  public BlockedPINException() {
    super("PIN blocked");
  }
}
