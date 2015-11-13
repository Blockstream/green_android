package com.greenaddress.greenapi;


public interface INotificationHandler {
    void onNewBlock(final int count);

    void onNewTransaction(final int wallet_id, final int[] subaccounts, final long value, final String txhash);

    void onConnectionClosed(final int code);
}