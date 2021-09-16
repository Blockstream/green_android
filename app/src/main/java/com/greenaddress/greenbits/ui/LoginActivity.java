package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.greenaddress.Bridge;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenbits.ui.components.ProgressBarHandler;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import javax.annotation.Nullable;

import io.reactivex.android.schedulers.AndroidSchedulers;

public abstract class LoginActivity extends GaActivity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSession().getNotificationModel().getTorObservable().observeOn(AndroidSchedulers.mainThread()).subscribe((
                                                                                                                       JsonNode
                                                                                                                       jsonNode) -> {
            final int progress = jsonNode.get(
                "progress").asInt(0);
            final ProgressBarHandler pbar = getProgressBarHandler();
            if (pbar != null)
                pbar.setMessage(String.format("%s %d %%",getString(R.string.id_tor_status), progress));
        }, (err) -> {
            Log.d(TAG, err.getLocalizedMessage());
        });
    }

    public void onPostLogin() {
        // Uncomment to test slow login post processing
        // android.os.SystemClock.sleep(10000);
        Log.d(TAG, "Success LOGIN callback onPostLogin" );
    }

    public void connect(@Nullable HWWallet hwWallet) throws Exception {
        final String network = PreferenceManager.getDefaultSharedPreferences(this).getString(PrefKeys.NETWORK_ID_ACTIVE, "mainnet");

        getSession().setNetwork(network);
        Bridge.INSTANCE.connect(this, getSession().getNativeSession(), network, hwWallet);
    }
}
