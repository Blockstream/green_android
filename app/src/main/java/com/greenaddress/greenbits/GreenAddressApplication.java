package com.greenaddress.greenbits;

import org.bitcoinj.crypto.MnemonicCode;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.blockstream.libwally.Wally;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.greenapi.GAException;
import com.greenaddress.greenapi.CryptoHelper;
import com.greenaddress.greenbits.ui.FailHardActivity;
import com.greenaddress.greenbits.ui.MnemonicHelper;

import java.util.ArrayList;

public class GreenAddressApplication extends MultiDexApplication {

    private static final String TAG = GreenAddressApplication.class.getSimpleName();

    private ServiceConnection mConnection;
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

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            PRNGFixes.apply();
        } catch (final SecurityException e) {
            e.printStackTrace();
            failHard("Security exception", e.getMessage());
            return;
        }

        if (!Wally.isEnabled()) {
            failHard("Unsupported platform", "A suitable libwallycore.so was not found");
            return;
        }

        if (!CryptoHelper.initialize()) {
            failHard("Initialization failed", "Cryptographic initialization failed");
            return;
        }

        // Provide bitcoinj with Mnemonics. These are used if we need to create a fake
        // wallet during SPV syncing to prevent an exception.
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
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(final ComponentName className,
                                           final IBinder service) {
                Log.d(TAG, "onServiceConnected: dispatching onServiceAttached callbacks");
                mService = ((GaService.GaBinder)service).getService();
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
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
}
