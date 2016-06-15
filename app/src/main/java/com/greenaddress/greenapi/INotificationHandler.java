package com.greenaddress.greenapi;

import java.util.List;

public interface INotificationHandler {
    void onNewBlock(final int count);

    // Called when a transaction has been received that affects the given sub-accounts
    void onNewTransaction(final int[] affectedSubAccounts);

    void onConnectionClosed(final int code);
}
