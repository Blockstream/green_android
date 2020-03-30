package com.greenaddress.greenbits.spv;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.GreenAddressApplication;
import com.greenaddress.greenbits.ui.R;

import java.io.File;

public class GaService extends Service  {
    private static final String TAG = GaService.class.getSimpleName();

    // This could be a local variable in theory but since there is a warning in the documentation
    // about possibly being garbage collected has been made a member of the class
    // https://developer.android.com/reference/android/content/SharedPreferences
    private SharedPreferences.OnSharedPreferenceChangeListener mSyncListener;

    class GaBinder extends Binder {
        GaService getService() { return GaService.this; }
    }
    private final IBinder mBinder = new GaBinder();

    @Override
    public IBinder onBind(final Intent intent) { return mBinder; }

    public void onBound(final Context ctx) {
        // Update our state when network connectivity changes.
        final BroadcastReceiver netConnectivityReceiver = new BroadcastReceiver() {
            public void onReceive(final Context context, final Intent intent) {
                onNetConnectivityChanged();
            }
        };
        ctx.registerReceiver(netConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        // Fire a fake connectivity change to kick start the state machine
        netConnectivityReceiver.onReceive(null, null);
    }

    public File getSPVChainFile(final String networkName) {
        final String dirName = "blockstore_" + networkName;
        Log.i(TAG, "dirName:" + dirName);
        return new File(getDir(dirName, Context.MODE_PRIVATE), "blockchain.spvchain");
    }

    public NetworkData getNetwork() {
        return ((GreenAddressApplication) getApplication()).getCurrentNetworkData();
    }
    public File getSPVChainFile() {
        return getSPVChainFile(getNetwork().getName());
    }

    public GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    private void onNetConnectivityChanged() {
        final NetworkInfo info = getNetworkInfo();
        Log.d(TAG, "onNetConnectivityChanged " + info);
        // TODO: auto-reconnect using gdk
        if (info != null)
            getGAApp().getSpv().onNetConnectivityChangedAsync(info);
    }

    public NetworkInfo getNetworkInfo() {
        final Context ctx = getApplicationContext();
        final ConnectivityManager cm;
        cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            final NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnectedOrConnecting() ? ni : null;
        } catch (final Exception e) {
            return null;
        }
    }

    public static void createNotificationChannel(final Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            final NotificationChannel nc =
                new NotificationChannel("spv_channel", ctx.getString(R.string.id_spv_notifications),
                                        NotificationManager.IMPORTANCE_LOW);
            nc.setDescription(ctx.getString(R.string.id_displays_the_progress_of_spv));
            nm.createNotificationChannel(nc);
        }
    }
}
