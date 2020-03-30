package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.SettingsData;
import com.greenaddress.greenbits.ui.assets.RegistryErrorActivity;
import com.greenaddress.greenbits.ui.components.ProgressBarHandler;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;
import com.greenaddress.greenbits.wallets.HardwareCodeResolver;

import java.util.Locale;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.greenaddress.greenapi.Registry.getRegistry;
import static com.greenaddress.greenapi.Session.getSession;

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

    protected int getCode(final Exception e) {
        try {
            final String stringCode = e.getMessage().split(" ")[1];
            return Integer.parseInt(stringCode);
        } catch (final Exception ignored) {}
        return 1;
    }

    public void onPostLogin() {
        // Uncomment to test slow login post processing
        // android.os.SystemClock.sleep(10000);
        Log.d(TAG, "Success LOGIN callback onPostLogin" );

        // setup data observers
        final NetworkData networkData = getGAApp().getCurrentNetworkData();
        final SharedPreferences preferences = getSharedPreferences(networkData.getNetwork(), MODE_PRIVATE);
        initSettings();

        // refresh assets in liquid network
        if (networkData.getLiquid()) {
            Observable.just(getSession())
            .subscribeOn(Schedulers.computation())
            .map((session) -> {
                getRegistry().cached();
                return session;
            })
            .map((session) -> {
                getRegistry().refresh();
                return session;
            })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe((session) -> {
                Log.d(TAG, "Assets refreshed");
            }, (final Throwable e) -> {
                final Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setClass(this, RegistryErrorActivity.class);
                startActivity(intent);
            });
        }

        // check and start spv if enabled
        final boolean isSpvEnabled = preferences.getBoolean(PrefKeys.SPV_ENABLED, false);
        if (!getSession().isWatchOnly() && isSpvEnabled) {
            try {
                getGAApp().getSpv().startService(getGAApp());
            } catch (final Exception e) {
                e.printStackTrace();
                Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initSettings() {
        Log.d(TAG,"initSettings");
        final SettingsData settings = getSession().getSettings();
        final SharedPreferences pref =
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final SharedPreferences.Editor edit = pref.edit();
        if (settings.getPricing() != null)
            edit.putString(PrefKeys.PRICING, settings.getPricing().toString());
        if (settings.getNotifications() != null)
            edit.putBoolean(PrefKeys.TWO_FAC_N_LOCKTIME_EMAILS,
                            settings.getNotifications().isEmailIncoming());
        if (settings.getAltimeout() != null)
            edit.putString(PrefKeys.ALTIMEOUT, String.valueOf(settings.getAltimeout()));
        if (settings.getUnit() != null)
            edit.putString(PrefKeys.UNIT, settings.getUnit());
        if (settings.getRequiredNumBlocks() != null)
            edit.putString(PrefKeys.REQUIRED_NUM_BLOCKS, String.valueOf(settings.getRequiredNumBlocks()));
        if (settings.getPgp() != null)
            edit.putString(PrefKeys.PGP_KEY, settings.getPgp());
        edit.apply();
    }

    public void connect() throws Exception {
        final String network = PreferenceManager.getDefaultSharedPreferences(this).getString(
            PrefKeys.NETWORK_ID_ACTIVE, "mainnet");
        final SharedPreferences preferences = this.getSharedPreferences(network, MODE_PRIVATE);
        final String proxyHost = preferences.getString(PrefKeys.PROXY_HOST, "");
        final String proxyPort = preferences.getString(PrefKeys.PROXY_PORT, "");
        final Boolean proxyEnabled = preferences.getBoolean(PrefKeys.PROXY_ENABLED, false);
        final Boolean torEnabled = preferences.getBoolean(PrefKeys.TOR_ENABLED, false);

        String deviceId = preferences.getString(PrefKeys.DEVICE_ID, null);
        if (deviceId == null) {
            // Generate a unique device id
            deviceId = UUID.randomUUID().toString();
            preferences.edit().putString(PrefKeys.DEVICE_ID, deviceId).apply();
        }

        final boolean isDebug = BuildConfig.DEBUG;
        Log.d(TAG,"connecting to " + network + (isDebug ? " in DEBUG mode" : "") + (torEnabled ? " with TOR" : ""));
        if (proxyEnabled || torEnabled) {
            final String proxyString;
            if (!proxyEnabled || TextUtils.isEmpty(proxyHost)) {
                proxyString = "";
            } else {
                proxyString = String.format(Locale.US, "%s:%s", proxyHost, proxyPort);
                Log.d(TAG, "connecting with proxy " + proxyString);
            }
            getSession().connectWithProxy(network, proxyString, torEnabled, isDebug);
        } else {
            getSession().connect(network, isDebug);
        }
    }
}
