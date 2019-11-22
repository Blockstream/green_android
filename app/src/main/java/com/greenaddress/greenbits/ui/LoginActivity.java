package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.SettingsData;
import com.greenaddress.greenapi.model.AssetsDataObservable;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenapi.model.SettingsObservable;
import com.greenaddress.greenapi.model.TorProgressObservable;
import com.greenaddress.greenbits.ui.assets.RegistryErrorActivity;
import com.greenaddress.greenbits.ui.components.ProgressBarHandler;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.util.Observable;
import java.util.Observer;

import static com.greenaddress.gdk.GDKSession.getSession;

public abstract class LoginActivity extends GaActivity implements Observer {

    private final TorProgressObservable mTorProgressObservable = new TorProgressObservable();

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
        getSession().getNotificationModel().setTorProgressObservable(mTorProgressObservable);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getConnectionManager() != null) {
            final ConnectionManager cm = getConnectionManager();
            cm.clearPreviousLoginError();
        }
        mTorProgressObservable.addObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mTorProgressObservable.deleteObserver(this);
    }

    @Override
    public void update(final Observable observable, final Object o) {
        if (observable instanceof TorProgressObservable) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final int progress = ((TorProgressObservable) observable).get().get("progress").asInt(0);
                    final ProgressBarHandler pbar = getProgressBarHandler();
                    if (pbar != null)
                        pbar.setMessage(String.format("%s %d%",getString(R.string.id_tor_status),
                                                      String.valueOf(progress)));
                }
            });
        }
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
        final ConnectionManager connectionManager = getConnectionManager();
        final NetworkData networkData = getGAApp().getCurrentNetworkData();
        final Model model = new Model(getGAApp().getExecutor(), networkData);
        final SharedPreferences preferences = getSharedPreferences(networkData.getNetwork(), MODE_PRIVATE);
        final int activeAccount =
            connectionManager.isLoginWithPin() ? preferences.getInt(PrefKeys.ACTIVE_SUBACCOUNT, 0) : 0;
        if (model.getSubaccountDataObservable().getSubaccountDataWithPointer(activeAccount) != null)
            model.getActiveAccountObservable().setActiveAccount(activeAccount);
        else
            model.getActiveAccountObservable().setActiveAccount(0);
        getGAApp().setModel(model);
        getSession().getNotificationModel().setModel(model);
        getSession().getNotificationModel().setConnectionManager(connectionManager);
        initSettings();
        connectionManager.goPostLogin();

        // refresh assets in liquid network
        if (networkData.getLiquid()) {
            model.getAssetsObservable().addObserver((observable, o) -> {
                final AssetsDataObservable assetsDataObservable = (AssetsDataObservable) observable;
                if (!assetsDataObservable.isAssetsLoaded() && !assetsDataObservable.isShownErrorPopup()) {

                    final Intent intent = new Intent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setClass(this, RegistryErrorActivity.class);
                    startActivity(intent);

                    assetsDataObservable.setShownErrorPopup();
                }
            });
            model.getAssetsObservable().refresh();
        }

        // check and start spv if enabled
        final boolean isSpvEnabled = preferences.getBoolean(PrefKeys.SPV_ENABLED, false);
        if (!connectionManager.isWatchOnly() && isSpvEnabled) {
            try {
                getGAApp().getSpv().startService(this);
            } catch (final Exception e) {
                e.printStackTrace();
                Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initSettings() {
        final Observer observer = new Observer() {
            @Override
            public void update(final Observable observable, final Object o) {
                if (observable instanceof SettingsObservable) {
                    Log.d(TAG,"initSettings");
                    final SettingsData settings = ((SettingsObservable) observable).getSettings();
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
                    getModel().getSettingsObservable().deleteObserver(this);
                }
            }
        };
        getModel().getSettingsObservable().addObserver(observer);
    }
}
