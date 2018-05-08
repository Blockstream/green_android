package com.greenaddress.greenbits;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

import com.blockstream.libwally.Wally;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLongs;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.GeneratedMessage;
import com.greenaddress.greenapi.ConfidentialAddress;
import com.greenaddress.greenapi.CryptoHelper;
import com.greenaddress.greenapi.ElementsRegTestParams;
import com.greenaddress.greenapi.HDClientKey;
import com.greenaddress.greenapi.HDKey;
import com.greenaddress.greenapi.INotificationHandler;
import com.greenaddress.greenapi.ISigningWallet;
import com.greenaddress.greenapi.JSONMap;
import com.greenaddress.greenapi.LoginData;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.Output;
import com.greenaddress.greenapi.PinData;
import com.greenaddress.greenapi.PreparedTransaction;
import com.greenaddress.greenapi.SWWallet;
import com.greenaddress.greenapi.WalletClient;
import com.greenaddress.greenbits.spv.SPV;
import com.greenaddress.greenbits.ui.BuildConfig;
import com.greenaddress.greenbits.ui.R;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.protocols.payments.PaymentProtocol;
import org.bitcoinj.protocols.payments.PaymentProtocolException;
import org.bitcoinj.protocols.payments.PaymentSession;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;
import org.bitcoinj.utils.MonetaryFormat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class GaService extends Service implements INotificationHandler {
    private static final String TAG = GaService.class.getSimpleName();
    public static final boolean IS_ELEMENTS = Network.NETWORK == ElementsRegTestParams.get();

    private enum ConnState {
        OFFLINE, DISCONNECTED, CONNECTING, CONNECTED, LOGGINGIN, LOGGEDIN
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

    private final ListeningExecutorService mExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(3));
    public ListenableFuture<Void> onConnected;
    private final SparseArray<GaObservable> mBalanceObservables = new SparseArray<>();
    private final GaObservable mNewTxObservable = new GaObservable();
    private final GaObservable mVerifiedTxObservable = new GaObservable();
    private String mSignUpMnemonic;
    private Bitmap mSignUpQRCode;
    private int mCurrentBlock; // FIXME: Pass current block height back in login data.

    private boolean mAutoReconnect = true;
    // cache
    private ListenableFuture<List<List<String>>> mCurrencyExchangePairs;

    private final SparseArray<Coin> mCoinBalances = new SparseArray<>();

    private Double mFiatRate;
    private String mFiatCurrency;
    private String mFiatExchange;
    private JSONMap mLimitsData;
    private ArrayList<Map<String, Object>> mSubAccounts;
    private String mReceivingId;
    private Coin mDustThreshold = Coin.valueOf(546); // Per 0.13.0, updated on login
    private Coin mMinFeeRate = Coin.valueOf(1000); // Per 0.12.0, updated on login
    private Map<?, ?> mTwoFactorConfig;
    private final GaObservable mTwoFactorConfigObservable = new GaObservable();
    private String mDeviceId;
    private boolean mUserCancelledPINEntry;
    public byte[] mAssetId;
    private String mAssetSymbol;
    private MonetaryFormat mAssetFormat;

    private final SPV mSPV = new SPV(this);

    private WalletClient mClient;

    public ListeningExecutorService getExecutor() {
        return mExecutor;
    }

    public ISigningWallet getSigningWallet() {
        return mClient.getSigningWallet();
    }

    public String getBitcoinUnit() {
        final Object unit = getUserConfig("unit");
        return unit == null ? "bits" : (String) unit;
    }

    public void setDefaultTransactionPriority(final int priority) {
        setUserConfig("required_num_blocks", priority, true);
    }

    public int getDefaultTransactionPriority() {
        try {
            return (int) getUserConfig("required_num_blocks");
        } catch (final Exception e) {
            return 6; // Not logged in/not set, default to Normal/6 confs
        }
    }

    public final boolean isRBFEnabled() {
        final Object rbf_optin = getUserConfig("replace_by_fee");
        return rbf_optin != null && (Boolean) rbf_optin;
    }

    public boolean isValidFeeRate(final String feeRate) {
        try {
            return feeRate.isEmpty() || Double.valueOf(feeRate) >= mMinFeeRate.longValue();
        } catch (final Exception e) {
            return false;
        }
    }

    public int getAutoLogoutMinutes() {
        try {
            final int timeout = (int)getUserConfig("altimeout");
            return timeout < 1 ? 1 : timeout;
        } catch (final Exception e) {
            return 5; // Not logged in/not set, default to 5 min
        }
    }

    public File getSPVChainFile() {
        final String dirName = "blockstore_" + mReceivingId;
        return new File(getDir(dirName, Context.MODE_PRIVATE), "blockchain.spvchain");
    }

    private void getAvailableTwoFactorMethods() {
        Futures.addCallback(mClient.getTwoFactorConfig(), new FutureCallback<Map<?, ?>>() {
            @Override
            public void onSuccess(final Map<?, ?> result) {
                mTwoFactorConfig = result;
                mTwoFactorConfigObservable.doNotify();
            }

            @Override
            public void onFailure(final Throwable t) {
                t.printStackTrace();
            }
        }, mExecutor);
    }

    public boolean getUserCancelledPINEntry() {
        return mUserCancelledPINEntry;
    }

    public void setUserCancelledPINEntry(final boolean value) {
        mUserCancelledPINEntry = value;
    }

    private void reloadSettings() {
        mClient.setProxy(getProxyHost(), getProxyPort());
        mClient.setTorEnabled(getTorEnabled());
    }

    private void reconnect() {
        reloadSettings();
        Log.i(TAG, "Submitting reconnect after " + mReconnectDelay);
        onConnected = mClient.connect();
        mState.transitionTo(ConnState.CONNECTING);

        Futures.addCallback(onConnected, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                mState.transitionTo(ConnState.CONNECTED);
                Log.i(TAG, "Success CONNECTED callback");
                if (mState.isForcedOff())
                    return;
                try {
                    if (isWatchOnly())
                        loginImpl(mClient.watchOnlylogin(mClient.getWatchOnlyUsername(), mClient.getWatchOnlyPassword()));
                    else if (mClient.getSigningWallet() != null)
                        loginImpl(mClient.login(mClient.getSigningWallet(), mDeviceId, null));
                } catch (final Exception e) {
                    e.printStackTrace();
                    this.onFailure(e);
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                t.printStackTrace();
                Log.i(TAG, "Failure throwable callback " + t.toString());
                mState.transitionTo(ConnState.DISCONNECTED);
                scheduleReconnect();
            }
        }, mExecutor);
    }

    private static String getBech32Prefix() {
        if (Network.NETWORK == MainNetParams.get())
            return "bc";
        if (Network.NETWORK == TestNet3Params.get())
            return "tb";
        return "bcrt";
    }

    public static byte[] decodeBech32Address(final String address) {
        try {
            final byte decoded[] = Wally.addr_segwit_to_bytes(address, getBech32Prefix(), 0);
            // Valid native segwit addresses are v0 p2wphk or v0 p2wsh, i.e.
            // 0 PUSH(hash160 or sha256)
            if ((decoded.length == Wally.WALLY_SCRIPTPUBKEY_P2WPKH_LEN ||
                 decoded.length == Wally.WALLY_SCRIPTPUBKEY_P2WSH_LEN) &&
                decoded[0] == 0 && decoded[1] == decoded.length - 2)
                return decoded;
        } catch (final Exception e) {
            // Fall through
        }
        return null;
    }

    public static boolean isValidAddress(final String address) {
        try {
            if (IS_ELEMENTS)
                ConfidentialAddress.fromBase58(Network.NETWORK, address);
            else
                Address.fromBase58(Network.NETWORK, address);
            return true;
        } catch (final AddressFormatException e) {
            if (IS_ELEMENTS)
                return false; // No bech32 for elements yet
            return decodeBech32Address(address) != null;
        }
    }

    public boolean isWatchOnly() {
        return mClient.isWatchOnly();
    }

    public boolean isSegwitUnconfirmed() {
        return mClient.isSegwitUnconfirmed();
    }

    public boolean isSegwitEnabled() {
        return mClient.isSegwitEnabled();
    }

    // Sugar for fetching/editing preferences
    public SharedPreferences cfg() { return PreferenceManager.getDefaultSharedPreferences(this); }
    public SharedPreferences cfg(final String name) { return getSharedPreferences(name, MODE_PRIVATE); }
    public SharedPreferences.Editor cfgEdit(final String name) { return cfg(name).edit(); }
    public SharedPreferences cfgIn(final String name) { return cfg(name + mReceivingId); }
    public SharedPreferences.Editor cfgInEdit(final String name) { return cfgIn(name).edit(); }

    // User config is stored on the server (unlike preferences which are local)
    public Object getUserConfig(final String key) {
        return mClient.getUserConfig(key);
    }

    public String getProxyHost() { return cfg().getString("proxy_host", ""); }
    public String getProxyPort() { return cfg().getString("proxy_port", ""); }
    private boolean getTorEnabled() { return cfg().getBoolean("tor_enabled", false); }
    public boolean isSegwitUnlocked() { return !cfgIn("CONFIG").getBoolean("sw_locked", false); }
    private void setSegwitLocked() { cfgInEdit("CONFIG").putBoolean("sw_locked", true).apply(); }
    public boolean isProxyEnabled() { return !TextUtils.isEmpty(getProxyHost()) && !TextUtils.isEmpty(getProxyPort()); }
    public int getCurrentSubAccount() { return cfgIn("CONFIG").getInt("current_subaccount", 0); }
    public void setCurrentSubAccount(final int subAccount) { cfgInEdit("CONFIG").putInt("current_subaccount", subAccount).apply(); }
    public boolean showBalanceInTitle() { return cfg().getBoolean("show_balance_in_title", false); }

    // SPV
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

    public static boolean isBadAddress(final String s) {
        if (s.isEmpty())
            return false;

        try {
            new URI("btc://" + s);
            return false;
        } catch (final URISyntaxException e) {
        }

        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Uncomment to test slow service creation
        // android.os.SystemClock.sleep(10000);

        mTimerExecutor = new ScheduledThreadPoolExecutor(1);
        mTimerExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);

        mDeviceId = cfg("service").getString("device_id", null);
        if (mDeviceId == null) {
            // Generate a unique device id
            mDeviceId = UUID.randomUUID().toString();
            cfgEdit("service").putString("device_id", mDeviceId).apply();
        }

        mClient = new WalletClient(this, mExecutor);
    }

    @Override
    public void onNewBlock(final int blockHeight) {
        Log.i(TAG, "onNewBlock");
        setCurrentBlock(blockHeight);
        mSPV.onNewBlock(blockHeight);
        mNewTxObservable.doNotify();
    }

    @Override
    public void onNewTransaction(final int[] affectedSubAccounts) {
        Log.i(TAG, "onNewTransaction");
        mSPV.updateUnspentOutputs();
        mNewTxObservable.doNotify();
        for (final int subAccount : affectedSubAccounts)
            updateBalance(subAccount);
    }

    @Override
    public void onConnectionClosed(final int code) {
        HDKey.resetCache(null);
        HDClientKey.resetCache(null, null);

        // Server error codes FIXME: These should be in a class somewhere
        // 4000 (concurrentLoginOnDifferentDeviceId) && 4001 (concurrentLoginOnSameDeviceId!)
        // 1000 NORMAL_CLOSE
        // 1006 SERVER_RESTART
        mState.setForcedLogout(code == 4000);
        mState.transitionTo(ConnState.DISCONNECTED);

        if (getNetworkInfo() == null) {
            mState.transitionTo(ConnState.OFFLINE);
            return;
        }

        Log.i(TAG, "onConnectionClosed code=" + String.valueOf(code));
        // FIXME: some callback to UI so you see what's happening.
        mReconnectDelay = 0;
        if (mAutoReconnect)
            reconnect();
    }

    public static byte[] createOutScript(final int subAccount, final Integer pointer,
                                         final byte[] backupPubkey, final byte[] backupChaincode) {
        final List<ECKey> pubkeys = new ArrayList<>();
        pubkeys.add(HDKey.getGAPublicKeys(subAccount, pointer)[1]);
        pubkeys.add(HDClientKey.getMyPublicKey(subAccount, pointer));
        if (backupPubkey != null && backupChaincode != null)
            pubkeys.add(HDKey.getRecoveryKeys(backupChaincode, backupPubkey, pointer)[1]);
        return Script.createMultiSigOutputScript(2, pubkeys);
    }

    public byte[] createOutScript(final int subAccount, final Integer pointer) {
        byte[] backupPubkey = null;
        byte[] backupChaincode = null;
        final Map<String, Object> m = findSubaccountByType(subAccount, "2of3");
        if (m != null) {
            backupPubkey = Wally.hex_to_bytes((String) m.get("2of3_backup_pubkey"));
            backupChaincode = Wally.hex_to_bytes((String) m.get("2of3_backup_chaincode"));
        }
        return createOutScript(subAccount, pointer, backupPubkey, backupChaincode);
    }

    private ListenableFuture<Boolean> verifyP2SHSpendableBy(final Script scriptHash, final int subAccount, final Integer pointer) {
        if (!scriptHash.isPayToScriptHash())
            return Futures.immediateFuture(false);
        final byte[] gotP2SH = scriptHash.getPubKeyHash();

        return mExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                final byte[] multisig = createOutScript(subAccount, pointer);
                if (isSegwitEnabled() &&
                    Arrays.equals(gotP2SH, Wally.hash160(getSegWitScript(multisig))))
                    return true;
                return Arrays.equals(gotP2SH, Wally.hash160(multisig));
            }
        });
    }

    public ListenableFuture<Boolean> verifySpendableBy(final TransactionOutput txOutput, final int subAccount, final Integer pointer) {
        return verifyP2SHSpendableBy(txOutput.getScriptPubKey(), subAccount, pointer);
    }

    public String getWatchOnlyUsername() throws Exception {
        return mClient.getWatchOnlyUsername();
    }

    public void registerWatchOnly(final String username, final String password) throws Exception {
        mClient.registerWatchOnly(username, password);
    }

    private ListenableFuture<LoginData> loginImpl(final ListenableFuture<LoginData> loginFn) {
        mState.transitionTo(ConnState.LOGGINGIN);

        // Chain the login and post-login processing together, so any
        // callbacks added by the caller are executed only once our post
        // login processing is completed.
        final ListenableFuture fn = Futures.transform(loginFn, new Function<LoginData, LoginData>() {
            @Override
            public LoginData apply(final LoginData loginData) {
                onPostLogin(loginData);
                return loginData;
            }
        });

        // Add a callback to set our state back to connected if an error
        // occurs. Ideally we could add this in transform(), but it doesnt
        // seem possible. So there is a delay before our state is updated.
        Futures.addCallback(fn, new FutureCallback<LoginData>() {
            @Override
            public void onSuccess(final LoginData result) { }

            @Override
            public void onFailure(final Throwable t) {
                t.printStackTrace();
                mState.transitionTo(ConnState.CONNECTED);
            }
        }, mExecutor);

        return fn;
    }

    private void onPostLogin(final LoginData loginData) {

        // Uncomment to test slow login post processing
        // android.os.SystemClock.sleep(10000);
        Log.d(TAG, "Success LOGIN callback");

        // FIXME: Why are we copying these? If we need them when not logged in,
        // we should just copy the whole loginData instance
        mFiatCurrency = loginData.get("currency");
        mFiatExchange = loginData.get("exchange");
        mSubAccounts = loginData.mSubAccounts;
        mReceivingId = loginData.get("receiving_id");
        mLimitsData = new JSONMap((Map) loginData.get("limits"));

        if (loginData.mRawData.containsKey("min_fee"))
            mMinFeeRate = Coin.valueOf((int) loginData.get("min_fee"));
        if (loginData.mRawData.containsKey("dust"))
            mDustThreshold = Coin.valueOf((int) loginData.get("dust"));

        HDKey.resetCache(loginData.mGaitPath);

        mBalanceObservables.put(0, new GaObservable());
        if (IS_ELEMENTS) {
            // ignore login data from elements since it doesn't include confidential values
            updateBalance(0);
            for (final Map<String, Object> data : mSubAccounts)
                updateBalance((Integer) data.get("pointer"));
            // fetch the asset id and symbol for elements:
            int maxId = 0;
            final Map<String, Integer> assetIds = (Map<String, Integer>) loginData.mRawData.get("asset_ids");
            final Map<String, String> assetSymbols = (Map<String, String>) loginData.mRawData.get("asset_symbols");
            for (final String assetIdHex : assetIds.keySet()) {
                // find largest asset id that has a symbol set:
                if (assetIds.get(assetIdHex) > maxId && assetSymbols.get(assetIdHex) != null) {
                    maxId = assetIds.get(assetIdHex);
                    mAssetId = Wally.hex_to_bytes(assetIdHex);
                }
            }
            mAssetSymbol = assetSymbols.get(Wally.hex_from_bytes(mAssetId));
            final int decimalPlaces = ((Map<String, Integer>) loginData.mRawData.get("asset_decimal_places")).get(
                    Wally.hex_from_bytes(mAssetId)
            );
            mAssetFormat = new MonetaryFormat().shift(8 - decimalPlaces).minDecimals(decimalPlaces).noCode();
        } else {
            updateBalance(0, loginData.mRawData);
            for (final Map<String, Object> data : mSubAccounts) {
                final int pointer = ((Integer) data.get("pointer"));
                mBalanceObservables.put(pointer, new GaObservable());
                updateBalance(pointer, data);
            }
        }

        if (!isWatchOnly()) {
            getAvailableTwoFactorMethods();
            mSPV.startAsync();
        }
        mState.transitionTo(ConnState.LOGGEDIN);
    }

    public ListenableFuture<LoginData> login(final ISigningWallet signingWallet) {
        return loginImpl(mClient.login(signingWallet, mDeviceId, null));
    }

    public ListenableFuture<LoginData> watchOnlyLogin(final String username, final String password) {
        return loginImpl(mClient.watchOnlylogin(username, password));
    }

    public void disableWatchOnly() throws Exception {
        mClient.disableWatchOnly();
    }

    public ListenableFuture<LoginData> login(final String mnemonic) {
        return login(new SWWallet(mnemonic), mnemonic);
    }

    private ListenableFuture<LoginData> login(final ISigningWallet signingWallet, final String mnemonic) {
        return loginImpl(mClient.login(signingWallet, mDeviceId, mnemonic));
    }

    public ListenableFuture<LoginData> signup(final ISigningWallet signingWallet,
                                              final String mnemonic, final String userAgent,
                                              final byte[] pubkey, final byte[] chaincode) {
        mState.transitionTo(ConnState.LOGGINGIN);

        return mExecutor.submit(new Callable<LoginData>() {
                   @Override
                   public LoginData call() throws Exception {
                       try {
                           mClient.registerUser(signingWallet, mnemonic, userAgent,
                                                pubkey, chaincode,
                                                mDeviceId);
                           onPostLogin(mClient.getLoginData());
                           return mClient.getLoginData();
                       } catch (final Exception e) {
                           e.printStackTrace();
                           mState.transitionTo(ConnState.CONNECTED);
                           throw e;
                       }
                   }
               });
    }

    public ListenableFuture<LoginData> signup(final String mnemonic) {
        final SWWallet sw = new SWWallet(mnemonic);
        return signup(sw, mnemonic, /*agent*/ null, sw.getMasterKey().getPubKey(),
                      sw.getMasterKey().getChainCode());
    }

    public String getMnemonic() {
        return mClient.getMnemonic();
    }

    public LoginData getLoginData() {
        return mClient.getLoginData();
    }

    public JSONMap getFeeEstimates() {
        return mClient.getFeeEstimates();
    }

    // Get the fee rate to confirm at the next blockNum blocks in BTC/1000 bytes
    public Double getFeeRate(final int blockNum) {
        final JSONMap m = new JSONMap((Map) getFeeEstimates().get(Integer.toString(blockNum)));
        return m.getDouble("feerate");
    }

    public Integer getFeeBlocks(final int blockNum) {
        final JSONMap m = new JSONMap((Map) getFeeEstimates().get(Integer.toString(blockNum)));
        return m.getInt("blocks");
    }

    public Coin getMinFeeRate() {
        return mMinFeeRate;
    }

    public Coin getDustThreshold() {
        return mDustThreshold;
    }

    public void disconnect(final boolean autoReconnect) {
        mAutoReconnect = autoReconnect;
        mSPV.stopSyncAsync();
        final int size = mBalanceObservables.size();
        for(int i = 0; i < size; ++i) {
            final int key = mBalanceObservables.keyAt(i);
            mBalanceObservables.get(key).deleteObservers();
        }
        mClient.disconnect();
        mState.transitionTo(ConnState.DISCONNECTED);
    }

    public void updateBalance(final int subAccount) {
        Futures.addCallback(getSubaccountBalance(subAccount), new FutureCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(final Map<String, Object> data) {
                updateBalance(subAccount, data);
            }

            @Override
            public void onFailure(final Throwable t) { }
        }, mExecutor);
    }

    private void updateBalance(final int subAccount, final Map<String, Object> rawData) {
        final JSONMap data = new JSONMap(rawData);
        final String fiatCurrency = data.getString("fiat_currency");
        if (!TextUtils.isEmpty(fiatCurrency))
            if (mFiatCurrency == null || !fiatCurrency.equals(mFiatCurrency)) {
                mFiatCurrency = fiatCurrency;
                resetFiatSpendingLimits();
            }

        mCoinBalances.put(subAccount, data.getCoin("satoshi"));

        try {
            mFiatRate = data.getDouble("fiat_exchange");
        } catch (final java.lang.NumberFormatException e) {
            Log.d(TAG, "No exchange rate returned by server");
        }

        fireBalanceChanged(subAccount);
    }

    public ListenableFuture<Map<String, Object>> getSubaccountBalance(final int subAccount) {
        if (IS_ELEMENTS)
            return getBalanceFromUtxo(subAccount);
        return mClient.getSubaccountBalance(subAccount);
    }

    private ListenableFuture<Map<String,Object>> getBalanceFromUtxo(final int subAccount) {
        final boolean filterAsset = true;
        return Futures.transform(getAllUnspentOutputs(0, subAccount, filterAsset),
                                 new Function<List<JSONMap>, Map<String, Object>>() {
            @Override
            public Map<String, Object> apply(final List<JSONMap> utxos) {
                final Map <String, Object> res = new HashMap<>();
                res.put("fiat_currency", "?");
                res.put("fiat_exchange", "1");
                res.put("fiat_value", "0");

                BigInteger finalValue = BigInteger.ZERO;
                for (final JSONMap utxo : utxos)
                    finalValue = finalValue.add(utxo.getBigInteger("value"));

                res.put("satoshi", String.valueOf(finalValue));
                return res;
            }
        });
    }

    public void fireBalanceChanged(final int subAccount) {
        if (getCoinBalance(subAccount) == null) {
            // Called from addUtxoToValues before balance is fetched
            return;
        }
        mBalanceObservables.get(subAccount).doNotify();
    }

    public void setPricingSource(final String currency, final String exchange) {
        Futures.transform(mClient.setPricingSource(currency, exchange), new Function<Boolean, Boolean>() {
            @Override
            public Boolean apply(final Boolean input) {
                mFiatCurrency = currency;
                mFiatExchange = exchange;
                resetFiatSpendingLimits();
                return input;
            }
        });
    }

    public ListenableFuture<Map<String, Object>> getMyTransactions(final int subAccount) {
        return mExecutor.submit(new Callable<Map<String, Object>>() {
            @Override
            public Map<String, Object> call() throws Exception {
                final Map<String, Object> result = mClient.getMyTransactions(null, subAccount);
                setCurrentBlock((Integer) result.get("cur_block"));
                final List<JSONMap> txs = JSONMap.fromList((List) result.get("list"));
                for (final JSONMap tx : txs)
                    tx.mData.put("eps", unblindValues(JSONMap.fromList((List) tx.get("eps")), false, false));
                result.put("list", txs);
                return result;
            }
        });
    }

    public ListenableFuture<Void> setPin(final String mnemonic, final String pin) {
        return mExecutor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                final PinData pinData = mClient.setPin(mnemonic, pin, "default");
                // As this is a new PIN, save it to config
                final String encrypted = Base64.encodeToString(pinData.mSalt, Base64.NO_WRAP) + ';' +
                                         Base64.encodeToString(pinData.mEncryptedData, Base64.NO_WRAP);
                cfgEdit("pin").putString("ident", pinData.mPinIdentifier)
                              .putInt("counter", 0)
                              .putString("encrypted", encrypted)
                              .apply();
               return null;
            }
        });
    }

    public ListenableFuture<LoginData> pinLogin(final String pin) throws Exception {
        final String pinIdentifier = cfg("pin").getString("ident", null);
        final byte[] password = mClient.getPinPassword(pinIdentifier, pin);
        final String[] split = cfg("pin").getString("encrypted", null).split(";");
        final byte[] salt = split[0].getBytes();
        final byte[] encryptedData = Base64.decode(split[1], Base64.NO_WRAP);
        final PinData pinData = PinData.fromEncrypted(pinIdentifier, salt, encryptedData, password);
        final DeterministicKey master = HDKey.createMasterKeyFromSeed(pinData.mSeed);
        return login(new SWWallet(master), pinData.mMnemonic);
    }

    public ListenableFuture<List<byte[]>> signTransaction(final PreparedTransaction ptx) {
        return mClient.signTransaction(mClient.getSigningWallet(), ptx);
    }

    public List<byte[]> signTransaction(final Transaction tx, final PreparedTransaction ptx, final List<Output> prevOuts) {
        return mClient.getSigningWallet().signTransaction(tx, ptx, prevOuts);
    }

    public ListenableFuture<String>
    sendTransaction(final List<byte[]> sigs, final Object twoFacData) {
        // FIXME: The server should return the full limits including is_fiat from send_tx
        return Futures.transform(mClient.sendTransaction(sigs, twoFacData),
                                 new Function<String, String>() {
                   @Override
                   public String apply(final String txHash) {
                       try {
                           mLimitsData = mClient.getSpendingLimits();
                       } catch (final Exception e) {
                           // We don't know what the new limit is so nuke it
                           mLimitsData.mData.put("total", 0);
                           e.printStackTrace();
                       }
                       return txHash;
                   }
        }, mExecutor);
    }

    public ListenableFuture<Pair<String, String>>
    sendRawTransaction(final Transaction tx, final Map<String, Object> twoFacData,
                       final JSONMap privateData, final boolean returnTx) {
        return Futures.transform(mClient.sendRawTransaction(tx, twoFacData, privateData, returnTx),
                                 new Function<Map<String, Object>, Pair<String, String>>() {
                   @Override
                   public Pair<String, String> apply(final Map<String, Object> ret) {
                       // FIXME: Server should return the full limits including is_fiat
                       if (ret.get("new_limit") != null)
                           mLimitsData.mData.put("total", ret.get("new_limit"));
                       return new Pair<>(ret.get("txhash").toString(),
                                         ret.get("tx_hex").toString());
                   }
        }, mExecutor);
    }

    private List<JSONMap> unblindValues(final List<JSONMap> values, final boolean filterAsset,
                                        final boolean isUtxo) {
        if (!IS_ELEMENTS)
            return values;

        final List<JSONMap> result = new ArrayList<>(values.size());
        final List<byte[]> unblinded = new ArrayList<>(3);
        for (final JSONMap v : values) {
            if ((isUtxo && v.get("value") == null) ||
                (!isUtxo && v.get("commitment") != null)) {
                // Blinded value: Unblind it
                final Long value;
                unblinded.clear();
                value = Wally.asset_unblind(v.getBytes("nonce_commitment"),
                                            getBlindingPrivKey(v),
                                            v.getBytes("range_proof"),
                                            v.getBytes("commitment"),
                                            null,
                                            v.getBytes("asset_tag"),
                                            unblinded);
                final byte[] assetId = unblinded.get(0);
                if (!Arrays.equals(assetId, mAssetId)) {
                    if (filterAsset)
                        continue; // Ignore
                    if (!isUtxo)
                        v.mData.put("is_relevant", false); // Mark irrelevant
                }
                v.mData.put("confidential", true);
                v.mData.put("value", UnsignedLongs.toString(value));
                if (isUtxo) {
                    v.putBytes("assetId", assetId);
                    v.putBytes("abf", unblinded.get(1));
                    v.putBytes("vbf", unblinded.get(2));
                }
            }
            result.add(v);
        }
        return result;
    }

    public ListenableFuture<List<JSONMap>> getAllUnspentOutputs(final int confs, final Integer subAccount,
                                                                final boolean filterAsset) {
        return Futures.transform(mClient.getAllUnspentOutputs(confs, subAccount),
                                 new Function<List<JSONMap>, List<JSONMap>>() {
            @Override
            public List<JSONMap> apply(final List<JSONMap> utxos) {
                return unblindValues(utxos, filterAsset, true);
            }
        });
    }

    public ListenableFuture<Transaction> getRawUnspentOutput(final Sha256Hash txHash) {
        return mClient.getRawUnspentOutput(txHash);
    }

    public String getRawOutputHex(final Sha256Hash txHash) throws Exception {
        return mClient.getRawOutputHex(txHash);
    }

    public ListenableFuture<Boolean> changeMemo(final String txHashHex, final String memo, final String memoType) {
        return mClient.changeMemo(txHashHex, memo, memoType);
    }

    // FIXME: Put this and other script stuff in wally
    public static byte[] getSegWitScript(final byte[] input) {
        final ByteArrayOutputStream bits = new ByteArrayOutputStream();
        bits.write(0);
        try {
            Script.writeBytes(bits, Wally.sha256(input));
        } catch (final IOException e) {
            throw new RuntimeException(e);  // cannot happen
        }
        return bits.toByteArray();
    }

    public JSONMap getNewAddress(final int subAccount) {
        final boolean userSegwit = isSegwitEnabled();
        if (userSegwit && isSegwitUnlocked())
            setSegwitLocked(); // Locally store that we have generated a SW address
        return mClient.getNewAddress(subAccount, userSegwit ? "p2wsh" : "p2sh");
    }

    private void storeCachedAddress(final int subAccount, final byte[] salt, final byte[] encryptedAddress) {
        final String configKey = "next_addr_" + subAccount;
        cfgInEdit(configKey).putString("salt", Wally.hex_from_bytes(salt))
                            .putString("encrypted", Wally.hex_from_bytes(encryptedAddress))
                            .apply();
    }

    private void cacheAddress(final int subAccount, final JSONMap address) {
        final byte[] password = getSigningWallet().getLocalEncryptionPassword();
        final byte[] salt = CryptoHelper.randomBytes(16);
        storeCachedAddress(subAccount, salt, CryptoHelper.encryptJSON(address, password, salt));
    }

    private void uncacheAddress(final int subAccount) {
        storeCachedAddress(subAccount, new byte[] { 0 }, new byte[] { 0 });
    }

    private JSONMap getCachedAddress(final int subAccount) {
        if (isWatchOnly())
            return null;

        final String configKey = "next_addr_" + subAccount;
        final String saltHex = cfgIn(configKey).getString("salt", null);
        if (saltHex == null || saltHex.length() != 32)
            return null;
        final String encryptedAddressHex = cfgIn(configKey).getString("encrypted", null);
        JSONMap json;
        try {
            json = CryptoHelper.decryptJSON(Wally.hex_to_bytes(encryptedAddressHex),
                                            getSigningWallet().getLocalEncryptionPassword(),
                                            Wally.hex_to_bytes(saltHex));
            final String expectedType = isSegwitEnabled() ? "p2wsh" : "p2sh";
            if (!json.getString("addr_type").equals(expectedType))
                json = null; // User has enabled SW, cached address is non-SW
        } catch (final RuntimeException e) {
            e.printStackTrace();
            json = null;
        }
        uncacheAddress(subAccount);
        return json;
     }

    private ListenableFuture<JSONMap> getNewAddressAsync(final int subAccount, final boolean cacheResult) {
        return mExecutor.submit(new Callable<JSONMap>() {
            @Override
            public JSONMap call() {
                final JSONMap address = getNewAddress(subAccount);
                if (cacheResult)
                    cacheAddress(subAccount, address);
                return address;
            }
        });
    }

    public ListenableFuture<QrBitmap> getNewAddressBitmap(final int subAccount,
                                                          final Callable<Void> waitFn,
                                                          final Long amount) {
        final Function<String, QrBitmap> generateQrBitmap = new Function<String, QrBitmap>() {
            @Override
            public QrBitmap apply(final String address) {
                final String uri;
                if (amount != null)
                    uri = "bitcoin:" + address + "?amount=" + Coin.valueOf(amount).toPlainString();
                else
                    uri = address;
                return new QrBitmap(uri, 0 /* transparent background */);
            }
        };
        return Futures.transform(getNewAddress(subAccount, waitFn), generateQrBitmap, mExecutor);
    }

    /**
     * Generate new address to the selected sub account, bitcoin or elements
     * @param subAccount sub account ID
     * @param waitFn eventually callback to execute (e.g. waiting popup)
     * @return the address in string format
     */
    public ListenableFuture<String> getNewAddress(final int subAccount,
                                                  final Callable<Void> waitFn) {
        // Fetch any cached address
        final JSONMap cachedAddress = getCachedAddress(subAccount);

        // Use either the cached address or a new address
        final ListenableFuture<JSONMap> addrFn;
        if (cachedAddress != null)
            addrFn = Futures.immediateFuture(cachedAddress);
        else {
            try {
                if (waitFn != null)
                    waitFn.call();
            } catch (final Exception e) {
            }
            addrFn = getNewAddressAsync(subAccount, false);
        }

        // Fetch and cache another address in the background
        if (!isWatchOnly())
            getNewAddressAsync(subAccount, true);

        // Convert the address into a bitmap and return it
        final AsyncFunction<JSONMap, String> verifyAddress = new AsyncFunction<JSONMap, String>() {
            @Override
            public ListenableFuture<String> apply(final JSONMap input) {
                if (input == null)
                    throw new IllegalArgumentException("Failed to generate a new address");

                final Integer pointer = input.getInt("pointer");
                final byte[] script = input.getBytes("script");
                final byte[] scriptHash;
                final String addrType = input.get("addr_type");
                if (addrType.equals("p2wsh"))
                    scriptHash = Wally.hash160(getSegWitScript(script));
                else if (addrType.equals("p2sh"))
                    scriptHash = Wally.hash160(script);
                else
                    throw new IllegalArgumentException("Unknown address type " + addrType);

                final ListenableFuture<Boolean> verify;
                if (isWatchOnly())
                    verify = Futures.immediateFuture(true);
                else {
                    final Script sc;
                    sc = ScriptBuilder.createP2SHOutputScript(scriptHash);
                    verify = verifyP2SHSpendableBy(sc, subAccount, pointer);
                }

                return Futures.transform(verify,
                        new Function<Boolean, String>() {
                    @Override
                    public String apply(final Boolean isValid) {
                        if (!isValid)
                            throw new IllegalArgumentException("Address validation failed");

                        final String address;
                        if (IS_ELEMENTS) {
                            final byte[] pubKey = getBlindingPubKey(subAccount, pointer);
                            address = ConfidentialAddress.fromP2SHHash(Network.NETWORK, scriptHash, pubKey).toString();
                        } else
                            address = Address.fromP2SHHash(Network.NETWORK, scriptHash).toString();
                        return address;
                    }
                });
            }
        };
        return Futures.transformAsync(addrFn, verifyAddress, mExecutor);
    }

    public byte[] getBlindingPubKey(final int subAccount, final int pointer) {
        return Wally.ec_public_key_from_private_key(getBlindingPrivKey(subAccount, pointer));
    }

    private byte[] getBlindingPrivKey(final int subAccount, final int pointer) {
        final byte[] privKey = new byte[32];
        // TODO derive real blinding key
        for (int i = 0; i < 32; ++i)
            privKey[i] = 1;
        return privKey;
    }

    // Fetch a blinding key from a utxo (or endpoint)
    private byte[] getBlindingPrivKey(final JSONMap utxo) {
        return getBlindingPrivKey(utxo.getInt("subaccount", 0),
                                  utxo.getInt(utxo.getKey("pubkey_pointer", "pointer")));
    }

    public ListenableFuture<List<List<String>>> getCurrencyExchangePairs() {
        if (mCurrencyExchangePairs == null) {
            mCurrencyExchangePairs = Futures.transform(mClient.getAvailableCurrencies(), new Function<Map<?, ?>, List<List<String>>>() {
                @Override
                public List<List<String>> apply(final Map<?, ?> result) {
                    final Map<String, ArrayList<String>> per_exchange = (Map) result.get("per_exchange");
                    final List<List<String>> ret = new LinkedList<>();
                    for (final String exchange : per_exchange.keySet()) {
                        for (final String currency : per_exchange.get(exchange))
                            ret.add(Lists.newArrayList(currency, exchange));
                    }
                    Collections.sort(ret, new Comparator<List<String>>() {
                        @Override
                        public int compare(final List<String> lhs, final List<String> rhs) {
                            return lhs.get(0).compareTo(rhs.get(0));
                        }
                    });
                    return ret;
                }
            }, mExecutor);
        }
        return mCurrencyExchangePairs;
    }

    public void resetSignUp() {
        mSignUpMnemonic = null;
        if (mSignUpQRCode != null)
            mSignUpQRCode.recycle();
        mSignUpQRCode = null;
    }

    public String getSignUpMnemonic() {
        if (mSignUpMnemonic == null)
            mSignUpMnemonic = CryptoHelper.mnemonic_from_bytes(CryptoHelper.randomBytes(32));
        return mSignUpMnemonic;
    }

    public Bitmap getSignUpQRCode() {
        if (mSignUpQRCode == null)
            mSignUpQRCode = new QrBitmap(getSignUpMnemonic(), Color.WHITE).getQRCode();
       return mSignUpQRCode;
    }

    public void addBalanceObserver(final int subAccount, final Observer o) {
        mBalanceObservables.get(subAccount).addObserver(o);
    }

    public void deleteBalanceObserver(final int subAccount, final Observer o) {
        mBalanceObservables.get(subAccount).deleteObserver(o);
    }

    public void addNewTxObserver(final Observer o) {
        mNewTxObservable.addObserver(o);
    }

    public void deleteNewTxObserver(final Observer o) {
        mNewTxObservable.deleteObserver(o);
    }

    public void addVerifiedTxObserver(final Observer o) {
        mVerifiedTxObservable.addObserver(o);
    }

    public void deleteVerifiedTxObserver(final Observer o) {
        mVerifiedTxObservable.deleteObserver(o);
    }

    public void addTwoFactorObserver(final Observer o) {
        mTwoFactorConfigObservable.addObserver(o);
    }

    public void deleteTwoFactorObserver(final Observer o) {
        mTwoFactorConfigObservable.deleteObserver(o);
    }

    public void notifyObservers(final Sha256Hash txHash) {
        // FIXME: later spent outputs can be purged
        mSPV.addUtxoToValues(txHash, true /* updateVerified */);
        mVerifiedTxObservable.doNotify();
    }

    public Coin getCoinBalance(final int subAccount) {
        return mCoinBalances.get(subAccount);
    }

    public String getFiatBalance(final int subAccount) {
        return coinToFiat(getCoinBalance(subAccount));
    }

    public String coinToFiat(final Coin btcValue) {
        if (!hasFiatRate())
            return "N/A";
        Fiat fiatValue = getFiatRate().coinToFiat(btcValue);
        // strip extra decimals (over 2 places) because that's what the old JS client does
        fiatValue = fiatValue.subtract(fiatValue.divideAndRemainder((long) Math.pow(10, Fiat.SMALLEST_UNIT_EXPONENT - 2))[1]);
        return MonetaryFormat.FIAT.minDecimals(2).noCode().format(fiatValue).toString();
    }

    public boolean hasFiatRate() {
        return mFiatRate != null;
    }

    public ExchangeRate getFiatRate() {
        final long rate = new BigDecimal(mFiatRate).movePointRight(Fiat.SMALLEST_UNIT_EXPONENT)
                                                   .toBigInteger().longValue();
        return new ExchangeRate(Fiat.valueOf("???", rate));
    }

    public String getFiatCurrency() {
        return mFiatCurrency;
    }

    public String getAssetSymbol() {
        return mAssetSymbol;
    }

    public MonetaryFormat getAssetFormat() {
        return mAssetFormat;
    }

    public String getFiatExchange() {
        return mFiatExchange;
    }

    public ArrayList<Map<String, Object>> getSubaccounts() {
        return mSubAccounts;
    }

    public boolean haveSubaccounts() {
        return mSubAccounts != null && !mSubAccounts.isEmpty();
    }

    public Map<String, Object> findSubaccountByType(final Integer subAccount, final String type) {
        if (haveSubaccounts())
            for (final Map<String, Object> ret : mSubAccounts)
                if (ret.get("pointer").equals(subAccount) &&
                   (type == null || ret.get("type").equals(type)))
                    return ret;
        return null;
    }

    public Map<String, Object> findSubaccount(final Integer subAccount) {
        return findSubaccountByType(subAccount, null);
    }

    public Map<?, ?> getTwoFactorConfig() {
        return mTwoFactorConfig;
    }

    public boolean hasAnyTwoFactor() {
        return mTwoFactorConfig != null && (Boolean) mTwoFactorConfig.get("any");
    }

    public ListenableFuture<Boolean> setUserConfig(final String key, final Object value, final boolean updateImmediately) {
        return mClient.setUserConfig(ImmutableMap.of(key, value), updateImmediately);
    }

    public ListenableFuture<Boolean> setUserConfig(final Map<String, Object> values, final boolean updateImmediately) {
        return mClient.setUserConfig(values, updateImmediately);
    }

    public ListenableFuture<Object> requestTwoFacCode(final String method, final String action, final Object data) {
        return mClient.requestTwoFacCode(method, action, data);
    }

    public ListenableFuture<Map<?, ?>> prepareSweepSocial(final byte[] pubKey, final boolean useElectrum) {
        return mClient.prepareSweepSocial(pubKey, useElectrum);
    }

    public static byte[] serializeProtobuf(final GeneratedMessage msg) {
        return WalletClient.serializeProtobuf(msg);
    }

    public ListenableFuture<PaymentSession> fetchPaymentRequest(final String url) {
        return mClient.fetchPaymentRequest(url);
    }

    public ListenableFuture<PaymentProtocol.Ack>
    sendPayment(final PaymentSession paymentSession, final List<Transaction> txns, final Address refundAddr, final String memo)
        throws PaymentProtocolException.InvalidNetwork, PaymentProtocolException.InvalidPaymentURL,
            PaymentProtocolException.Expired, IOException {
        return mClient.sendPayment(paymentSession, txns, refundAddr, memo);
    }

    public Map<String, String> make2FAData(final String method, final String code) {
        if (code == null)
            return new HashMap<>();
        return ImmutableMap.of("method", method, "code", code);
    }

    public ListenableFuture<Boolean> initEnableTwoFac(final String type, final String details, final Map<?, ?> twoFacData) {
        return mClient.initEnableTwoFac(type, details, twoFacData);
    }

    public ListenableFuture<Boolean> enableTwoFactor(final String type, final String code, final Object twoFacData) {
        return Futures.transform(mClient.enableTwoFactor(type, code, twoFacData), new Function<Boolean, Boolean>() {
            @Override
            public Boolean apply(final Boolean input) {
                getAvailableTwoFactorMethods();
                return input;
            }
        });
    }

    public Boolean disableTwoFactor(final String type, final Map<String, String> twoFacData) throws Exception {
        if (!mClient.disableTwoFactor(type, twoFacData))
            return false;
        mTwoFactorConfig = mClient.getTwoFactorConfigSync();
        mTwoFactorConfigObservable.doNotify();
        return true;
    }

    private void resetFiatSpendingLimits() {
        if (!isWatchOnly() && mLimitsData.getBool("is_fiat")) {
            mLimitsData.mData.put("total", 0);
            mLimitsData.mData.put("per_tx", 0);
        }
    }

    public JSONMap makeLimitsData(final long limit, final boolean isFiat) {
        final JSONMap limitsData = new JSONMap();
        limitsData.mData.put("total", limit);
        limitsData.mData.put("per_tx", 0);
        limitsData.mData.put("is_fiat", isFiat);
        return limitsData;
    }

    public void setSpendingLimits(final JSONMap limitsData,
                                  final Map<String, String> twoFacData) throws Exception {
        mClient.setSpendingLimits(limitsData, twoFacData);
        mLimitsData = limitsData;
    }

    public void sendNLocktime() throws Exception {
        mClient.sendNLocktime();
    }

    public JSONMap getSpendingLimits() {
        return mLimitsData;
    }

    // Get the users spending limit in BTC/the primary asset
    private Coin getSpendingLimitAmount() {
        final Coin unconverted = mLimitsData.getCoin("total");
        if (IS_ELEMENTS)
            return unconverted.multiply(100);
        if (!mLimitsData.getBool("is_fiat"))
            return unconverted;
        // Fiat class uses SMALLEST_UNIT_EXPONENT units (10^4), our limit is
        // held in 10^2 (e.g. cents) units, hence we * 100 below.
        final Fiat fiatLimit = Fiat.valueOf("???", mLimitsData.getLong("total") * 100);
        return getFiatRate().fiatToCoin(fiatLimit);
    }

    public boolean isUnderLimit(final Coin amount) {
        return !hasAnyTwoFactor() || !amount.isGreaterThan(getSpendingLimitAmount());
    }

    public boolean doesLimitChangeRequireTwoFactor(final long newValue, final boolean isFiat) {
        return hasAnyTwoFactor() && (isFiat != mLimitsData.getBool("is_fiat") ||
                                     newValue > mLimitsData.getLong("total"));
    }

    public List<String> getEnabledTwoFactorMethods() {
        if (mTwoFactorConfig == null)
            return null;
        final String[] methods = getResources().getStringArray(R.array.twoFactorMethods);
        final ArrayList<String> enabled = new ArrayList<>();
        for (final String method : methods)
            if (((Boolean) mTwoFactorConfig.get(method)))
                enabled.add(method);
        return enabled;
    }

    private static class GaObservable extends Observable {
        public void doNotify() {
            setChanged();
            notifyObservers();
        }
    }

    public int getCurrentBlock(){
        return mCurrentBlock;
    }

    private void setCurrentBlock(final int newBlock){
        // FIXME: In case a transaction list call races with a block
        // notification, this could potentially go backwards. It can also
        // go backwards following a reorg which is probably not handled well.
        if (newBlock > mCurrentBlock)
            mCurrentBlock = newBlock;
    }

    // FIXME: Operations should be atomic
    public static class State extends Observable {
        private ConnState mConnState;
        private boolean mForcedLogout;
        private boolean mForcedTimeout;

        public State() {
            mConnState = ConnState.OFFLINE;
            setForcedLogout(false);
            setForcedTimeout(false);
        }

        private void setForcedLogout(final boolean forcedLogout) { mForcedLogout = forcedLogout; }
        private void setForcedTimeout(final boolean forcedTimeout) { mForcedTimeout = forcedTimeout; }
        public boolean isForcedOff() { return mForcedLogout || mForcedTimeout; }
        public boolean isLoggedIn() { return mConnState == ConnState.LOGGEDIN; }
        public boolean isLoggedOrLoggingIn() {
            return mConnState == ConnState.LOGGEDIN || mConnState == ConnState.LOGGINGIN;
        }
        public boolean isConnected() { return mConnState == ConnState.CONNECTED; }
        public boolean isDisconnected() { return mConnState == ConnState.DISCONNECTED; }
        public boolean isDisconnectedOrOffline() {
            return mConnState == ConnState.DISCONNECTED || mConnState == ConnState.OFFLINE;
        }

        private void transitionTo(final ConnState newState) {
            if (mConnState == newState)
                return; // Nothing to do

            if (newState == ConnState.OFFLINE) {
                // Transition through disconnected before going offline
                transitionTo(ConnState.DISCONNECTED);
            }

            mConnState = newState;
            if (newState == ConnState.LOGGEDIN) {
                setForcedLogout(false);
                setForcedTimeout(false);
            }
            doNotify();
        }

        private void doNotify() {
             setChanged();
             // FIXME: Should pass a copy of ourselves
             notifyObservers(this);
        }
    }

    private final State mState = new State();

    public boolean isForcedOff() { return mState.isForcedOff(); }
    public boolean isLoggedIn() { return mState.isLoggedIn(); }
    public boolean isLoggedOrLoggingIn() { return mState.isLoggedOrLoggingIn(); }
    public boolean isConnected() { return mState.isConnected(); }

    public void addConnectionObserver(final Observer o) { mState.addObserver(o); }
    public void deleteConnectionObserver(final Observer o) { mState.deleteObserver(o); }

    private ScheduledThreadPoolExecutor mTimerExecutor;
    private BroadcastReceiver mNetConnectivityReceiver;
    private ScheduledFuture<?> mDisconnectTimer;
    private ScheduledFuture<?> mReconnectTimer;
    private int mReconnectDelay;
    private int mRefCount; // Number of non-paused activities using us

    public void incRef() {
        ++mRefCount;
        cancelDisconnect();
        if (mState.isDisconnected())
            reconnect();
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

    private void scheduleDisconnect() {
        final int delayMins = getAutoLogoutMinutes();
        cancelDisconnect();
        Log.d(TAG, "scheduleDisconnect in " + Integer.toString(delayMins) + " mins");
        mDisconnectTimer = mTimerExecutor.schedule(new Runnable() {
            public void run() {
                Log.d(TAG, "scheduled disconnect");
                mState.setForcedTimeout(true);
                disconnect(false); // Calls transitionTo(DISCONNECTED)
            }
        }, delayMins, TimeUnit.MINUTES);
    }

    private void scheduleReconnect() {
        final int RECONNECT_TIMEOUT = 6000;
        final int RECONNECT_TIMEOUT_MAX = 50000;

        if (mReconnectDelay < RECONNECT_TIMEOUT_MAX)
            mReconnectDelay *= 1.2;
        if (mReconnectDelay == 0)
            mReconnectDelay = RECONNECT_TIMEOUT;

        Log.d(TAG, "scheduleReconnect in " + Integer.toString(mReconnectDelay) + " ms");
        if (mReconnectTimer != null && !mReconnectTimer.isCancelled()) {
            Log.d(TAG, "cancelReconnect");
            mReconnectTimer.cancel(false);
        }
        mReconnectTimer = mTimerExecutor.schedule(new Runnable() {
            public void run() {
                Log.d(TAG, "scheduled reconnect");
                reconnect();
            }
        }, mReconnectDelay, TimeUnit.MILLISECONDS);
    }

    private void onNetConnectivityChanged() {
        final NetworkInfo info = getNetworkInfo();
        if (info == null) {
            // No network connection, go offline until notified that its back
            mState.transitionTo(ConnState.OFFLINE);
        } else if (mState.isDisconnectedOrOffline()) {
            // We have a network connection and are currently disconnected/offline:
            // Move to disconnected and try to reconnect
            mSPV.onNetConnectivityChangedAsync(info);
            mState.transitionTo(ConnState.DISCONNECTED);
            reconnect();
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

    public static Transaction buildTransaction(final String hex) {
        return new Transaction(Network.NETWORK, Wally.hex_to_bytes(hex));
    }
}
