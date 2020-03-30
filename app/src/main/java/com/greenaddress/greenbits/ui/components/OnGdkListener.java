package com.greenaddress.greenbits.ui.components;

import java.util.Observable;
import java.util.Observer;

public interface OnGdkListener {
    void onUpdateTransactions(final Observable observable);

    void onUpdateActiveSubaccount(final Observable observable);

    void onUpdateBalance(final Observable observable);

    void onNewTx(final Observer observable);

    void onVerifiedTx(final Observer observable);
}
