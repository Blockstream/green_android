package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.greenaddress.Bridge;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.SettingsData;
import com.greenaddress.greenbits.ui.assets.RegistryErrorActivity;
import com.greenaddress.greenbits.ui.components.ProgressBarHandler;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.util.Locale;
import java.util.UUID;

import javax.annotation.Nullable;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


public abstract class LoginActivity extends GaActivity {

    protected void onLoggedIn() {
        final Intent intent = new Intent(LoginActivity.this, TabbedMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setData(getIntent().getData());
        intent.setAction(getIntent().getAction());
        startActivity(intent);
        finishOnUiThread();
    }

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
