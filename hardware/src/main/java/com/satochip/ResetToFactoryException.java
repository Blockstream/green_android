package com.satochip;

/**
 * Exception thrown when checking PIN/PUK
 */
public class ResetToFactoryException extends ApduException {
  
  /**
   * Construct an exception with the given number of retry attempts.
   */
  public ResetToFactoryException() {
    super("Card reset to factory");
  }
}
