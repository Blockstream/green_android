package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import java.util.Observable;
import java.util.Observer;

public abstract class SubaccountFragment extends GAFragment {

    final private BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onSubaccountChanged(intent.getIntExtra("sub", 0));
                }
            });
        }
    };

    abstract protected void onSubaccountChanged(final int input);

    // Must be called by subclasses after onCreateView()
    protected void registerReceiver() {
        getActivity().registerReceiver(br, new IntentFilter("fragmentupdater"));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(br);
    }

    protected void hideKeyboard() {
        final InputMethodManager imm = (InputMethodManager)
                getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        final View v = getActivity().getCurrentFocus();
        if (v != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    protected Observer makeBalanceObserver() {
        return new Observer() {
            @Override
            public void update(final Observable observable, final Object o) {
                final Activity activity = getActivity();
                if (activity == null)
                    return;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onBalanceUpdated(activity);
                    }
                });
            }
        };
    }

    protected void onBalanceUpdated(final Activity activity) { }
}
