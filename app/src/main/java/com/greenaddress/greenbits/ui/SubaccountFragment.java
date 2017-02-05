package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.util.Observable;
import java.util.Observer;

import com.afollestad.materialdialogs.MaterialDialog;

public abstract class SubaccountFragment extends GAFragment {

    private static final String TAG = SubaccountFragment.class.getSimpleName();

    private BroadcastReceiver mBroadcastReceiver;
    private MaterialDialog mWaitDialog;
    private Observer mBalanceObserver;
    private int mBalanceObserverSubaccount;
    private boolean mIsSelected;
    private boolean mBlockWaitDialog;
    private boolean mIsDirty;
    protected View mView;
    private final Runnable mDialogCB = new Runnable() { public void run() { mWaitDialog = null; } };

    protected boolean IsPageSelected() {
        return mIsSelected;
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
        Log.d(TAG, "onDestroyView -> " + getClass().getSimpleName());
        if (mBroadcastReceiver != null) {
           getActivity().unregisterReceiver(mBroadcastReceiver);
           mBroadcastReceiver = null;
        }
        hideWaitDialog();
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

    public void setPageSelected(final boolean isSelected) {
        Log.d(TAG, "setPageSelected " + isSelected + " -> " + getClass().getSimpleName());
        mIsSelected = isSelected;
        if (!isSelected)
            hideWaitDialog();
    }

    protected void popupWaitDialog(final int message) {
        if (mIsSelected && mWaitDialog == null && getActivity() != null && !mBlockWaitDialog) {
            mWaitDialog = UI.popupWait(getActivity(), message);
            UI.setDialogCloseHandler(mWaitDialog, mDialogCB);
        }
    }

    public void hideWaitDialog() {
        if (mWaitDialog != null) {
            try {
                mWaitDialog.cancel();
            } catch (final IllegalArgumentException e) {
            }
            mWaitDialog = null;
        }
    }

    protected void setBlockWaitDialog(final boolean doBlock) {
        mBlockWaitDialog = doBlock;
        if (mBlockWaitDialog)
            hideWaitDialog();
    }

    protected boolean isDirty() {
        return mIsDirty;
    }

    protected void setIsDirty(final boolean isDirty) {
        mIsDirty = isDirty;
    }
}
