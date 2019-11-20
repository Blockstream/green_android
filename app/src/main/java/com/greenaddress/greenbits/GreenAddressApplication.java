package com.greenaddress.greenbits;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.multidex.MultiDexApplication;

import com.blockstream.libgreenaddress.GDK;
import com.blockstream.libwally.Wally;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.gdk.JSONConverterImpl;
import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.CryptoHelper;
import com.greenaddress.greenapi.GAException;
import com.greenaddress.greenapi.MnemonicHelper;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.SettingsData;
import com.greenaddress.greenapi.model.AssetsDataObservable;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenapi.model.SettingsObservable;
import com.greenaddress.greenapi.model.TorProgressObservable;
import com.greenaddress.greenbits.spv.GaService;
import com.greenaddress.greenbits.spv.SPV;
import com.greenaddress.greenbits.ui.FailHardActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.assets.RegistryErrorActivity;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import org.bitcoinj.crypto.MnemonicCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.greenaddress.gdk.GDKSession.getSession;

public class GreenAddressApplication extends MultiDexApplication {

    private static final String TAG = GreenAddressApplication.class.getSimpleName();
    private final TorProgressObservable mTorProgressObservable = new TorProgressObservable();
    private final ListeningExecutorService mExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(8));
    private Model mModel;
    private ConnectionManager mConnectionManager = new ConnectionManager("mainnet");
    private static AtomicBoolean isRunningTest;
    public final SPV mSPV = new SPV();
    private Timer mTimer = new Timer();

    private void failHard(final String title, final String message) {
        final Intent fail = new Intent(this, FailHardActivity.class);
        fail.putExtra("errorTitle", title);
        final String supportMessage = "Please contact info@greenaddress.it for support.";
        fail.putExtra("errorContent", String.format("%s. %s", message, supportMessage));
        fail.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(fail);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if(isRunningTest())
            return;

        if (!Wally.isEnabled()) {
            failHard("Unsupported platform", "A suitable libwallycore.so was not found");
            return;
        }

        if (!CryptoHelper.initialize()) {
            failHard("Initialization failed", "Cryptographic initialization failed");
            return;
        }

        // GDK initialization parameters
        final ObjectNode details = (new ObjectMapper()).createObjectNode();
        details.put("datadir", getFilesDir().getAbsolutePath());
        try {
            GDK.init(new JSONConverterImpl(), details);
        } catch (final Exception e) {
            failHard("GDK initialization failed", e.getMessage());
            return;
        }

        migratePreferences();


        final SharedPreferences preferences = getSharedPreferences(getCurrentNetwork(), MODE_PRIVATE);
        final boolean isEnabled = preferences.getBoolean(PrefKeys.SPV_ENABLED, false);
        try {
            if (isEnabled)
                mSPV.startService(this);
        } catch (final Exception e) {
            e.printStackTrace();
            failHard("Bitcoinj initialization failed", e.getMessage());
        }
    }

    public static synchronized boolean isRunningTest() {
        if (null == isRunningTest) {
            boolean istest;
            try {
                Class.forName("com.blockstream.libgreenaddress.GSDK");
                istest = true;
            } catch (ClassNotFoundException e) {
                istest = false;
            }
            isRunningTest = new AtomicBoolean(istest);
        }
        return isRunningTest.get();
    }

    // migrate preferences from previous GreenBits app with single network

    private void migratePreferences() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean migrated = sharedPreferences.getBoolean(PrefKeys.PREF_MIGRATED_V2_v3,false);
        if (!migrated) {
            // SPV_SYNCRONIZATION is now off by default unless a user had set the trusted peers,
            // in that case it stay how it was

            final SharedPreferences preferences = getSharedPreferences(getCurrentNetwork(), MODE_PRIVATE);
            final boolean isEnabled = preferences.getBoolean(PrefKeys.SPV_ENABLED, false);
            final boolean haveTrustedPeers = !"".equals(preferences.getString(PrefKeys.TRUSTED_ADDRESS, "").trim());
            if (haveTrustedPeers && isEnabled) {
                preferences.edit().putBoolean(PrefKeys.SPV_ENABLED, true);
            }
            // mainnet PIN migration
            copyPreferences(getSharedPreferences("pin", MODE_PRIVATE), getSharedPreferences("mainnet_pin", MODE_PRIVATE));
            sharedPreferences.edit().putBoolean(PrefKeys.PREF_MIGRATED_V2_v3, true).apply();
        }
    }

    private static void copyPreferences(final SharedPreferences source, final SharedPreferences destination) {
        if (source.getAll().isEmpty())
            return;
        final SharedPreferences.Editor destinationEditor = destination.edit();
        for (final Map.Entry<String, ?> entry : source.getAll().entrySet())
            writePreference(entry.getKey(), entry.getValue(), destinationEditor);
        destinationEditor.apply();
    }

    private static SharedPreferences.Editor writePreference(final String key, final Object value, final SharedPreferences.Editor preferences) {
        if (value instanceof Boolean)
            return preferences.putBoolean(key, (Boolean) value);
        else if (value instanceof String)
            return preferences.putString(key, (String) value);
        else if (value instanceof Long)
            return preferences.putLong(key, (Long) value);
        else if (value instanceof Integer)
            return preferences.putInt(key, (Integer) value);
        else if (value instanceof Float)
            return preferences.putFloat(key, (Float) value);
        else if (value instanceof Set)
            return preferences.putStringSet(key, (Set<String>) value);
        else
            throw new RuntimeException("Unknown preference type");
    }

    // get Network function
    public static NetworkData getNetworkData(final String network) {
        final List<NetworkData> networks = GDKSession.getNetworks();
        for (final NetworkData n : networks) {
            if (n.getNetwork().equals(network)) {
                return n;
            }
        }
        return null;
    }

    public void setCurrentNetwork(final String networkId) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(PrefKeys.NETWORK_ID_ACTIVE, networkId).apply();
    }

    public String getCurrentNetwork() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(PrefKeys.NETWORK_ID_ACTIVE, "mainnet");
    }
    public NetworkData getCurrentNetworkData() {
        return getNetworkData(getCurrentNetwork());
    }

    public Model getModel() {
        return mModel;
    }

    public ConnectionManager getConnectionManager() {
        return mConnectionManager;
    }

    public boolean isWatchOnly() {
        return mConnectionManager.isWatchOnly();
    }

    public String getWatchOnlyUsername() {
        return mConnectionManager.getWatchOnlyUsername();
    }

    public boolean warnIfOffline(final Activity activity) {
        if(getConnectionManager().isOffline()) {
            UI.toast(activity, R.string.id_you_are_not_connected_to_the, Toast.LENGTH_LONG);
            return true;
        }
        return false;
    }

    public void resetSession() {
        getSession().disconnect();
        getSession().destroy();
    }

    public TorProgressObservable getTorProgressObservable() {
        return mTorProgressObservable;
    }

    public ListeningExecutorService getExecutor() {
        return mExecutor;
    }

    public void onPostLogin() {
        // Uncomment to test slow login post processing
        // android.os.SystemClock.sleep(10000);
        Log.d(TAG, "Success LOGIN callback onPostLogin" );

        mModel = new Model(getExecutor(), getCurrentNetworkData());
        initSettings();
        getSession().setNotificationModel(mModel, mConnectionManager);
        final SharedPreferences preferences = getSharedPreferences(getCurrentNetwork(), MODE_PRIVATE);
        final int activeAccount = mConnectionManager.isLoginWithPin() ? preferences.getInt(PrefKeys.ACTIVE_SUBACCOUNT, 0) : 0;
        if (mModel.getSubaccountDataObservable().getSubaccountDataWithPointer(activeAccount) != null)
            mModel.getActiveAccountObservable().setActiveAccount(activeAccount);
        else
            mModel.getActiveAccountObservable().setActiveAccount(0);

        // FIXME the following prevents an issue when notification are not transmitted even if login was successful
        if (mModel.getBlockchainHeightObservable().getHeight() == null) {
            return;
        }
        if (getCurrentNetworkData().getLiquid()) {
            mModel.getAssetsObservable().addObserver((observable, o) -> {
                final AssetsDataObservable assetsDataObservable = (AssetsDataObservable) observable;
                if (assetsDataObservable.isAssetsLoaded() || assetsDataObservable.isShownErrorPopup()) {
                    return;
                }

                final Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setClass(this, RegistryErrorActivity.class);
                startActivity(intent);

                assetsDataObservable.setShownErrorPopup();
            });
            mModel.getAssetsObservable().refresh();
        }
        mConnectionManager.goPostLogin();

        final boolean isSpvEnabled = preferences.getBoolean(PrefKeys.SPV_ENABLED, false);
        if (!mConnectionManager.isWatchOnly() && isSpvEnabled) {
            try {
                mSPV.startService(this);
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
                    final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    final SharedPreferences.Editor edit = pref.edit();
                    if (settings.getPricing() != null)
                        edit.putString(PrefKeys.PRICING, settings.getPricing().toString());
                    if (settings.getNotifications() != null)
                        edit.putBoolean(PrefKeys.TWO_FAC_N_LOCKTIME_EMAILS, settings.getNotifications().isEmailIncoming());
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

    public SPV getSpv() {
        return mSPV;
    }
    public Timer getTimer() {
        return mTimer;
    }
    public void setTimer(final Timer timer) {
        mTimer = timer;
    }
}
