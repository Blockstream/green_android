package com.greenaddress.greenbits;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.multidex.MultiDexApplication;

import com.blockstream.libwally.Wally;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.greenapi.GAException;
import com.greenaddress.greenbits.ui.FailHardActivity;

import org.bitcoin.NativeSecp256k1;
import org.bitcoin.NativeSecp256k1Util;
import org.bitcoin.Secp256k1Context;

import java.security.SecureRandom;

public class GreenAddressApplication extends MultiDexApplication {

    public GaService gaService;
    @NonNull public final SettableFuture<Void> onServiceAttached = SettableFuture.create();
    private boolean mBound = false;
    private String errorTitle, errorContent;
    @Nullable
    private ConnectivityObservable connectionObservable = new ConnectivityObservable();
    @Nullable
    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(final ComponentName className,
                                       final IBinder service) {
            final GaBinder binder = (GaBinder) service;
            gaService = binder.gaService;
            mBound = true;
            connectionObservable.setService(gaService);
            onServiceAttached.set(null);
        }

        @Override
        public void onServiceDisconnected(@NonNull final ComponentName arg0) {
            mBound = false;
            connectionObservable = null;
            onServiceAttached.setException(new GAException(arg0.toString()));
        }
    };

    @Nullable
    public ConnectivityObservable getConnectionObservable() {
        failHardOnCriticalErrors();
        return connectionObservable;
    }

    private boolean randomizeSecp256k1Context() {
        try {
            final SecureRandom secureRandom = new SecureRandom();
            final byte[] seed = new byte[32];
            secureRandom.nextBytes(seed);
            return NativeSecp256k1.randomize(seed);

        } catch (final NativeSecp256k1Util.AssertFailException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void failHardOnCriticalErrors() {
        // fail hard
        if (errorTitle != null) {
            final Intent fail = new Intent(this, FailHardActivity.class);
            fail.putExtra("errorTitle", errorTitle);
            fail.putExtra("errorContent", errorContent);
            fail.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(fail);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            PRNGFixes.apply();
        } catch (final SecurityException e) {
            e.printStackTrace();
            errorTitle = "Security exception";
            errorContent = String.format("%s please contact support info@greenaddress.it", e.getMessage());
        }

        if (errorTitle == null && !Wally.isEnabled()) {
            errorTitle = "libwallycore not found";
            errorContent = "libwallycore.so not found, this platform is not supported, please contact support info@greenaddress.it";
        }
        else if (errorTitle == null && Secp256k1Context.isEnabled()) {
            if (randomizeSecp256k1Context()) {
                final Intent intent = new Intent(this, GaService.class);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            } else {
                errorTitle = "Randomization failed";
                errorContent = "Warning: Randomization of secp256k1lib context failed, please contact support info@greenaddress.it";
            }
        } else if (errorTitle == null){
            errorTitle = "libsecp256k1 not found";
            errorContent = "libsecp256k1.so not found, this platform is not supported, please contact support info@greenaddress.it";
        }
        failHardOnCriticalErrors();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
}
