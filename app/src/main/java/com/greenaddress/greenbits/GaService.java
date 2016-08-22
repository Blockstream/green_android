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

import com.blockstream.libwally.Wally;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.greenaddress.greenapi.CryptoHelper;
import com.greenaddress.greenapi.HDKey;
import com.greenaddress.greenapi.INotificationHandler;
import com.greenaddress.greenapi.ISigningWallet;
import com.greenaddress.greenapi.LoginData;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.PinData;
import com.greenaddress.greenapi.PreparedTransaction;
import com.greenaddress.greenapi.SWWallet;
import com.greenaddress.greenapi.WalletClient;
import com.greenaddress.greenbits.spv.SPV;
import com.greenaddress.greenbits.ui.R;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.utils.Fiat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
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

    private enum ConnState {
        OFFLINE, DISCONNECTED, CONNECTING, CONNECTED, LOGGINGIN, LOGGEDIN
    }

    class GaBinder extends Binder {
        GaService getService() { return GaService.this; }
    }
    private final IBinder mBinder = new GaBinder();

    @Override
    public IBinder onBind(final Intent intent) { return mBinder; }

    public void onBound(GreenAddressApplication app) {
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
    private final Map<Integer, GaObservable> mBalanceObservables = new HashMap<>();
    private final GaObservable mNewTxObservable = new GaObservable();
    private final GaObservable mVerifiedTxObservable = new GaObservable();
    private String mSignUpMnemonics = null;
    private Bitmap mSignUpQRCode = null;
    private int mCurrentBlock = 0;

    private boolean mAutoReconnect = true;
    // cache
    private ListenableFuture<List<List<String>>> mCurrencyExchangePairs;

    private final Map<Integer, Coin> mCoinBalances = new HashMap<>();

    private final Map<Integer, Fiat> mFiatBalances = new HashMap<>();
    private float mFiatRate;
    private String mFiatCurrency;
    private String mFiatExchange;
    private ArrayList mSubaccounts;
    private String mReceivingId;
    private Map<?, ?> mTwoFactorConfig;
    private final GaObservable mTwoFactorConfigObservable = new GaObservable();
    private String mDeviceId;
    private boolean mUserCancelledPINEntry = false;

    public final SPV mSPV = new SPV(this);

    private WalletClient mClient;

    public ListeningExecutorService getExecutor() {
        return mExecutor;
    }

    public ISigningWallet getSigningWallet() {
        return mClient.getSigningWallet();
    }

    public int getAutoLogoutMinutes() {
        try {
            return (int)getUserConfig("altimeout");
        } catch (final Exception e) {
            return 5; // Not logged in/not set, default to 5 min
        }
    }

    public File getSPVChainFile() {
        final String dirName = "blockstore_" + mReceivingId;
        return new File(getDir(dirName, Context.MODE_PRIVATE), "blockchain.spvchain");
    }

    private void setFiatCurrency(final String currency) {
        getLoginData().currency = currency;
        mFiatCurrency = currency;
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
                    if (mClient.isWatchOnly())
                        loginImpl(mClient.watchOnlylogin(mClient.getWatchOnlyUsername(), mClient.getWatchOnlyPassword()));
                    else if (mClient.getSigningWallet() != null)
                        loginImpl(mClient.login(mClient.getSigningWallet(), mDeviceId));
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

    public static boolean isValidAddress(final String address) {
        try {
            Address.fromBase58(Network.NETWORK, address);
            return true;
        } catch (final AddressFormatException e) {
            return false;
        }
    }

    public boolean isWatchOnly() {
        return mClient.isWatchOnly();
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
    public boolean getTorEnabled() { return cfg().getBoolean("tor_enabled", false); }
    public boolean isProxyEnabled() { return !TextUtils.isEmpty(getProxyHost()) && !TextUtils.isEmpty(getProxyPort());}
    public int getCurrentSubAccount() { return cfg("main").getInt("curSubaccount", 0); }
    public void setCurrentSubAccount(int subAccount) { cfgEdit("main").putInt("curSubaccount", subAccount).apply(); }

    // SPV
    public String getSPVTrustedPeers() { return mSPV.getTrustedPeers(); }
    public void setSPVTrustedPeers(final String peers) { mSPV.setTrustedPeers(peers); }

    public boolean isSPVEnabled() { return mSPV.isEnabled(); }
    public void setSPVEnabled(final boolean enabled) { mSPV.setEnabled(enabled); }

    public boolean isSPVSyncOnMobileEnabled() { return mSPV.isSyncOnMobileEnabled(); }
    public void setSPVSyncOnMobileEnabled(final boolean enabled) { mSPV.setSyncOnMobileEnabled(enabled); }

    public void resetSPV() { mSPV.reset(true /* deleteAllData */, true /* deleteUnspent */); }

    public SPV.Node createSPVNode(final String address) { return mSPV.createNode(address); }
    public PeerGroup getSPVPeerGroup() { return mSPV.getPeerGroup(); }
    public int getSPVHeight() { return mSPV.getSPVHeight(); }
    public int getSPVBlocksRemaining() { return mSPV.getSPVBlocksRemaining(); }
    public Coin getSPVVerifiedBalance(final int subAccount) { return mSPV.getVerifiedBalance(subAccount); }

    public boolean isSPVVerified(final Sha256Hash txHash) { return mSPV.isVerified(txHash); }

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
        mSPV.onNewBlock(blockHeight);
        mNewTxObservable.doNotify();
    }

    @Override
    public void onNewTransaction(final int[] affectedSubAccounts) {
        Log.i(TAG, "onNewTransactions");
        mSPV.updateUnspentOutputs();
        mNewTxObservable.doNotify();
        for (final int subAccount : affectedSubAccounts)
            updateBalance(subAccount);
    }

    @Override
    public void onConnectionClosed(final int code) {
        HDKey.resetCache(null);

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

    public ListenableFuture<byte[]> createOutScript(final int subAccount, final Integer pointer) {
        final List<ECKey> pubkeys = new ArrayList<>();
        pubkeys.add(HDKey.getGAPublicKeys(subAccount, pointer)[1]);
        pubkeys.add(mClient.getSigningWallet().getMyPublicKey(subAccount, pointer));

        return mExecutor.submit(new Callable<byte[]>() {
            public byte[] call() {

                final Map<String, ?> m = findSubaccount("2of3", subAccount);
                if (m != null)
                    pubkeys.add(HDKey.getRecoveryKeys((String) m.get("2of3_backup_chaincode"),
                                                      (String) m.get("2of3_backup_pubkey"), pointer)[1]);

                return Script.createMultiSigOutputScript(2, pubkeys);
            }
        });
    }

    private ListenableFuture<Boolean> verifyP2SHSpendableBy(final Script scriptHash, final int subAccount, final Integer pointer) {
        if (!scriptHash.isPayToScriptHash())
            return Futures.immediateFuture(false);
        final byte[] gotP2SH = scriptHash.getPubKeyHash();

        return Futures.transform(createOutScript(subAccount, pointer), new Function<byte[], Boolean>() {
            @Override
            public Boolean apply(final byte[] multisig) {
                if (getLoginData().segwit) {
                    // allow segwit p2sh only if segwit is enabled
                    if (Arrays.equals(gotP2SH, Utils.sha256hash160(getSegWitScript(multisig)))) {
                        return true;
                    }
                }

                final byte[] expectedP2SH = Utils.sha256hash160(multisig);

                return Arrays.equals(gotP2SH, expectedP2SH);
            }
        });
    }

    public ListenableFuture<Boolean> verifySpendableBy(final TransactionOutput txOutput, final int subAccount, final Integer pointer) {
        return verifyP2SHSpendableBy(txOutput.getScriptPubKey(), subAccount, pointer);
    }

    public String getWatchOnlyUsername() throws Exception {
        return mClient.getWatchOnlyUsername();
    }

    public boolean registerWatchOnly(final String username, final String password) throws Exception {
        return mClient.registerWatchOnly(username, password);
    }

    private ListenableFuture<LoginData> loginImpl(final ListenableFuture<LoginData> loginFn) {
        mState.transitionTo(ConnState.LOGGINGIN);
        Futures.addCallback(loginFn, new FutureCallback<LoginData>() {
            @Override
            public void onSuccess(final LoginData result) {
                // FIXME: Why are we copying these? If we need them when not logged in,
                // we should just copy the whole loginData instance
                mFiatCurrency = result.currency;
                mFiatExchange = result.exchange;
                mSubaccounts = result.subAccounts;
                mReceivingId = result.receivingId;
                HDKey.resetCache(result.gaUserPath);

                mBalanceObservables.put(0, new GaObservable());
                updateBalance(0);
                for (final Object s : result.subAccounts) {
                    final Map<?, ?> m = (Map) s;
                    final int pointer = ((Integer) m.get("pointer"));
                    mBalanceObservables.put(pointer, new GaObservable());
                    updateBalance(pointer);
                }
                if (!isWatchOnly()) {
                    getAvailableTwoFactorMethods();
                    mSPV.start();
                }
                mState.transitionTo(ConnState.LOGGEDIN);
            }

            @Override
            public void onFailure(final Throwable t) {
                t.printStackTrace();
                mState.transitionTo(ConnState.CONNECTED);
            }
        }, mExecutor);
        return loginFn;
    }

    public ListenableFuture<LoginData> login(final ISigningWallet signingWallet) {
        return loginImpl(mClient.login(signingWallet, mDeviceId));
    }

    public ListenableFuture<LoginData> watchOnlyLogin(final String username, final String password) {
        return loginImpl(mClient.watchOnlylogin(username, password));
    }

    public void disableWatchOnly() throws Exception {
        mClient.disableWatchOnly();
    }

    public ListenableFuture<LoginData> login(final String mnemonics) {
        return login(new SWWallet(mnemonics), mnemonics);
    }

    public ListenableFuture<LoginData> login(final ISigningWallet signingWallet, final String mnemonics) {
        return loginImpl(mClient.login(signingWallet, mnemonics, mDeviceId));
    }

    public ListenableFuture<LoginData> signup(final String mnemonics) {
        final SWWallet signingWallet = new SWWallet(mnemonics);
        return loginImpl(mClient.loginRegister(signingWallet,
                                               signingWallet.getMasterKey().getPubKey(),
                                               signingWallet.getMasterKey().getChainCode(),
                                               mnemonics, mDeviceId));
    }

    public ListenableFuture<LoginData> signup(final ISigningWallet signingWallet,
                                              final byte[] masterPublicKey, final byte[] masterChaincode,
                                              final byte[] pathPublicKey, final byte[] pathChaincode) {
        return loginImpl(mClient.loginRegister(signingWallet, masterPublicKey, masterChaincode,
                                               pathPublicKey, pathChaincode, mDeviceId));
    }

    public String getMnemonics() {
        return mClient.getMnemonics();
    }

    public LoginData getLoginData() {
        return mClient.getLoginData();
    }

    public void disconnect(final boolean autoReconnect) {
        mAutoReconnect = autoReconnect;
        mSPV.stopSync();
        for (final GaObservable o : mBalanceObservables.values())
            o.deleteObservers();
        mClient.disconnect();
        mState.transitionTo(ConnState.DISCONNECTED);
    }

    public void updateBalance(final int subAccount) {
        Futures.addCallback(mClient.getSubaccountBalance(subAccount), new FutureCallback<Map<?, ?>>() {
            @Override
            public void onSuccess(final Map<?, ?> result) {
                final String fiatCurrency = (String) result.get("fiat_currency");
                mCoinBalances.put(subAccount, Coin.valueOf(Long.valueOf((String) result.get("satoshi"))));
                mFiatRate = Float.valueOf((String) result.get("fiat_exchange"));
                if (mClient.isWatchOnly())
                    setFiatCurrency(fiatCurrency);
                // Fiat.parseFiat uses toBigIntegerExact which requires at most 4 decimal digits,
                // while the server can return more, hence toBigInteger instead here:
                final BigInteger tmpValue = new BigDecimal((String) result.get("fiat_value"))
                        .movePointRight(Fiat.SMALLEST_UNIT_EXPONENT).toBigInteger();
                // also strip extra decimals (over 2 places) because that's what the old JS client does
                final BigInteger fiatValue = tmpValue.subtract(tmpValue.mod(BigInteger.valueOf(10).pow(Fiat.SMALLEST_UNIT_EXPONENT - 2)));
                mFiatBalances.put(subAccount, Fiat.valueOf(fiatCurrency, fiatValue.longValue()));
                fireBalanceChanged(subAccount);
            }

            @Override
            public void onFailure(final Throwable t) { }
        }, mExecutor);
    }

    public ListenableFuture<Map<?, ?>> getSubaccountBalance(final int pointer) {
        return mClient.getSubaccountBalance(pointer);
    }

    public void fireBalanceChanged(final int subAccount) {
        if (getCoinBalance(subAccount) == null) {
            // Called from addUtxoToValues before balance is fetched
            return;
        }
        mBalanceObservables.get(subAccount).doNotify();
    }

    public ListenableFuture<Boolean> setPricingSource(final String currency, final String exchange) {
        return Futures.transform(mClient.setPricingSource(currency, exchange), new Function<Boolean, Boolean>() {
            @Override
            public Boolean apply(final Boolean input) {
                mFiatCurrency = currency;
                mFiatExchange = exchange;
                return input;
            }
        });
    }

    public ListenableFuture<Map<?, ?>> getMyTransactions(final int subAccount) {
        return mExecutor.submit(new Callable<Map<?, ?>>() {
            @Override
            public Map<?, ?> call() throws Exception {
                Map<?, ?> result = mClient.getMyTransactions(subAccount);
                setCurrentBlock((Integer) result.get("cur_block"));
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
                final String encrypted = Base64.encodeToString(pinData.mSalt, Base64.NO_WRAP) + ";" +
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

    private void preparePrivData(final Map<String, Object> privateData) {
        final int subAccount = privateData.containsKey("subaccount")? (int) privateData.get("subaccount"):0;
        // skip fetching raw if not needed
        final Coin verifiedBalance = getSPVVerifiedBalance(subAccount);
        if (!isSPVEnabled() ||
            verifiedBalance == null || !verifiedBalance.equals(getCoinBalance(subAccount)) ||
            mClient.getSigningWallet().requiresPrevoutRawTxs()) {
            privateData.put("prevouts_mode", "http");
        } else {
            privateData.put("prevouts_mode", "skip");
        }

        final Object rbf_optin = getUserConfig("replace_by_fee");
        if (rbf_optin != null)
            privateData.put("rbf_optin", rbf_optin);
    }

    public ListenableFuture<List<byte[]>> signTransaction(final PreparedTransaction ptx) {
        return mClient.signTransaction(mClient.getSigningWallet(), ptx);
    }

    public ListenableFuture<Coin>
    validateTx(final PreparedTransaction ptx, final String recipientStr, final Coin amount) {
        return mSPV.validateTx(ptx, recipientStr, amount);
    }

    public ListenableFuture<PreparedTransaction> prepareTx(final Coin coinValue, final String recipient, final Map<String, Object> privateData) {
        preparePrivData(privateData);
        return mClient.prepareTx(coinValue.longValue(), recipient, "sender", privateData);
    }

    public ListenableFuture<PreparedTransaction> prepareSweepAll(final int subAccount, final String recipient, final Map<String, Object> privateData) {
        preparePrivData(privateData);
        return mClient.prepareTx(getCoinBalance(subAccount).longValue(), recipient, "receiver", privateData);
    }

    public ListenableFuture<String> signAndSendTransaction(final PreparedTransaction ptx, final Object twoFacData) {
        return Futures.transform(signTransaction(ptx), new AsyncFunction<List<byte[]>, String>() {
            @Override
            public ListenableFuture<String> apply(final List<byte[]> txSigs) throws Exception {
                return mClient.sendTransaction(txSigs, twoFacData);
            }
        }, mExecutor);
    }

    public ListenableFuture<Map<String, Object>> sendRawTransaction(Transaction tx, Map<String, Object> twoFacData, final boolean returnErrorUri) {
        return mClient.sendRawTransaction(tx, twoFacData, returnErrorUri);
    }

    public ListenableFuture<ArrayList> getAllUnspentOutputs(int confs, final Integer subAccount) {
        return mClient.getAllUnspentOutputs(confs, subAccount);
    }

    public ListenableFuture<Transaction> getRawUnspentOutput(final Sha256Hash txHash) {
        return mClient.getRawUnspentOutput(txHash);
    }

    public ListenableFuture<Transaction> getRawOutput(final Sha256Hash txHash) {
        return mClient.getRawOutput(txHash);
    }

    public ListenableFuture<Boolean> changeMemo(final Sha256Hash txHash, final String memo) {
        return mClient.changeMemo(txHash, memo);
    }

    public ListenableFuture<String> sendTransaction(final List<byte[]> txSigs) {
        return mClient.sendTransaction(txSigs, null);
    }

    private static byte[] getSegWitScript(final byte[] input) {
        final ByteArrayOutputStream bits = new ByteArrayOutputStream();
        bits.write(0);
        try {
            Script.writeBytes(bits, Wally.sha256(input));
        } catch (final IOException e) {
            throw new RuntimeException(e);  // cannot happen
        }
        return bits.toByteArray();
    }

    public ListenableFuture<Map> getNewAddress(final int subAccount) {
        return mClient.getNewAddress(subAccount);
    }

    public ListenableFuture<QrBitmap> getNewAddressBitmap(final int subAccount) {
        final AsyncFunction<Map, QrBitmap> verifyAddress = new AsyncFunction<Map, QrBitmap>() {
            @Override
            public ListenableFuture<QrBitmap> apply(final Map input) throws Exception {
                final Integer pointer = ((Integer) input.get("pointer"));
                final byte[] script = Wally.hex_to_bytes((String) input.get("script"));
                final byte[] scriptHash;
                if (getLoginData().segwit) {
                    // allow segwit p2sh only if segwit is enabled
                    scriptHash = Utils.sha256hash160(getSegWitScript(script));
                } else {
                    scriptHash = Utils.sha256hash160(script);
                }

                final ListenableFuture<Boolean> verify;
                if (isWatchOnly()) {
                    verify = Futures.immediateFuture(true);
                } else {
                    final Script sc;
                    sc = ScriptBuilder.createP2SHOutputScript(scriptHash);
                    verify = verifyP2SHSpendableBy(sc, subAccount, pointer);
                }

                return Futures.transform(verify,
                        new Function<Boolean, QrBitmap>() {
                    @Override
                    public QrBitmap apply(final Boolean isValid) {
                        if (!isValid)
                            throw new IllegalArgumentException("Address validation failed");
                        final String address = Address.fromP2SHHash(Network.NETWORK, scriptHash).toString();
                        return new QrBitmap(address, 0 /* transparent background */);
                    }
                });
            }
        };
        return Futures.transform(getNewAddress(subAccount), verifyAddress, mExecutor);
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
        mSignUpMnemonics = null;
        if (mSignUpQRCode != null)
            mSignUpQRCode.recycle();
        mSignUpQRCode = null;
    }

    public String getSignUpMnemonic() {
        if (mSignUpMnemonics == null)
            mSignUpMnemonics = CryptoHelper.mnemonic_from_bytes(CryptoHelper.randomBytes(32));
        return mSignUpMnemonics;
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

    public Fiat getFiatBalance(final int subAccount) {
        return mFiatBalances.get(subAccount);
    }

    public float getFiatRate() {
        return mFiatRate;
    }

    public String getFiatCurrency() {
        return mFiatCurrency;
    }

    public String getFiatExchange() {
        return mFiatExchange;
    }

    public ArrayList getSubaccounts() {
        return mSubaccounts;
    }

    public boolean haveSubaccounts() {
        return mSubaccounts != null && !mSubaccounts.isEmpty();
    }

    public Map<String, ?> findSubaccount(final String type, final Integer subAccount) {
        if (haveSubaccounts()) {
            for (final Object s : mSubaccounts) {
                final Map<String, ?> ret = (Map) s;
                if (ret.get("pointer").equals(subAccount) &&
                   (type == null || ret.get("type").equals(type)))
                    return ret;
            }
        }
        return null;
    }

    public Map<?, ?> getTwoFactorConfig() {
        return mTwoFactorConfig;
    }

    /**
     * @param updateImmediately whether to not wait for server to reply before updating
     *                          the value in local settings dict (set false to wait)
     */
    public ListenableFuture<Boolean> setUserConfig(final String key, final Object value, final boolean updateImmediately) {
        return mClient.setUserConfig(key, value, updateImmediately);
    }


    public ListenableFuture<Object> requestTwoFacCode(final String method, final String action, final Object data) {
        return mClient.requestTwoFacCode(method, action, data);
    }

    public ListenableFuture<Map<?, ?>> prepareSweepSocial(final byte[] pubKey, final boolean useElectrum) {
        return mClient.prepareSweepSocial(pubKey, useElectrum);
    }

    public ListenableFuture<Map<?, ?>> processBip70URL(final String url) {
        return mClient.processBip70URL(url);
    }

    public ListenableFuture<PreparedTransaction> preparePayreq(final Coin amount, final Map<?, ?> data, final Map<String, Object> privateData) {
        preparePrivData(privateData);
        return mClient.preparePayreq(amount, data, privateData);
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

    public ListenableFuture<Boolean> disableTwoFac(final String type, final Map<String, String> twoFacData) {
        return Futures.transform(mClient.disableTwoFac(type, twoFacData), new Function<Boolean, Boolean>() {
            @Override
            public Boolean apply(final Boolean input) {
                getAvailableTwoFactorMethods();
                return input;
            }
        });
    }

    public List<String> getEnabledTwoFacNames(final boolean useSystemNames) {
        if (mTwoFactorConfig == null)
            return null;
        final String[] allTwoFac = getResources().getStringArray(R.array.twoFactorChoices);
        final String[] allTwoFacSystem = getResources().getStringArray(R.array.twoFactorChoicesSystem);
        final ArrayList<String> enabledTwoFac = new ArrayList<>();
        for (int i = 0; i < allTwoFac.length; ++i) {
            if (((Boolean) mTwoFactorConfig.get(allTwoFacSystem[i]))) {
                if (useSystemNames) {
                    enabledTwoFac.add(allTwoFacSystem[i]);
                } else {
                    enabledTwoFac.add(allTwoFac[i]);
                }
            }
        }
        return enabledTwoFac;
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
            if (mConnState.equals(newState))
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

    public void addConnectionObserver(Observer o) { mState.addObserver(o); }
    public void deleteConnectionObserver(Observer o) { mState.deleteObserver(o); }

    private ScheduledThreadPoolExecutor mTimerExecutor = null;
    private BroadcastReceiver mNetConnectivityReceiver;
    private ScheduledFuture<?> mDisconnectTimer = null;
    private ScheduledFuture<?> mReconnectTimer = null;
    private int mReconnectDelay = 0;
    private int mRefCount = 0; // Number of non-paused activities using us

    public void incRef() {
        ++mRefCount;
        cancelDisconnect();
        if (mState.isDisconnected())
            reconnect();
    }

    public void decRef() {
        assert mRefCount > 0 : "Incorrect reference count";
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
            @Override
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
            @Override
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
            mSPV.onNetConnectivityChanged(info);
            mState.transitionTo(ConnState.DISCONNECTED);
            reconnect();
        } else
            mSPV.onNetConnectivityChanged(info);
    }

    public NetworkInfo getNetworkInfo() {
        final Context ctx = getApplicationContext();
        final ConnectivityManager cm;
        cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting() ? ni : null;
    }
}
