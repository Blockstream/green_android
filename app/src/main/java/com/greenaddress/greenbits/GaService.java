package com.greenaddress.greenbits;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.data.AssetInfoData;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.PinData;
import com.greenaddress.greenapi.data.SettingsData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.model.AssetsDataObservable;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenapi.model.SettingsObservable;
import com.greenaddress.greenapi.model.TorProgressObservable;
import com.greenaddress.greenbits.spv.SPV;
import com.greenaddress.greenbits.ui.BuildConfig;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.assets.RegistryErrorActivity;
import com.greenaddress.greenbits.ui.authentication.FirstScreenActivity;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.h2.command.ddl.GrantRevoke;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.greenaddress.gdk.GDKSession.getSession;

public class GaService extends Service  {
    private static final String TAG = GaService.class.getSimpleName();

    //private NetworkData mNetwork;
    //private Model mModel;
    //private ConnectionManager mConnectionManager;
    //private final ListeningExecutorService mExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(8));

    private String mSignUpMnemonic;
    private Bitmap mSignUpQRCode;

    private final SPV mSPV = new SPV(this);

    private int mRefCount; // Number of non-paused activities using us
    private ScheduledThreadPoolExecutor mTimerExecutor = new ScheduledThreadPoolExecutor(1);
    private Long mDisconnectTimer;

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

    public ScheduledThreadPoolExecutor getTimerExecutor() {
        return mTimerExecutor;
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

        mTimerExecutor.scheduleWithFixedDelay(this::checkDisconnect, 5,5, TimeUnit.SECONDS);
    }

    public GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    public String getMnemonic() {
        return getSession().getMnemonicPassphrase();
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

    public void incRef() {
        ++mRefCount;
        rescheduleDisconnect();
    }

    public void decRef() {
        if (BuildConfig.DEBUG && mRefCount <= 0)
            throw new RuntimeException("Incorrect reference count");
        if (--mRefCount == 0)
            checkDisconnect();
    }

    public void rescheduleDisconnect() {
        mDisconnectTimer = System.currentTimeMillis() + getAutoLogoutTimeout() * 60 * 1000;
    }

    public int getAutoLogoutTimeout() {
        if (getGAApp().getModel() == null || getGAApp().getModel().getSettings() == null) {
            try {
                // we need to fetch this also locally,
                // cause we can scheduleDisconnect before being logged in
                final String altimeString = cfg().getString(PrefKeys.ALTIMEOUT, "5");
                return Integer.parseInt(altimeString);
            } catch (Exception e) {
                Log.e(TAG,"getAutoLogoutTimeout: " + e.getMessage());
                return 5;
            }
        }
        return getGAApp().getModel().getSettings().getAltimeout();
    }

    private void checkDisconnect() {
        if (getGAApp().getConnectionManager().isDisconnected())
            return;
        if (mDisconnectTimer != null && System.currentTimeMillis() > mDisconnectTimer) {
            getGAApp().getExecutor().submit(() -> getGAApp().getConnectionManager().disconnect());
        }
    }

    public ListenableFuture<Boolean> changeMemo(final String txHashHex, final String memo) {
        return getGAApp().getExecutor().submit(() -> getSession().changeMemo(txHashHex, memo));
    }

}
