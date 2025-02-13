package com.satochip;

/**
 * Listener for card connection events.
 */
public interface CardListener {
  /**
   * Executes when the card channel is connected.
   *
   * @param channel the connected card channel
   */
  void onConnected(CardChannel channel);

  /**
   * Executes when a previously connected card is disconnected.
   */
  void onDisconnected();
}
