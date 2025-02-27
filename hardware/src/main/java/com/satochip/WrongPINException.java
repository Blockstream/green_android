package com.satochip;

/**
 * Exception thrown when checking PIN/PUK
 */
public class WrongPINException extends ApduException {
  private int retryAttempts;

  /**
   * Construct an exception with the given number of retry attempts.
   *
   * @param retryAttempts the number of retry attempts
   */
  public WrongPINException(int retryAttempts) {
    super("Wrong PIN");
    this.retryAttempts = retryAttempts;
  }

  /**
   * Returns the number of available retry attempts.
   *
   * @return the number of retry attempts
   */
  public int getRetryAttempts() {
    return retryAttempts;
  }
}
