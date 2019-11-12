package com.greenaddress.greenbits;

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

import androidx.multidex.MultiDexApplication;

import com.blockstream.libgreenaddress.GDK;
import com.blockstream.libwally.Wally;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.gdk.JSONConverterImpl;
import com.greenaddress.greenapi.CryptoHelper;
import com.greenaddress.greenapi.GAException;
import com.greenaddress.greenapi.MnemonicHelper;
import com.greenaddress.greenbits.ui.FailHardActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import org.bitcoinj.crypto.MnemonicCode;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class GreenAddressApplication extends MultiDexApplication {

    private static final String TAG = GreenAddressApplication.class.getSimpleName();

    public GaService mService;
    public final SettableFuture<Void> onServiceAttached = SettableFuture.create();

    private void failHard(final String title, final String message) {
        final Intent fail = new Intent(this, FailHardActivity.class);
        fail.putExtra("errorTitle", title);
        final String supportMessage = "Please contact info@greenaddress.it for support.";
        fail.putExtra("errorContent", String.format("%s. %s", message, supportMessage));
        fail.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(fail);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);

            NotificationChannel nc = new NotificationChannel("spv_channel", getString(R.string.id_spv_notifications),
                                                             NotificationManager.IMPORTANCE_LOW);
            nc.setDescription(getString(R.string.id_displays_the_progress_of_spv));
            nm.createNotificationChannel(nc);

            NotificationChannel tor_nc = new NotificationChannel("tor_channel", "Tor Status",
                    NotificationManager.IMPORTANCE_LOW);
            tor_nc.setDescription("Displays the progress of Tor initialization");
            nm.createNotificationChannel(tor_nc);
        }
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
        } catch (Exception e) {
            failHard("GDK initialization failed", e.getMessage());
            return;
        }

        createNotificationChannel();
        migratePreferences();

        // Provide bitcoinj with Mnemonics. These are used if we need to create a fake
        // wallet during SPV_SYNCRONIZATION syncing to prevent an exception.
        try {
            final ArrayList<String> words = new ArrayList<>(Wally.BIP39_WORDLIST_LEN);
            MnemonicHelper.initWordList(words, null);
            MnemonicCode.INSTANCE = new MnemonicCode(words, null);
        } catch (final Exception e) {
            e.printStackTrace();
            failHard("Bitcoinj initialization failed", e.getMessage());
            return;
        }

        Log.d(TAG, "onCreate: binding service");
        final ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(final ComponentName className,
                                           final IBinder service) {
                Log.d(TAG, "onServiceConnected: dispatching onServiceAttached callbacks");
                mService = ((GaService.GaBinder) service).getService();
                mService.onBound(GreenAddressApplication.this);
                onServiceAttached.set(null);
            }

            @Override
            public void onServiceDisconnected(final ComponentName name) {
                Log.d(TAG, "onServiceDisconnected: dispatching onServiceAttached exception");
                onServiceAttached.setException(new GAException(name.toString()));
            }
        };

        final Intent intent = new Intent(this, GaService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private static AtomicBoolean isRunningTest;
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

    public String getCurrentNetwork() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(PrefKeys.NETWORK_ID_ACTIVE, "mainnet");
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

}
