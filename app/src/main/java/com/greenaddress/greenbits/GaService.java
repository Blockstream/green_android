package com.greenaddress.greenbits;

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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.PinData;
import com.greenaddress.greenapi.data.SettingsData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenapi.model.SettingsObservable;
import com.greenaddress.greenbits.spv.SPV;
import com.greenaddress.greenbits.ui.BuildConfig;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.utils.Fiat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GaService extends Service  {
    private static final String TAG = GaService.class.getSimpleName();

    private NetworkData mNetwork;
    private Model mModel;
    private ConnectionManager mConnectionManager;
    private GDKSession mSession;
    private final ListeningExecutorService mExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(8));
    private BroadcastReceiver mNetConnectivityReceiver;

    private String mSignUpMnemonic;
    private Bitmap mSignUpQRCode;
    private String mDeviceId;
    private boolean mUserCancelledPINEntry = false;

    private final SPV mSPV = new SPV(this);

    private int mRefCount; // Number of non-paused activities using us
    private ScheduledThreadPoolExecutor mTimerExecutor = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> mDisconnectTimer;

    // This could be a local variable in theory but since there is a warning in the documentation
    // about possibly being garbage collected has been made a member of the class
    // https://developer.android.com/reference/android/content/SharedPreferences
    private SharedPreferences.OnSharedPreferenceChangeListener mSyncListener;

    public NetworkData getNetwork() {
        return mNetwork;
    }

    public NetworkParameters getNetworkParameters() {
        return mNetwork.getNetworkParameters();
    }

    public boolean isElements() {
        return mNetwork.isElements();
    }

    public boolean isMainnet() {
        return mNetwork.IsNetworkMainnet();
    }

    public boolean isRegtest() {
        return mNetwork.isRegtest();
    }

    public synchronized void disconnect() {
        mConnectionManager.disconnect();
        mSession = GDKSession.getInstance();
    }

    public synchronized void connect() {
        mConnectionManager.setNetwork(mNetwork.getNetwork());
        mConnectionManager.connect();
    }

    public synchronized void reconnect() {
        disconnect();
        connect();
    }

    public boolean hasPin() {
        final String ident = cfgPin().getString("ident", null);
        return ident != null;
    }

    class GaBinder extends Binder {
        GaService getService() { return GaService.this; }
    }
    private final IBinder mBinder = new GaBinder();

    @Override
    public IBinder onBind(final Intent intent) { return mBinder; }

    public void onBound(final GreenAddressApplication app) {
        // Update our state when network connectivity changes.
        mNetConnectivityReceiver = new BroadcastReceiver() {
            public void onReceive(final Context context, final Intent intent) {
                onNetConnectivityChanged();
            }
        };
        app.registerReceiver(mNetConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        // Fire a fake connectivity change to kick start the state machine
        mNetConnectivityReceiver.onReceive(null, null);
    }

    public ListeningExecutorService getExecutor() {
        return mExecutor;
    }

    public Model getModel() {
        return mModel;
    }

    public GDKSession getSession() {
        return mSession;
    }

    public ConnectionManager getConnectionManager() {
        return mConnectionManager;
    }

    public String getReceivingId() {
        return getSubaccountData(0).getReceivingId();
    }

    public File getSPVChainFile(final String networkName) {
        final String dirName;
        if (getNetwork().IsNetworkMainnet()) {
            dirName = "blockstore_" + getReceivingId();
        } else {
            dirName = "blockstore_" + networkName;
        }

        Log.i(TAG, "dirName:" + dirName);
        return new File(getDir(dirName, Context.MODE_PRIVATE), "blockchain.spvchain");
    }

    public File getSPVChainFile() {
        return getSPVChainFile(getNetwork().getName());
    }

    public boolean getUserCancelledPINEntry() {
        return mUserCancelledPINEntry;
    }

    public void setUserCancelledPINEntry(final boolean value) {
        mUserCancelledPINEntry = value;
    }

    public String getBitcoinUnit() {
        return mModel.getSettings().getUnit();
    }

    public String getUnitKey() {
        final String unit = getBitcoinUnit();
        return unit.equals("\u00B5BTC") ? "ubtc" : unit.toLowerCase(Locale.US);
    }

    public String getValueString(final ObjectNode amount, final boolean asFiat, boolean withUnit) {
        if (asFiat)
            return amount.get("fiat").asText() + (withUnit ? (" " + getFiatCurrency()) : "");
        return amount.get(getUnitKey()).asText() + (withUnit ? (" " + getBitcoinUnit()) : "");
    }

    public boolean isWatchOnly() {
        return mConnectionManager.isWatchOnly();
    }

    public String getCurrentNetworkId() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(PrefKeys.NETWORK_ID_ACTIVE, "mainnet");
    }

    public void setCurrentNetworkId(final String networkId) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(PrefKeys.NETWORK_ID_ACTIVE, networkId).apply();

        final HashMap<String, NetworkData> networks = GDKSession.getNetworks();

        mNetwork = null;
        for (final NetworkData n : networks.values()) {
            if (n.getNetwork().equals(networkId)) {
                mNetwork = n;
                break;
            }
        }
    }

    public SharedPreferences cfg() {
        return getSharedPreferences(getNetwork().getNetwork(), MODE_PRIVATE);
    }

    public SharedPreferences cfgPin() {
        return getSharedPreferences(getNetwork().getNetwork() + "_pin", MODE_PRIVATE);
    }

    public SharedPreferences.Editor cfgEdit() { return cfg().edit(); }

    public String getProxyHost() { return cfg().getString(PrefKeys.PROXY_HOST, ""); }
    public String getProxyPort() { return cfg().getString(PrefKeys.PROXY_PORT, ""); }
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
    public int getSPVBlocksRemaining() { return mSPV.getSPVBlocksRemaining(); }
    public Coin getSPVVerifiedBalance(final int subAccount) {
        final Coin balance = mSPV.getVerifiedBalance(subAccount);
        return balance == null ? Coin.ZERO : balance;
    }

    public boolean isSPVVerified(final Sha256Hash txHash) { return mSPV.isVerified(txHash); }

    public void enableSPVPingMonitoring() { mSPV.enablePingMonitoring(); }
    public void disableSPVPingMonitoring() { mSPV.disablePingMonitoring(); }

    @Override
    public void onCreate() {
        super.onCreate();

        if(GreenAddressApplication.isRunningTest())
            return;

        // Uncomment to test slow service creation
        // android.os.SystemClock.sleep(10000);

        mSession = GDKSession.getInstance();
        final String activeNetwork = getCurrentNetworkId();
        setCurrentNetworkId(activeNetwork);
        if (mNetwork == null) {
            // Handle a previously registered network being deleted
            setCurrentNetworkId("mainnet");
        }
        mConnectionManager = new ConnectionManager(mSession, mNetwork.getNetwork(), getProxyHost(), getProxyPort(), getTorEnabled());
        mExecutor.execute(() -> mConnectionManager.connect());

        mDeviceId = cfg().getString(PrefKeys.DEVICE_ID, null);
        if (mDeviceId == null) {
            // Generate a unique device id
            mDeviceId = UUID.randomUUID().toString();
            cfgEdit().putString(PrefKeys.DEVICE_ID, mDeviceId).apply();
        }

        migratePreferences();
    }

    private void migratePreferences() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean migrated = sharedPreferences.getBoolean(PrefKeys.PREF_MIGRATED_V2_v3,false);
        if (!migrated) {
            // SPV_SYNCRONIZATION is now off by default unless a user had set the trusted peers,
            // in that case it stay how it was
            final boolean haveTrustedPeers = !"".equals(mSPV.getTrustedPeers());
            if (haveTrustedPeers && mSPV.isEnabled()) {
                cfgEdit().putBoolean(PrefKeys.SPV_ENABLED, true);
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

    public String getWatchOnlyUsername() {
        return mConnectionManager.getWatchOnlyUsername();
    }

    public void onPostLogin() {
        // Uncomment to test slow login post processing
        // android.os.SystemClock.sleep(10000);
        Log.d(TAG, "Success LOGIN callback onPostLogin" );

        mModel = new Model(mSession, mExecutor);
        initSettings();
        mSession.setNotificationModel(this);
        mConnectionManager.goPostLogin();

        if (!isWatchOnly()) {
            mSPV.startAsync();
        }
    }

    private void initSettings() {
        final Observer observer = new Observer() {
            @Override
            public void update(Observable observable, Object o) {
                if (observable instanceof SettingsObservable) {
                    Log.d(TAG,"initSettings");
                    final SettingsData settings = ((SettingsObservable) observable).getSettings();
                    final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(GaService.this);
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
                    mModel.getSettingsObservable().deleteObserver(this);
                }
            }
        };
        mModel.getSettingsObservable().addObserver(observer);
    }

    public String getMnemonic() {
        return mSession.getMnemonicPassphrase();
    }

    public List<Long> getFeeEstimates() {
        return mModel.getFeeObservable().getFees();
    }

    public ListenableFuture<Void> setPin(final String mnemonic, final String pin) {
        return mExecutor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                final PinData pinData = mSession.setPin(mnemonic, pin, "default");
                cfgPin().edit().putString("ident", pinData.getPinIdentifier())
                        .putString("encrypted", pinData.getEncryptedGB())
                        .putInt("counter", 0)
                        .putBoolean("is_six_digit", pin.length() == 6)
                        .apply();
               return null;
            }
        });
    }

    public void resetSignUp() {
        mSignUpMnemonic = null;
        if (mSignUpQRCode != null)
            mSignUpQRCode.recycle();
        mSignUpQRCode = null;
    }

    public String getSignUpMnemonic() {
        if (mSignUpMnemonic == null)
            mSignUpMnemonic = GDKSession.generateMnemonic("en");
        return mSignUpMnemonic;
    }

    public Bitmap getSignUpQRCode() {
        if (mSignUpQRCode == null)
            mSignUpQRCode = new QrBitmap(getSignUpMnemonic(), getResources().getColor(R.color.green)).getQRCode();
       return mSignUpQRCode;
    }

    public SubaccountData getSubaccountData(final int subAccount) {
        return getModel().getSubaccountDataObservable().getSubaccountDataWithPointer(subAccount);
    }

    public String getAddress(final int subAccount) {
        return getModel().getReceiveAddressObservable(subAccount).getReceiveAddress();
    }

    public BalanceData getBalanceData(final int subAccount) {
        return getModel().getBalanceDataObservable(subAccount).getBalanceData();
    }

    public Coin getCoinBalance(final int subAccount) {
        BalanceData balanceData = getBalanceData(subAccount);
        if (balanceData != null)
            return balanceData.getSatoshiAsCoin();
        return null;
    }

    public Fiat coinToFiat(final Coin btcValue) {
        try {
            final BalanceData balanceReq = new BalanceData();
            balanceReq.setSatoshi(btcValue.value);
            final BalanceData balanceRes = mSession.convertBalance(balanceReq);
            return balanceRes.getFiatAsFiat();
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Coin fiatToCoin(final Fiat fiatValue) {
        try {
            final BalanceData balanceReq = new BalanceData();
            balanceReq.setFiatAsFiat(fiatValue);
            final BalanceData balanceRes = mSession.convertBalance(balanceReq);
            return balanceRes.getSatoshiAsCoin();
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getFiatCurrency() {
        try {
            final BalanceData balanceReq = new BalanceData();
            balanceReq.setSatoshi(0L);
            final BalanceData balanceRes = mSession.convertBalance(balanceReq);
            return balanceRes.getFiatCurrency();
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void onNetConnectivityChanged() {
        final NetworkInfo info = getNetworkInfo();
        if (info == null) {
            // No network connection, go offline until notified that its back
            mConnectionManager.goOffline();
        } else if (mConnectionManager.isDisconnectedOrLess()) {
            // We have a network connection and are currently disconnected/offline:
            // Move to disconnected and try to reconnect
            mSPV.onNetConnectivityChangedAsync(info);
            mConnectionManager.goOnline();
            mConnectionManager.connect();
        } else
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
        cancelDisconnect();
    }

    public void decRef() {
        if (BuildConfig.DEBUG && mRefCount <= 0)
            throw new RuntimeException("Incorrect reference count");
        if (--mRefCount == 0)
            scheduleDisconnect();
    }

    private void cancelDisconnect() {
        if (mDisconnectTimer != null && !mDisconnectTimer.isCancelled()) {
            Log.d(TAG, "cancelDisconnect");
            mDisconnectTimer.cancel(false);
        }
    }

    public int getAutoLogoutTimeout() {
        if (mModel == null || mModel.getSettings() == null) {
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
        return mModel.getSettings().getAltimeout();
    }

    private void scheduleDisconnect() {
        if (getConnectionManager().isDisconnectedOrLess())
            return;
        final int delayMins = getAutoLogoutTimeout();
        cancelDisconnect();
        Log.d(TAG, "scheduleDisconnect in " + Integer.toString(delayMins) + " mins");
        mDisconnectTimer = mTimerExecutor.schedule(new Runnable() {
            public void run() {
                Log.d(TAG, "scheduled disconnect");
                disconnect();
            }
        }, delayMins, TimeUnit.MINUTES);
    }

    public ListenableFuture<Boolean> changeMemo(final String txHashHex, final String memo) {
        return mExecutor.submit(() -> mSession.changeMemo(txHashHex, memo));
    }

}
