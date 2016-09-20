package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.util.Observable;
import java.util.Observer;

import com.afollestad.materialdialogs.MaterialDialog;

public abstract class SubaccountFragment extends GAFragment {

    private BroadcastReceiver mBroadcastReceiver = null;
    private MaterialDialog mWaitDialog = null;
    private Observer mBalanceObserver = null;
    private int mBalanceObserverSubaccount = 0;

    // Must be called by subclasses at the end of onCreateView()
    protected void registerReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        onSubaccountChanged(intent.getIntExtra("sub", 0));
                    }
                });
            }
        };

        getActivity().registerReceiver(mBroadcastReceiver, new IntentFilter("fragmentupdater"));
    }

    // Subclasses must override this to process subaccount changes
    abstract protected void onSubaccountChanged(final int input);

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(mBroadcastReceiver);
        mBroadcastReceiver = null;
    }

    protected void hideKeyboard() {
        if (getActivity() == null)
            return;

        final View v = getActivity().getCurrentFocus();
        if (v == null)
            return;

        final InputMethodManager imm;
        imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    public void attachObservers() {}

    public void detachObservers() {
        hideWaitDialog();
    }

    protected Observer makeUiObserver(final Runnable r) {
        return new Observer() {
            @Override
            public void update(final Observable observable, final Object o) {
                final Activity activity = getActivity();
                if (activity != null)
                    activity.runOnUiThread(r);
            }
        };
    }

    protected void makeBalanceObserver(final int subAccount) {
        deleteBalanceObserver();
        mBalanceObserver = makeUiObserver(new Runnable() { public void run() { onBalanceUpdated(); } });
        mBalanceObserverSubaccount = subAccount;
        getGAService().addBalanceObserver(mBalanceObserverSubaccount, mBalanceObserver);
    }

    protected void deleteBalanceObserver() {
        if (mBalanceObserver == null)
            return;
        getGAService().deleteBalanceObserver(mBalanceObserverSubaccount, mBalanceObserver);
        mBalanceObserver = null;
        mBalanceObserverSubaccount = 0;
    }

    protected void onBalanceUpdated() { }

    protected void popupWaitDialog() {
        if (mWaitDialog == null && getActivity() != null) {
            mWaitDialog = UI.popupWait(getActivity(), R.string.loading);
            mWaitDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(final DialogInterface d) {
                    mWaitDialog = null;
                }
            });
        }
    }

    protected void hideWaitDialog() {
        if (mWaitDialog != null) {
            try {
                mWaitDialog.cancel();
            } catch (final IllegalArgumentException e) {
            }
            mWaitDialog = null;
        }
    }
}
