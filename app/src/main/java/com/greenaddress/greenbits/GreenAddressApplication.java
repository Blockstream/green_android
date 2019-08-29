package com.greenaddress.greenbits;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
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

import org.bitcoinj.crypto.MnemonicCode;

import java.util.ArrayList;
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
            NotificationChannel nc = new NotificationChannel("spv_channel", getString(R.string.id_spv_notifications),
                                                             NotificationManager.IMPORTANCE_LOW);
            nc.setDescription(getString(R.string.id_displays_the_progress_of_spv));
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(nc);
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
        try {
            GDK.init(new JSONConverterImpl(), details);
        } catch (Exception e) {
            failHard("GDK initialization failed", e.getMessage());
            return;
        }

        createNotificationChannel();

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
}
