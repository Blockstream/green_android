package com.greenaddress.greenapi;


public interface INotificationHandler {
    void onNewBlock(final long count);

    void onNewTransaction(final long wallet_id, final long[] subaccounts, final long value, final String txhash);

    void onConnectionClosed(final int code);
}