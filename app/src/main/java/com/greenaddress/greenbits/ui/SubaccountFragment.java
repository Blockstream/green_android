package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.util.Observable;
import java.util.Observer;

public abstract class SubaccountFragment extends GAFragment {

    private BroadcastReceiver mBroadcastReceiver = null;

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

        final InputMethodManager imm;
        imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        final View v = getActivity().getCurrentFocus();
        if (v != null)
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    public void attachObservers() {};
    public void detachObservers() {};

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

    protected Observer makeBalanceObserver() {
        return makeUiObserver(new Runnable() { public void run() { onBalanceUpdated(); } });
    }

    protected void onBalanceUpdated() { }
}
