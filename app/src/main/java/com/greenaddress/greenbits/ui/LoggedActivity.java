package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.model.ToastObservable;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.util.Observable;
import java.util.Observer;

public abstract class LoggedActivity extends GaActivity implements Observer {

    private boolean mChangingActivity = false;

    @Override
    protected void onResumeWithService() {
        super.onResumeWithService();
        if (mService == null || mService.getModel() == null) {
            toFirst();
            return;
        }
        if (mService.isDisconnected()) {
            toFirst();
            return;
        }
        mService.getConnectionManager().addObserver(this);
        mService.getModel().getToastObservable().addObserver(this);
    }

    @Override
    protected void onPauseWithService() {
        super.onPauseWithService();
        if (mService == null || mService.getModel() == null)
            return;
        mService.getConnectionManager().deleteObserver(this);
        mService.getModel().getToastObservable().deleteObserver(this);
    }

    @Override
    public void update(final Observable observable, final Object o) {
        if (observable instanceof ConnectionManager) {
            final ConnectionManager cm = mService.getConnectionManager();
            if (cm.isLoginRequired()) {
                cm.login(this, cm.getHWDeviceData(), cm.getHWResolver());
            } else if (cm.isDisconnected()) {
                toFirst();
            }
        } else if (observable instanceof ToastObservable) {
            final ToastObservable tObs = (ToastObservable) observable;
            UI.toast(this, tObs.getMessage(getResources()), Toast.LENGTH_LONG);
        }
    }

    public void logout() {
        if (mService == null || getModel() == null) {
            toFirst();
            return;
        }
        mService.getConnectionManager().deleteObserver(this);
        mService.getExecutor().execute(() -> {
            mService.disconnect();
            toFirst();
        });
    }

    public void toFirst() {
        if (!mChangingActivity) {
            mChangingActivity = true;
            final Intent intent = new Intent(this, FirstScreenActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // No hardware wallet, jump to PIN or mnemonic entry
            if (mService.cfgPin().getString("ident", null) != null)
                startActivity(new Intent(this, PinActivity.class));
            else
                startActivity(new Intent(this, MnemonicActivity.class));
            finishOnUiThread();
        }
    }

    protected void onAssetSelected(final String assetId, final BalanceData balance) {
        Log.d("ASSET", "selected " + assetId);
        if (getCallingActivity() !=
            null && getCallingActivity().getClassName().equals(TabbedMainActivity.class.getName()) ) {
            if ("btc".equals(assetId))
                return;
            final Intent intent = new Intent(this, AssetActivity.class);
            intent.putExtra("ASSET_ID", assetId)
            .putExtra("ASSET_INFO", balance.getAssetInfo())
            .putExtra("SATOSHI", balance.getSatoshi());
            startActivity(intent);
            return;
        }
        final Intent intent = getIntent();
        intent.putExtra(PrefKeys.ASSET_SELECTED, assetId);
        setResult(RESULT_OK, intent);
        finish();
    }
}
