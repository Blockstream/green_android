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
import android.os.IBinder;
import android.os.Binder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.blockstream.libwally.Wally;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.greenapi.CryptoHelper;
import com.greenaddress.greenapi.INotificationHandler;
import com.greenaddress.greenapi.ISigningWallet;
import com.greenaddress.greenapi.LoginData;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.PinData;
import com.greenaddress.greenapi.PreparedTransaction;
import com.greenaddress.greenapi.WalletClient;
import com.greenaddress.greenbits.spv.SPV;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.wallets.TrezorHWWallet;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.utils.Fiat;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
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

public class GaService extends Service {
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
        checkNetwork();
        app.registerReceiver(mNetBroadReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @NonNull public final ListeningExecutorService es = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(3));
    @NonNull final private Map<Integer, GaObservable> balanceObservables = new HashMap<>();
    @NonNull final private GaObservable newTransactionsObservable = new GaObservable();
    @NonNull final private GaObservable newTxVerifiedObservable = new GaObservable();
    public ListenableFuture<Void> onConnected;
    private String mSignUpMnemonics = null;
    private QrBitmap mSignUpQRCode = null;
    private int curBlock = 0;

    private boolean mAutoReconnect = true;
    // cache
    private ListenableFuture<List<List<String>>> currencyExchangePairs;

    private final Map<Integer, Coin> balancesCoin = new HashMap<>();

    private final Map<Integer, Fiat> balancesFiat = new HashMap<>();
    private float fiatRate;
    private String fiatCurrency;
    private String fiatExchange;
    private ArrayList subaccounts;

    private final Map<Integer, DeterministicKey> gaDeterministicKeys = new HashMap<>();
    private String receivingId;
    private byte[] gaitPath;
    @Nullable
    private Map<?, ?> twoFacConfig;
    private final GaObservable twoFacConfigObservable = new GaObservable();
    @Nullable
    private String deviceId;

    public final SPV spv = new SPV(this);

    private WalletClient mClient;

    public int getAutoLogoutMinutes() {
        try {
            return (int)getUserConfig("altimeout");
        } catch (final Exception e) {
            return 5; // Not logged in/not set, default to 5 min
        }
    }

    public File getSPVChainFile() {
        final String dirName = "blockstore_" + getReceivingId();
        return new File(getDir(dirName, Context.MODE_PRIVATE), "blockchain.spvchain");
    }

    @NonNull
    public Observable getTwoFacConfigObservable() {
        return twoFacConfigObservable;
    }

    private void getAvailableTwoFacMethods() {
        Futures.addCallback(mClient.getTwoFacConfig(), new FutureCallback<Map<?, ?>>() {
            @Override
            public void onSuccess(@Nullable final Map<?, ?> result) {
                twoFacConfig = result;
                twoFacConfigObservable.setChangedAndNotify();
            }

            @Override
            public void onFailure(@NonNull final Throwable t) {
                t.printStackTrace();
            }
        }, es);
    }

    private void reconnect() {
        Log.i(TAG, "Submitting reconnect after " + mReconnectDelay);
        onConnected = mClient.connect();
        mState.transitionTo(ConnState.CONNECTING);

        Futures.addCallback(onConnected, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable final Void result) {
                mState.transitionTo(ConnState.CONNECTED);
                Log.i(TAG, "Success CONNECTED callback");
                if (!mState.isForcedOff() && mClient.canLogin()) {
                    login();
                }
            }

            @Override
            public void onFailure(@NonNull final Throwable t) {
                Log.i(TAG, "Failure throwable callback " + t.toString());
                mState.transitionTo(ConnState.DISCONNECTED);
                scheduleReconnect();
            }
        }, es);
    }

    public static boolean isValidAddress(final String address) {
        try {
            new org.bitcoinj.core.Address(Network.NETWORK, address);
            return true;
        } catch (@NonNull final AddressFormatException e) {
            return false;
        }
    }

    // Sugar for fetching/editing preferences
    public SharedPreferences cfg() { return PreferenceManager.getDefaultSharedPreferences(this); }
    public SharedPreferences cfg(final String name) { return getSharedPreferences(name, MODE_PRIVATE); }
    public SharedPreferences.Editor cfgEdit(final String name) { return cfg(name).edit(); }
    public SharedPreferences cfgIn(final String name) { return cfg(name + getReceivingId()); }
    public SharedPreferences.Editor cfgInEdit(final String name) { return cfgIn(name).edit(); }

    // User config is stored on the server (unlike preferences which are local)
    public Object getUserConfig(@NonNull final String key) {
        return mClient.getUserConfig(key);
    }

    public boolean isSPVEnabled() { return cfg("SPV").getBoolean("enabled", true); }
    public String getProxyHost() { return cfg().getString("proxy_host", null); }
    public String getProxyPort() { return cfg().getString("proxy_port", null); }
    public int getCurrentSubAccount() { return cfg("main").getInt("curSubaccount", 0); }
    public void setCurrentSubAccount(int subaccount) { cfgEdit("main").putInt("curSubaccount", subaccount).apply(); }

    @Override
    public void onCreate() {
        super.onCreate();

        // Uncomment to test slow service creation
        // android.os.SystemClock.sleep(10000);

        mTimerExecutor = new ScheduledThreadPoolExecutor(1);
        mTimerExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        mNetBroadReceiver = new BroadcastReceiver() {
            public void onReceive(final Context context, final Intent intent) {
                checkNetwork();
            }
        };

        deviceId = cfg("service").getString("device_id", null);
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            cfgEdit("service").putString("device_id", deviceId).apply();
        }

        mClient = new WalletClient(new INotificationHandler() {
            @Override
            public void onNewBlock(final int count) {
                Log.i(TAG, "onNewBlock");
                if (isSPVEnabled())
                    spv.addToBloomFilter(count, null, -1, -1, -1);

                newTransactionsObservable.setChangedAndNotify();
            }

            @Override
            public void onNewTransaction(final int wallet_id, @NonNull final int[] subaccounts, final long value, final String txhash) {
                Log.i(TAG, "onNewTransactions");
                spv.updateUnspentOutputs();
                newTransactionsObservable.setChangedAndNotify();
                for (final int subaccount : subaccounts) {
                    updateBalance(subaccount);
                }
            }

            @Override
            public void onConnectionClosed(final int code) {
                gaDeterministicKeys.clear();

                // Server error codes FIXME: These should be in a class somewhere
                // 4000 (concurrentLoginOnDifferentDeviceId) && 4001 (concurrentLoginOnSameDeviceId!)
                // 1000 NORMAL_CLOSE
                // 1006 SERVER_RESTART
                final boolean forcedLogout = code == 4000;
                setDisconnected(forcedLogout);

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
        }, es);

        final String proxyHost = getProxyHost();
        final String proxyPort = getProxyPort();
        if (proxyHost != null && proxyPort != null) {
            mClient.setProxy(proxyHost, proxyPort);
        }
    }

    public ListenableFuture<byte[]> createOutScript(final Integer subaccount, final Integer pointer) {
        final List<ECKey> pubkeys = new ArrayList<>();
        final DeterministicKey gaWallet = getGaDeterministicKey(subaccount);
        final ECKey gaKey = HDKeyDerivation.deriveChildKey(gaWallet, new ChildNumber(pointer));
        pubkeys.add(gaKey);

        ISigningWallet userWallet = mClient.getHdWallet();
        if (subaccount != 0) {
            userWallet = userWallet.deriveChildKey(new ChildNumber(3, true));
            userWallet = userWallet.deriveChildKey(new ChildNumber(subaccount, true));
        }
        final DeterministicKey master = userWallet.getPubKey();

        return es.submit(new Callable<byte[]>() {
            public byte[] call() {
                final DeterministicKey derivedRoot = HDKeyDerivation.deriveChildKey(master, new ChildNumber(1));
                final DeterministicKey derivedPointer = HDKeyDerivation.deriveChildKey(derivedRoot, new ChildNumber(pointer));
                pubkeys.add(derivedPointer);

                String twoOfThreeBackupChaincode = null, twoOfThreeBackupPubkey = null;
                for (final Object subaccount_ : subaccounts) {
                    final Map<String, ?> subaccountMap = (Map) subaccount_;
                    if (subaccountMap.get("type").equals("2of3") && subaccountMap.get("pointer").equals(subaccount)) {
                        twoOfThreeBackupChaincode = (String) subaccountMap.get("2of3_backup_chaincode");
                        twoOfThreeBackupPubkey = (String) subaccountMap.get("2of3_backup_pubkey");
                    }
                }

                if (twoOfThreeBackupChaincode != null) {
                    final DeterministicKey backupWalletMaster = new DeterministicKey(
                            new ImmutableList.Builder<ChildNumber>().build(),
                            Hex.decode(twoOfThreeBackupChaincode),
                            ECKey.fromPublicOnly(Hex.decode(twoOfThreeBackupPubkey)).getPubKeyPoint(),
                            null, null);
                    final DeterministicKey derivedBackupRoot = HDKeyDerivation.deriveChildKey(backupWalletMaster, new ChildNumber(1));
                    final DeterministicKey derivedBackupPointer = HDKeyDerivation.deriveChildKey(derivedBackupRoot, new ChildNumber(pointer));
                    pubkeys.add(derivedBackupPointer);
                }

                return Script.createMultiSigOutputScript(2, pubkeys);
            }
        });
    }

    @NonNull
    private ListenableFuture<Boolean> verifyP2SHSpendableBy(@NonNull final Script scriptHash, final Integer subaccount, final Integer pointer) {
        if (!scriptHash.isPayToScriptHash())
            return Futures.immediateFuture(false);
        final byte[] gotP2SH = scriptHash.getPubKeyHash();

        return Futures.transform(createOutScript(subaccount, pointer), new Function<byte[], Boolean>() {
            @javax.annotation.Nullable
            @Override
            public Boolean apply(final @javax.annotation.Nullable byte[] multisig) {
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

    @NonNull
    public ListenableFuture<Boolean> verifySpendableBy(@NonNull final TransactionOutput txOutput, final Integer subaccount, final Integer pointer) {
        return verifyP2SHSpendableBy(txOutput.getScriptPubKey(), subaccount, pointer);
    }

    private DeterministicKey getKeyPath(final DeterministicKey node) {
        int childNum;
        DeterministicKey nodePath = node;
        for (int i = 0; i < 32; ++i) {
            int b1 = gaitPath[i * 2];
            if (b1 < 0) {
                b1 = 256 + b1;
            }
            int b2 = gaitPath[i * 2 + 1];
            if (b2 < 0) {
                b2 = 256 + b2;
            }
            childNum = b1 * 256 + b2;
            nodePath = HDKeyDerivation.deriveChildKey(nodePath, new ChildNumber(childNum));
        }
        return nodePath;
    }

    private DeterministicKey getGaDeterministicKey(final Integer subaccount) {
        if (gaDeterministicKeys.keySet().contains(subaccount)) {
            return gaDeterministicKeys.get(subaccount);
        }

        final DeterministicKey nodePath = getKeyPath(HDKeyDerivation.deriveChildKey(new DeterministicKey(
                new ImmutableList.Builder<ChildNumber>().build(),
                Hex.decode(Network.depositChainCode),
                ECKey.fromPublicOnly(Hex.decode(Network.depositPubkey)).getPubKeyPoint(),
                null, null), new ChildNumber(subaccount != 0 ? 3 : 1)));

        final DeterministicKey key = subaccount == 0 ? nodePath : HDKeyDerivation.deriveChildKey(nodePath, new ChildNumber(subaccount, false));
        gaDeterministicKeys.put(subaccount, key);
        return key;
    }

    private ListenableFuture<LoginData> loginImpl(final ListenableFuture<LoginData> f) {
        mState.transitionTo(ConnState.LOGGINGIN);
        Futures.addCallback(f, new FutureCallback<LoginData>() {
            @Override
            public void onSuccess(final LoginData result) {
                // FIXME: Why are we copying these? If we need them when not logged in,
                // we should just copy the whole loginData instance
                fiatCurrency = result.currency;
                fiatExchange = result.exchange;
                subaccounts = result.subaccounts;
                receivingId = result.receiving_id;
                gaitPath = Hex.decode(result.gait_path);

                balanceObservables.put(0, new GaObservable());
                updateBalance(0);
                for (final Object subaccount : result.subaccounts) {
                    final Map<?, ?> subaccountMap = (Map) subaccount;
                    final int pointer = ((Integer) subaccountMap.get("pointer"));
                    balanceObservables.put(pointer, new GaObservable());
                    updateBalance(pointer);
                }
                getAvailableTwoFacMethods();
                gaDeterministicKeys.clear();
                spv.startIfEnabled();
                mState.transitionTo(ConnState.LOGGEDIN);
            }

            @Override
            public void onFailure(final Throwable t) {
                t.printStackTrace();
                mState.transitionTo(ConnState.CONNECTED);
            }
        }, es);
        return f;
    }

    private ListenableFuture<LoginData> login() {
        return loginImpl(mClient.login(deviceId));
    }

    public ListenableFuture<LoginData> login(final ISigningWallet signingWallet) {
        return loginImpl(mClient.login(signingWallet, deviceId));
    }

    public ListenableFuture<LoginData> login(final String mnemonics) {
        return loginImpl(mClient.login(mnemonics, deviceId));
    }

    public ListenableFuture<LoginData> login(final PinData pinData, final String pin) {
        return loginImpl(mClient.login(pinData, pin, deviceId));
    }

    public ListenableFuture<LoginData> signup(final String mnemonics) {
        return loginImpl(mClient.loginRegister(mnemonics, deviceId));
    }

    @NonNull
    public ListenableFuture<LoginData> signup(final ISigningWallet signingWallet,
                                              final byte[] masterPublicKey, final byte[] masterChaincode,
                                              final byte[] pathPublicKey, final byte[] pathChaincode) {
        return loginImpl(mClient.loginRegister(signingWallet, masterPublicKey, masterChaincode,
                                               pathPublicKey, pathChaincode, deviceId));
    }

    @Nullable
    public String getMnemonics() {
        return mClient.getMnemonics();
    }

    public LoginData getLoginData() {
        return mClient.getLoginData();
    }

    public void disconnect(final boolean autoReconnect) {
        mAutoReconnect = autoReconnect;
        spv.stopSPVSync();
        for (final Integer key : balanceObservables.keySet())
            balanceObservables.get(key).deleteObservers();
        mClient.disconnect();
        mState.transitionTo(ConnState.DISCONNECTED);
    }

    @NonNull
    public ListenableFuture<Map<?, ?>> updateBalance(final int subaccount) {
        final ListenableFuture<Map<?, ?>> future = mClient.getSubaccountBalance(subaccount);
        Futures.addCallback(future, new FutureCallback<Map<?, ?>>() {
            @Override
            public void onSuccess(@Nullable final Map<?, ?> result) {
                balancesCoin.put(subaccount, Coin.valueOf(Long.valueOf((String) result.get("satoshi"))));
                fiatRate = Float.valueOf((String) result.get("fiat_exchange"));
                // Fiat.parseFiat uses toBigIntegerExact which requires at most 4 decimal digits,
                // while the server can return more, hence toBigInteger instead here:
                final BigInteger tmpValue = new BigDecimal((String) result.get("fiat_value"))
                        .movePointRight(Fiat.SMALLEST_UNIT_EXPONENT).toBigInteger();
                // also strip extra decimals (over 2 places) because that's what the old JS client does
                final BigInteger fiatValue = tmpValue.subtract(tmpValue.mod(BigInteger.valueOf(10).pow(Fiat.SMALLEST_UNIT_EXPONENT - 2)));
                balancesFiat.put(subaccount, Fiat.valueOf((String) result.get("fiat_currency"), fiatValue.longValue()));

                fireBalanceChanged(subaccount);
            }

            @Override
            public void onFailure(@NonNull final Throwable t) {

            }
        }, es);
        return future;
    }

    @NonNull
    public ListenableFuture<Map<?, ?>> getSubaccountBalance(final int pointer) {
        return mClient.getSubaccountBalance(pointer);
    }

    public void fireBalanceChanged(final int subaccount) {
        if (getBalanceCoin(subaccount) == null) {
            // Called from addUtxoToValues before balance is fetched
            return;
        }
        balanceObservables.get(subaccount).setChangedAndNotify();
    }

    @NonNull
    public ListenableFuture<Boolean> setPricingSource(final String currency, final String exchange) {
        return Futures.transform(mClient.setPricingSource(currency, exchange), new Function<Boolean, Boolean>() {
            @Override
            public Boolean apply(final Boolean input) {
                fiatCurrency = currency;
                fiatExchange = exchange;
                return input;
            }
        });
    }

    public ListenableFuture<Map<?, ?>> getMyTransactions(final int subaccount) {
        return es.submit(new Callable<Map<?, ?>>() {
            @Override
            public Map<?, ?> call() throws Exception {
                Map<?, ?> result = mClient.getMyTransactions(subaccount);
                final int curBlock = ((Integer) result.get("cur_block"));
                setCurBlock(curBlock);
                // FIXME: Does this really belong here?
                if (isSPVEnabled()) {
                    spv.setUpSPV();
                    spv.startSpvSync();
                }
                return result;
            }
        });
    }

    @NonNull
    public ListenableFuture<PinData> setPin(@NonNull final byte[] seed, final String mnemonic, final String pin, final String device_name) {
        return mClient.setPin(seed, mnemonic, pin, device_name);
    }

    private void preparePrivData(@NonNull final Map<String, Object> privateData) {
        final int subaccount = privateData.containsKey("subaccount")? (int) privateData.get("subaccount"):0;
        // skip fetching raw if not needed
        final Coin verifiedBalance = spv.verifiedBalancesCoin.get(subaccount);
        if (!isSPVEnabled() ||
            verifiedBalance == null || !verifiedBalance.equals(getBalanceCoin(subaccount)) ||
            mClient.getHdWallet().requiresPrevoutRawTxs()) {
            privateData.put("prevouts_mode", "http");
        } else {
            privateData.put("prevouts_mode", "skip");
        }

        final Object rbf_optin = getUserConfig("replace_by_fee");
        if (rbf_optin != null)
            privateData.put("rbf_optin", rbf_optin);
    }

    public ListenableFuture<List<String>> signTransaction(final PreparedTransaction tx, final boolean isPrivate) {
        return mClient.signTransaction(tx, isPrivate);
    }

    @NonNull
    public ListenableFuture<PreparedTransaction> prepareTx(@NonNull final Coin coinValue, final String recipient, @NonNull final Map<String, Object> privateData) {
        preparePrivData(privateData);
        return mClient.prepareTx(coinValue.longValue(), recipient, "sender", privateData);
    }

    @NonNull
    public ListenableFuture<PreparedTransaction> prepareSweepAll(final int subaccount, final String recipient, @NonNull final Map<String, Object> privData) {
        preparePrivData(privData);
        return mClient.prepareTx(
                getBalanceCoin(subaccount).longValue(),
                recipient, "receiver", privData
        );
    }

    @NonNull
    public ListenableFuture<String> signAndSendTransaction(@NonNull final PreparedTransaction prepared, final Object twoFacData) {
        return Futures.transform(signTransaction(prepared, false), new AsyncFunction<List<String>, String>() {
            @NonNull
            @Override
            public ListenableFuture<String> apply(@NonNull final List<String> input) throws Exception {
                return mClient.sendTransaction(input, twoFacData);
            }
        }, es);
    }

    public ListenableFuture<Map<String, Object>> sendRawTransaction(Transaction tx, Map<String, Object> twoFacData, final boolean returnErrorUri) {
        return mClient.sendRawTransaction(tx, twoFacData, returnErrorUri);
    }

    public ListenableFuture<ArrayList> getAllUnspentOutputs(int confs, Integer subaccount) {
        return mClient.getAllUnspentOutputs(confs, subaccount);
    }

    public ListenableFuture<Transaction> getRawUnspentOutput(final Sha256Hash txHash) {
        return mClient.getRawUnspentOutput(txHash);
    }

    public ListenableFuture<Transaction> getRawOutput(final Sha256Hash txHash) {
        return mClient.getRawOutput(txHash);
    }

    public ListenableFuture<Boolean> changeMemo(final String txHash, final String memo) {
        return mClient.changeMemo(txHash, memo);
    }

    @NonNull
    public ListenableFuture<String> sendTransaction(@NonNull final List<TransactionSignature> signatures) {
        final List<String> signaturesStrings = new LinkedList<>();
        for (final TransactionSignature sig : signatures) {
            signaturesStrings.add(new String(Hex.encode(sig.encodeToBitcoin())));
        }
        return mClient.sendTransaction(signaturesStrings, null);
    }

    private static byte[] getSegWitScript(final byte[] input) {
        final ByteArrayOutputStream bits = new ByteArrayOutputStream();
        bits.write(0);
        try {
            Script.writeBytes(bits, Wally.sha256(input, null));
        } catch (final IOException e) {
            throw new RuntimeException(e);  // cannot happen
        }
        return bits.toByteArray();
    }

    public ListenableFuture<Map> getNewAddress(final int subaccount) {
        return mClient.getNewAddress(subaccount);
    }

    @NonNull
    public ListenableFuture<QrBitmap> getNewAddressBitmap(final int subaccount) {
        final AsyncFunction<Map, String> verifyAddress = new AsyncFunction<Map, String>() {
            @NonNull
            @Override
            public ListenableFuture<String> apply(@NonNull final Map input) throws Exception {
                final Integer pointer = ((Integer) input.get("pointer"));
                final byte[] script = Hex.decode((String) input.get("script")),
                             scriptHash;
                if (getLoginData().segwit) {
                    // allow segwit p2sh only if segwit is enabled
                    scriptHash = Utils.sha256hash160(getSegWitScript(script));
                } else {
                    scriptHash = Utils.sha256hash160(script);
                }
                return Futures.transform(verifyP2SHSpendableBy(
                        ScriptBuilder.createP2SHOutputScript(scriptHash),
                        subaccount, pointer), new Function<Boolean, String>() {
                    @Nullable
                    @Override
                    public String apply(final @Nullable Boolean input) {
                        if (input) {
                            return Address.fromP2SHHash(Network.NETWORK, scriptHash).toString();
                        } else {
                            throw new IllegalArgumentException("Address validation failed");
                        }
                    }
                });
            }
        };
        final AsyncFunction<String, QrBitmap> addressToQr = new AsyncFunction<String, QrBitmap>() {
            @NonNull
            @Override
            public ListenableFuture<QrBitmap> apply(@NonNull final String input) {
                return es.submit(new QrBitmap(input, 0 /* transparent background */));
            }
        };
        final ListenableFuture<String> verifiedAddress = Futures.transform(getNewAddress(subaccount), verifyAddress, es);
        return Futures.transform(verifiedAddress, addressToQr, es);
    }

    public ListenableFuture<List<List<String>>> getCurrencyExchangePairs() {
        if (currencyExchangePairs == null) {
            currencyExchangePairs = Futures.transform(mClient.getAvailableCurrencies(), new Function<Map<?, ?>, List<List<String>>>() {
                @Override
                public List<List<String>> apply(@Nullable final Map<?, ?> result) {
                    final Map<String, ArrayList<String>> per_exchange = (Map) result.get("per_exchange");
                    final List<List<String>> ret = new LinkedList<>();
                    for (final String exchange : per_exchange.keySet()) {
                        for (final String currency : per_exchange.get(exchange)) {
                            final ArrayList<String> currency_exchange = new ArrayList<>(2);
                            currency_exchange.add(currency);
                            currency_exchange.add(exchange);
                            ret.add(currency_exchange);
                        }
                    }
                    Collections.sort(ret, new Comparator<List<String>>() {
                        @Override
                        public int compare(final @NonNull List<String> lhs, @NonNull final List<String> rhs) {
                            return lhs.get(0).compareTo(rhs.get(0));
                        }
                    });
                    return ret;
                }
            }, es);
        }
        return currencyExchangePairs;
    }

    public void resetSignUp() {
        mSignUpMnemonics = null;
        mSignUpQRCode = null;
    }

    public String getSignUpMnemonic() {
        if (mSignUpMnemonics == null)
            mSignUpMnemonics = CryptoHelper.mnemonic_from_bytes(CryptoHelper.randomBytes(32));
        return mSignUpMnemonics;
    }

    public Bitmap getSignUpQRCode() {
        if (mSignUpQRCode == null)
            try {
                mSignUpQRCode = new QrBitmap(getSignUpMnemonic(), Color.WHITE).call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        return mSignUpQRCode.qrcode;
    }

    public boolean isTrezorHWWallet() {
        return mClient.getHdWallet() instanceof TrezorHWWallet;
    }

    @NonNull
    public Map<Integer, Observable> getBalanceObservables() {
        final Map<Integer, Observable> ret = new HashMap<>();
        for (final Integer key : balanceObservables.keySet()) {
            ret.put(key, balanceObservables.get(key));
        }
        return ret;
    }

    @NonNull
    public Observable getNewTransactionsObservable() {
        return newTransactionsObservable;
    }

    @NonNull
    public Observable getNewTxVerifiedObservable() {
        return newTxVerifiedObservable;
    }


    public void notifyObservers(final Sha256Hash tx) {
        // FIXME: later spent outputs can be purged
        cfgInEdit("verified_utxo_").putBoolean(tx.toString(), true).apply();
        spv.addUtxoToValues(tx);
        newTxVerifiedObservable.setChangedAndNotify();
    }

    public Coin getBalanceCoin(final int subaccount) {
        return balancesCoin.get(subaccount);
    }

    public Fiat getBalanceFiat(final int subaccount) {
        return balancesFiat.get(subaccount);
    }

    public float getFiatRate() {
        return fiatRate;
    }

    public String getFiatCurrency() {
        return fiatCurrency;
    }

    public String getFiatExchange() {
        return fiatExchange;
    }

    public ArrayList getSubaccounts() {
        return subaccounts;
    }

    @Nullable
    public Map<?, ?> getTwoFacConfig() {
        return twoFacConfig;
    }

    /**
     * @param updateImmediately whether to not wait for server to reply before updating
     *                          the value in local settings dict (set false to wait)
     */
    @NonNull
    public ListenableFuture<Boolean> setUserConfig(@NonNull final String key, @NonNull final Object value, final boolean updateImmediately) {
        return mClient.setUserConfig(key, value, updateImmediately);
    }


    @NonNull
    public ListenableFuture<Object> requestTwoFacCode(@NonNull final String method, @NonNull final String action, @NonNull final Object data) {
        return mClient.requestTwoFacCode(method, action, data);
    }

    @NonNull
    public ListenableFuture<Map<?, ?>> prepareSweepSocial(@NonNull final byte[] pubKey, final boolean useElectrum) {
        return mClient.prepareSweepSocial(pubKey, useElectrum);
    }

    @NonNull
    public ListenableFuture<Map<?, ?>> processBip70URL(@NonNull final String url) {
        return mClient.processBip70URL(url);
    }

    @NonNull
    public ListenableFuture<PreparedTransaction> preparePayreq(@NonNull final Coin amount, @NonNull final Map<?, ?> data, @NonNull final Map<String, Object> privateData) {
        preparePrivData(privateData);
        return mClient.preparePayreq(amount, data, privateData);
    }

    public String getReceivingId() {
        return receivingId;
    }

    @NonNull
    public ListenableFuture<Boolean> initEnableTwoFac(@NonNull final String type, @NonNull final String details, @NonNull final Map<?, ?> twoFacData) {
        return mClient.initEnableTwoFac(type, details, twoFacData);
    }

    public ListenableFuture<Boolean> enableTwoFactor(final String type, final String code, final Object twoFacData) {
        return Futures.transform(mClient.enableTwoFactor(type, code, twoFacData), new Function<Boolean, Boolean>() {
            @Override
            public Boolean apply(final @Nullable Boolean input) {
                getAvailableTwoFacMethods();
                return input;
            }
        });
    }

    @NonNull
    public ListenableFuture<Boolean> disableTwoFac(@NonNull final String type, @NonNull final Map<String, String> twoFacData) {
        return Futures.transform(mClient.disableTwoFac(type, twoFacData), new Function<Boolean, Boolean>() {
            @Nullable
            @Override
            public Boolean apply(final @Nullable Boolean input) {
                getAvailableTwoFacMethods();
                return input;
            }
        });
    }

    @Nullable
    public List<String> getEnabledTwoFacNames(final boolean useSystemNames) {
        if (twoFacConfig == null) return null;
        final String[] allTwoFac = getResources().getStringArray(R.array.twoFactorChoices);
        final String[] allTwoFacSystem = getResources().getStringArray(R.array.twoFactorChoicesSystem);
        final ArrayList<String> enabledTwoFac = new ArrayList<>();
        for (int i = 0; i < allTwoFac.length; ++i) {
            if (((Boolean) twoFacConfig.get(allTwoFacSystem[i]))) {
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
        public void setChangedAndNotify() {
            super.setChanged();
            super.notifyObservers();
        }
    }

    public int getCurBlock(){
        return curBlock;
    }

    private void setCurBlock(final int newBlock){
        curBlock = newBlock;
    }

    public Map<Sha256Hash, List<Integer>> getUnspentOutputsOutpoints() {
        return spv.unspentOutputsOutpoints;
    }

    // FIXME: Operations should be atomic
    public static class State extends Observable {
        private ConnState mConnState;
        private boolean mForcedLogout;
        private boolean mForcedTimeout;

        public State() {
            mConnState = ConnState.OFFLINE;
            mForcedLogout = false;
            mForcedTimeout = false;
        }

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
            mConnState = newState;
            if (newState == ConnState.LOGGEDIN) {
                mForcedLogout = false;
                mForcedTimeout = false;
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
    private BroadcastReceiver mNetBroadReceiver;
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
                setForcedTimeout();
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
        if (mReconnectTimer != null)
            mReconnectTimer.cancel(false);
        mReconnectTimer = mTimerExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "scheduled reconnect");
                reconnect();
            }
        }, mReconnectDelay, TimeUnit.MILLISECONDS);
    }

    private void setForcedTimeout() {
        mState.mForcedTimeout = true;
        disconnect(false); // Calls transitionTo(DISCONNECTED)
    }

    private void setDisconnected(final boolean forcedLogout) {
        mState.mForcedLogout = forcedLogout;
        mState.transitionTo(ConnState.DISCONNECTED);
    }

    private void checkNetwork() {
        boolean changedState = false;
        final NetworkInfo ni = getNetworkInfo();
        if (ni != null) {
            if (mState.isDisconnectedOrOffline()) {
                mState.transitionTo(ConnState.DISCONNECTED);
                changedState = true;
                reconnect();
            }
        } else {
            mState.transitionTo(ConnState.DISCONNECTED);
            mState.transitionTo(ConnState.OFFLINE);
            changedState = true;
        }
        if (!changedState &&
            ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI)
            mState.doNotify();
    }

    private NetworkInfo getNetworkInfo() {
        final Context ctx = getApplicationContext();
        final ConnectivityManager cm;
        cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting() ? ni : null;
    }
}
