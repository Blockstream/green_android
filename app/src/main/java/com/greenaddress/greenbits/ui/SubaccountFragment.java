package com.greenaddress.greenbits.ui;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.greenaddress.greenapi.model.ActiveAccountObservable;
import com.greenaddress.greenapi.model.BalanceDataObservable;
import com.greenaddress.greenapi.model.ReceiveAddressObservable;
import com.greenaddress.greenapi.model.TransactionDataObservable;
import com.greenaddress.greenbits.ui.components.OnGdkListener;

import java.util.Observable;
import java.util.Observer;

public abstract class SubaccountFragment extends GAFragment implements Observer, OnGdkListener {

    private static final String TAG = SubaccountFragment.class.getSimpleName();

    private boolean mIsDirty;
    protected View mView;

    protected BalanceDataObservable mBalanceDataObservable;
    protected ReceiveAddressObservable mReceiveAddressObservable;
    protected TransactionDataObservable mTransactionDataObservable;
    protected ActiveAccountObservable mActiveAccountObservable;

    protected boolean isPageSelected() {
        return false;
    }

    private boolean getZombieStatus(final boolean status) {
        if (status)
            Log.d(TAG, "Zombie re-awakening: " + getClass().getName());
        return status;
    }

    // Returns true if we are being restored without an activity or service
    protected boolean isZombieNoView() {
        return getZombieStatus(getActivity() == null || getGAService() == null);
    }

    // Returns true if we are being restored without an activity, service or view
    protected boolean isZombie() {
        return getZombieStatus(getActivity() == null || getGAService() == null || mView == null);
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause -> " + TAG);
        detachObservers();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume -> " + TAG);
        if (!isDisconnected()) {
            setupObservers();
            attachObservers();
            onUpdateActiveSubaccount(getGAService().getModel().getActiveAccountObservable());
        }
    }

    protected void hideKeyboard() {
        final GaActivity activity = getGaActivity();
        if (activity != null)
            activity.hideKeyboardFrom(null); // Current focus
    }

    public void setupObservers() {
        mActiveAccountObservable = getGAService().getModel().getActiveAccountObservable();
        final int subAccount = mActiveAccountObservable.getActiveAccount();
        Log.d(TAG, "subaccount is " + subAccount);
        mBalanceDataObservable = getGAService().getModel().getBalanceDataObservable(subAccount);
        mReceiveAddressObservable = getGAService().getModel().getReceiveAddressObservable(subAccount);
        mTransactionDataObservable = getGAService().getModel().getTransactionDataObservable(subAccount);
    }

    public void attachObservers() {
        if (mBalanceDataObservable == null)
            return;
        mBalanceDataObservable.addObserver(this);
        mReceiveAddressObservable.addObserver(this);
        mTransactionDataObservable.addObserver(this);
        mActiveAccountObservable.addObserver(this);
    }

    public void detachObservers() {
        if (mBalanceDataObservable == null)
            return;
        mBalanceDataObservable.deleteObserver(this);
        mReceiveAddressObservable.deleteObserver(this);
        mTransactionDataObservable.deleteObserver(this);
        mActiveAccountObservable.deleteObserver(this);
    }

    public void onShareClicked() {}

    protected boolean isDirty() {
        return mIsDirty;
    }

    protected void setIsDirty(final boolean isDirty) {
        mIsDirty = isDirty;
    }

    @Override
    public void update(final Observable observable, final Object o) {

        if (observable instanceof BalanceDataObservable) {
            onUpdateBalance((BalanceDataObservable) observable);
        } else if (observable instanceof ReceiveAddressObservable) {
            onUpdateReceiveAddress((ReceiveAddressObservable) observable);
        } else if (observable instanceof TransactionDataObservable) {
            onUpdateTransactions((TransactionDataObservable) observable);
            //toast(R.string.id_a_new_transaction_has_just);
        } else if (observable instanceof ActiveAccountObservable) {
            detachObservers();
            setupObservers();
            attachObservers();
            onUpdateActiveSubaccount((ActiveAccountObservable) observable);
        }
    }

    protected void toast(final int resource) {
        getGaActivity().runOnUiThread(() -> UI.toast(getGaActivity(), resource, Toast.LENGTH_LONG));
    }

}
