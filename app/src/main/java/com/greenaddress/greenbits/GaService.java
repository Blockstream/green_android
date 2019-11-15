package com.greenaddress.greenbits;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.spv.SPV;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;

import java.io.File;

public class GaService extends Service  {
    private static final String TAG = GaService.class.getSimpleName();

    private final SPV mSPV = new SPV(this);

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

    public void onBound(final GreenAddressApplication app) {
        // Update our state when network connectivity changes.
        final BroadcastReceiver netConnectivityReceiver = new BroadcastReceiver() {
            public void onReceive(final Context context, final Intent intent) {
                onNetConnectivityChanged();
            }
        };
        app.registerReceiver(netConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        // Fire a fake connectivity change to kick start the state machine
        netConnectivityReceiver.onReceive(null, null);
    }

    public File getSPVChainFile(final String networkName) {
        final String dirName;
        if (getNetwork().IsNetworkMainnet()) {
            dirName = "blockstore_" + getGAApp().getModel().getReceivingId();
        } else {
            dirName = "blockstore_" + networkName;
        }

        Log.i(TAG, "dirName:" + dirName);
        return new File(getDir(dirName, Context.MODE_PRIVATE), "blockchain.spvchain");
    }

    public NetworkData getNetwork() {
        return ((GreenAddressApplication) getApplication()).getCurrentNetworkData();
    }
    public File getSPVChainFile() {
        return getSPVChainFile(getNetwork().getName());
    }

    public SharedPreferences cfg() {
        final String network = PreferenceManager.getDefaultSharedPreferences(this).getString(PrefKeys.NETWORK_ID_ACTIVE, "mainnet");
        return getSharedPreferences(network, MODE_PRIVATE);
    }

    public SharedPreferences.Editor cfgEdit() { return cfg().edit(); }

    public String getProxyHost() { return cfg().getString(PrefKeys.PROXY_HOST, ""); }
    public String getProxyPort() { return cfg().getString(PrefKeys.PROXY_PORT, ""); }
    public boolean getProxyEnabled() { return cfg().getBoolean(PrefKeys.PROXY_ENABLED, false); }
    public boolean getTorEnabled() { return cfg().getBoolean(PrefKeys.TOR_ENABLED, false); }
    public boolean isProxyEnabled() { return !TextUtils.isEmpty(getProxyHost()) && !TextUtils.isEmpty(getProxyPort()); }

    // SPV_SYNCRONIZATION
    public String getSPVTrustedPeers() { return mSPV.getTrustedPeers(); }
    public void setSPVTrustedPeersAsync(final String peers) { mSPV.setTrustedPeersAsync(peers); }

    public boolean isSPVEnabled() { return mSPV.isEnabled(); }
    public void setSPVEnabledAsync(final boolean enabled) { mSPV.setEnabledAsync(enabled); }

    public boolean isSPVSyncOnMobileEnabled() { return mSPV.isSyncOnMobileEnabled(); }
    public void setSPVSyncOnMobileEnabledAsync(final boolean enabled) { mSPV.setSyncOnMobileEnabledAsync(enabled); }

    public void resetSPVAsync() { mSPV.resetAsync(); }

    public PeerGroup getSPVPeerGroup() { return mSPV.getPeerGroup(); }
    public int getSPVHeight() { return mSPV.getSPVHeight(); }

    public boolean isSPVVerified(final Sha256Hash txHash) { return mSPV.isVerified(txHash); }

    public void enableSPVPingMonitoring() { mSPV.enablePingMonitoring(); }
    public void disableSPVPingMonitoring() { mSPV.disablePingMonitoring(); }

    @Override
    public void onCreate() {
        super.onCreate();

        if(GreenAddressApplication.isRunningTest())
            return;
    }

    public GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    private void onNetConnectivityChanged() {
        final NetworkInfo info = getNetworkInfo();
        Log.d(TAG, "onNetConnectivityChanged " + info);
        // TODO: auto-reconnect using gdk
        if (info != null)
            mSPV.onNetConnectivityChangedAsync(info);
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
}
