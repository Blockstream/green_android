package com.greenaddress.greenbits;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.blockstream.libwally.Wally;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.greenapi.GAException;
import com.greenaddress.greenapi.CryptoHelper;
import com.greenaddress.greenbits.ui.FailHardActivity;

public class GreenAddressApplication extends MultiDexApplication {

    private static final String TAG = GreenAddressApplication.class.getSimpleName();

    public GaService gaService;
    @NonNull public final SettableFuture<Void> onServiceAttached = SettableFuture.create();
    private boolean mBound = false;
    private String mErrorTitle, mErrorContent;
    @Nullable
    private ConnectivityObservable connectionObservable = new ConnectivityObservable();
    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(final ComponentName className,
                                       final IBinder service) {
            Log.d(TAG, "onServiceConnected: dispatching onServiceAttached callbacks");
            gaService = ((GaService.GaBinder)service).getService();
            mBound = true;
            connectionObservable.setService(gaService);
            onServiceAttached.set(null);
        }

        @Override
        public void onServiceDisconnected(@NonNull final ComponentName arg0) {
            Log.d(TAG, "onServiceDisconnected: dispatching onServiceAttached exception");
            mBound = false;
            connectionObservable = null;
            onServiceAttached.setException(new GAException(arg0.toString()));
        }
    };

    @Nullable
    public ConnectivityObservable getConnectionObservable() {
        if (mErrorTitle != null)
            failHard(mErrorTitle, mErrorContent);
        return connectionObservable;
    }

    private void failHard(final String title, final String message) {
        mErrorTitle = title;
        mErrorContent = message;
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

        Log.d(TAG, "onCreate: binding service");
        final Intent intent = new Intent(this, GaService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
}
