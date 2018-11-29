package com.greenaddress.greenbits.ui.components;

import com.greenaddress.greenapi.model.ActiveAccountObservable;
import com.greenaddress.greenapi.model.BalanceDataObservable;
import com.greenaddress.greenapi.model.ReceiveAddressObservable;
import com.greenaddress.greenapi.model.TransactionDataObservable;

import java.util.Observer;

public interface OnGdkListener {
    void onUpdateTransactions(final TransactionDataObservable observable);

    void onUpdateActiveSubaccount(final ActiveAccountObservable observable);

    void onUpdateReceiveAddress(final ReceiveAddressObservable observable);

    void onUpdateBalance(final BalanceDataObservable observable);

    void onNewTx(final Observer observable);

    void onVerifiedTx(final Observer observable);
}
