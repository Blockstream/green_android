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
import com.greenaddress.greenapi.CryptoHelper;
import com.greenaddress.greenbits.ui.FailHardActivity;

public class GreenAddressApplication extends MultiDexApplication {

    public GaService gaService;
    @NonNull public final SettableFuture<Void> onServiceAttached = SettableFuture.create();
    private boolean mBound = false;
    private String errorTitle, errorContent;
    @Nullable
    private ConnectivityObservable connectionObservable = new ConnectivityObservable();
    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(final ComponentName className,
                                       final IBinder service) {
            gaService = ((GaService.GaBinder)service).getService();
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

        if (errorTitle == null && !CryptoHelper.initialize()) {
            errorTitle = "Initialization failed";
            errorContent = "Cryptographic initialization failed, please contact support info@greenaddress.it";
        }

        if (errorTitle == null) {
            final Intent intent = new Intent(this, GaService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
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
