package com.greenaddress.greenbits;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.greenaddress.greenapi.INotificationHandler;
import com.greenaddress.greenapi.ISigningWallet;
import com.greenaddress.greenapi.LoginData;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.PinData;
import com.greenaddress.greenapi.PreparedTransaction;
import com.greenaddress.greenapi.WalletClient;
import com.greenaddress.greenbits.ui.BTChipHWWallet;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.utils.Fiat;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;


public class GaService extends Service {

    public final ListeningExecutorService es = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(3));

    private final IBinder mBinder = new GaBinder(this);
    final private Map<Long, GaObservable> balanceObservables = new HashMap<>();
    final private GaObservable newTransactionsObservable = new GaObservable();
    public ListenableFuture<Void> onConnected;
    private int refcount = 0;
    private ListenableFuture<QrBitmap> latestQrBitmapMnemonics;
    private ListenableFuture<String> latestMnemonics;

    private boolean reconnect = true;

    // cache
    private ListenableFuture<List<List<String>>> currencyExchangePairs;
    private Map<Long, ListenableFuture<QrBitmap>> latestAddresses;

    private Map<Long, Coin> balancesCoin = new HashMap<>();
    private Map<Long, Fiat> balancesFiat = new HashMap<>();
    private float fiatRate;
    private String fiatCurrency;
    private String fiatExchange;
    private ArrayList subaccounts;

    private FutureCallback<LoginData> handleLoginData = new FutureCallback<LoginData>() {
        @Override
        public void onSuccess(@Nullable final LoginData result) {
            fiatCurrency = result.currency;
            fiatExchange = result.exchange;
            subaccounts = result.subaccounts;
            getLatestOrNewAddress(getSharedPreferences("receive", MODE_PRIVATE).getInt("curSubaccount", 0));
            balanceObservables.put(new Long(0), new GaObservable());
            updateBalance(0);
            for (Object subaccount : result.subaccounts) {
                Map<?, ?> subaccountMap = (Map) subaccount;
                long pointer = ((Number) subaccountMap.get("pointer")).longValue();
                balanceObservables.put(pointer, new GaObservable());
                updateBalance(pointer);
            }
            getAvailableTwoFacMethods();
            connectionObservable.setState(ConnectivityObservable.State.LOGGEDIN);
        }

        @Override
        public void onFailure(final Throwable t) {
            t.printStackTrace();
            connectionObservable.setState(ConnectivityObservable.State.CONNECTED);
        }
    };

    private Map<?, ?> twoFacConfig;
    private String deviceId;
    private int background_color;
    // fix me implement Preference change listener?
    // http://developer.android.com/guide/topics/ui/settings.html
    private int reconnectTimeout = 0;

    private WalletClient client;
    private ConnectivityObservable connectionObservable = null;

    private void getAvailableTwoFacMethods() {
        Futures.addCallback(client.getTwoFacConfig(), new FutureCallback<Map<?, ?>>() {
            @Override
            public void onSuccess(@Nullable final Map<?, ?> result) {
                twoFacConfig = result;
            }

            @Override
            public void onFailure(final Throwable t) {
                t.printStackTrace();
            }
        }, es);
    }

    public void resetSignUp() {
        latestMnemonics = null;
        latestQrBitmapMnemonics = null;
    }


    void reconnect() {
        Log.i("GaService", "Submitting reconnect in " + reconnectTimeout);
        onConnected = client.connect();
        connectionObservable.setState(ConnectivityObservable.State.CONNECTING);

        Futures.addCallback(onConnected, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable final Void result) {
                connectionObservable.setState(ConnectivityObservable.State.CONNECTED);

                Log.i("GaService", "Success CONNECTED callback");
                if (!connectionObservable.getIsForcedLoggedOut() && !connectionObservable.getIsForcedTimeout() && client.canLogin()) {

                    Futures.addCallback(onConnected, new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable final Void result) {
                            // FIXME: Maybe callback to UI to say we are good?
                            login();
                        }

                        @Override
                        public void onFailure(final Throwable t) {

                        }
                    }, es);
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.i("GaService", "Failure throwable callback " + t.toString());
                connectionObservable.setState(ConnectivityObservable.State.DISCONNECTED);

                if (reconnectTimeout < connectionObservable.RECONNECT_TIMEOUT_MAX) {
                    reconnectTimeout *= 1.2;
                }

                if (reconnectTimeout == 0) {
                    reconnectTimeout = connectionObservable.RECONNECT_TIMEOUT;
                }

                // FIXME: handle delayed login
                es.submit(new Runnable() {
                    @Override
                    public void run() {
                        reconnect();
                    }
                });
            }
        }, es);
    }

    public boolean isValidAddress(final String address) {
        try {
            new org.bitcoinj.core.Address(Network.NETWORK, address);
            return true;
        } catch (final AddressFormatException e) {
            return false;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // FIXME: kinda hacky... maybe better getting background color of the activity?
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.background_color = 0xffffff;
        } else {
            this.background_color = 0xeeeeee;
        }
        connectionObservable = ((GreenAddressApplication) getApplication()).getConnectionObservable();


        deviceId = getSharedPreferences("service", MODE_PRIVATE).getString("device_id", null);
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            final SharedPreferences.Editor editor = getSharedPreferences("service", MODE_PRIVATE).edit();
            editor.putString("device_id", deviceId);
        }

        client = new WalletClient(new INotificationHandler() {
            @Override
            public void onNewBlock(final long count) {
                Log.i("GaService", "onNewBlock");
                newTransactionsObservable.setChanged();
                newTransactionsObservable.notifyObservers();
            }

            @Override
            public void onNewTransaction(final long wallet_id, final long[] subaccounts, final long value, final String txhash) {
                Log.i("GaService", "onNewTransactions");
                newTransactionsObservable.setChanged();
                newTransactionsObservable.notifyObservers();
                for (long subaccount : subaccounts) {
                    updateBalance(subaccount);
                }
            }


            @Override
            public void onConnectionClosed(final int code) {
                if (code == 4000) {
                    connectionObservable.setForcedLoggedOut();
                }
                connectionObservable.setState(ConnectivityObservable.State.DISCONNECTED);


                if (!connectionObservable.isNetworkUp()) {
                    // do nothing
                    connectionObservable.setState(ConnectivityObservable.State.OFFLINE);

                } else {

                    connectionObservable.setState(ConnectivityObservable.State.DISCONNECTED);

                    // 4000 (concurrentLoginOnDifferentDeviceId) && 4001 (concurrentLoginOnSameDeviceId!)
                    Log.i("GaService", "onConnectionClosed code=" + String.valueOf(code));
                    // FIXME: some callback to UI so you see what's happening.
                    // 1000 NORMAL_CLOSE
                    // 1006 SERVER_RESTART
                    reconnectTimeout = 0;
                    if (reconnect) {
                        reconnect();
                    }
                }
            }
        }, es);
    }

    private ListenableFuture<LoginData> login() {
        connectionObservable.setState(ConnectivityObservable.State.LOGGINGIN);
        final ListenableFuture<LoginData> future = client.login(deviceId);
        Futures.addCallback(future, handleLoginData, es);
        return future;
    }

    public ListenableFuture<LoginData> login(final ISigningWallet signingWallet) {
        latestAddresses = new HashMap<>();
        connectionObservable.setState(ConnectivityObservable.State.LOGGINGIN);

        final ListenableFuture<LoginData> future = client.login(signingWallet, deviceId);
        Futures.addCallback(future, handleLoginData, es);
        return future;
    }

    public ListenableFuture<LoginData> login(final String mnemonics) {
        latestAddresses = new HashMap<>();
        connectionObservable.setState(ConnectivityObservable.State.LOGGINGIN);

        final ListenableFuture<LoginData> future = client.login(mnemonics, deviceId);
        Futures.addCallback(future, handleLoginData, es);
        return future;
    }

    public ListenableFuture<LoginData> signup(final String mnemonics) {
        latestAddresses = new HashMap<>();
        final ListenableFuture<LoginData> signupFuture = client.loginRegister(mnemonics, deviceId);
        connectionObservable.setState(ConnectivityObservable.State.LOGGINGIN);

        Futures.addCallback(signupFuture, handleLoginData, es);
        return signupFuture;
    }

    public String getMnemonics() {
        return client.getMnemonics();
    }

    public ListenableFuture<LoginData> pinLogin(final PinData pinData, final String pin) {
        latestAddresses = new HashMap<>();
        connectionObservable.setState(ConnectivityObservable.State.LOGGINGIN);

        final ListenableFuture<LoginData> login = client.pinLogin(pinData, pin, deviceId);
        Futures.addCallback(login, handleLoginData, es);
        return login;
    }

    public void disconnect(final boolean reconnect) {
        this.reconnect = reconnect;
        for (Long key : balanceObservables.keySet()) {
            balanceObservables.get(key).deleteObservers();
        }
        latestAddresses = new HashMap<>();
        client.disconnect();
        connectionObservable.setState(ConnectivityObservable.State.DISCONNECTED);
    }

    public ListenableFuture<Map<?, ?>> updateBalance(final long subaccount) {
        final ListenableFuture<Map<?, ?>> future = client.getBalance(subaccount);
        Futures.addCallback(future, new FutureCallback<Map<?, ?>>() {
            @Override
            public void onSuccess(@Nullable final Map<?, ?> result) {
                balancesCoin.put(subaccount, Coin.valueOf(Long.valueOf((String) result.get("satoshi")).longValue()));
                fiatRate = Float.valueOf((String) result.get("fiat_exchange")).floatValue();
                // Fiat.parseFiat uses toBigIntegerExact which requires at most 4 decimal digits,
                // while the server can return more, hence toBigInteger instead here:
                BigInteger fiatValue = new BigDecimal((String) result.get("fiat_value"))
                        .movePointRight(Fiat.SMALLEST_UNIT_EXPONENT).toBigInteger();
                // also strip extra decimals (over 2 places) because that's what the old JS client does
                fiatValue = fiatValue.subtract(fiatValue.mod(BigInteger.valueOf(10).pow(Fiat.SMALLEST_UNIT_EXPONENT - 2)));
                balancesFiat.put(subaccount, Fiat.valueOf((String) result.get("fiat_currency"), fiatValue.longValue()));

                fireBalanceChanged(subaccount);
            }

            @Override
            public void onFailure(final Throwable t) {

            }
        }, es);
        return future;
    }

    public ListenableFuture<Map<?, ?>> getSubaccountBalance(int pointer) {
        return client.getSubaccountBalance(pointer);
    }

    public void fireBalanceChanged(long subaccount) {
        balanceObservables.get(subaccount).setChanged();
        balanceObservables.get(subaccount).notifyObservers();
    }

    public ListenableFuture<Boolean> setPricingSource(final String currency, final String exchange) {
        return Futures.transform(client.setPricingSource(currency, exchange), new Function<Boolean, Boolean>() {
            @Override
            public Boolean apply(final Boolean input) {
                fiatCurrency = currency;
                fiatExchange = exchange;
                return input;
            }
        });
    }

    public ListenableFuture<Map<?, ?>> getMyTransactions(int subaccount) {
        return client.getMyTransactions(subaccount);
    }

    public ListenableFuture<PinData> setPin(final byte[] seed, final String mnemonic, final String pin, final String device_name) {
        return client.setPin(seed, mnemonic, pin, device_name);
    }

    public ListenableFuture<PreparedTransaction> prepareTx(final Coin coinValue, final String recipient, final Map<String, Object> privateData) {
        return client.prepareTx(coinValue.longValue(), recipient, "sender", privateData);
    }

    public ListenableFuture<String> signAndSendTransaction(final PreparedTransaction prepared, final Object twoFacData) {
        return Futures.transform(client.signTransaction(prepared, false), new AsyncFunction<List<String>, String>() {
            @Override
            public ListenableFuture<String> apply(final List<String> input) throws Exception {
                return client.sendTransaction(input, twoFacData);
            }
        }, es);
    }

    public ListenableFuture<String> sendTransaction(final List<TransactionSignature> signatures, final Object twoFacData) {
        final List<String> signaturesStrings = new LinkedList<>();
        for (final TransactionSignature sig : signatures) {
            signaturesStrings.add(new String(Hex.encode(sig.encodeToBitcoin())));
        }
        return client.sendTransaction(signaturesStrings, twoFacData);
    }

    public ListenableFuture<QrBitmap> getLatestOrNewAddress(long subaccount) {
        ListenableFuture<QrBitmap> latestAddress = latestAddresses.get(new Long(subaccount));
        return (latestAddress == null) ? getNewAddress(subaccount) : latestAddress;
    }

    public ListenableFuture<QrBitmap> getNewAddress(long subaccount) {
        final AsyncFunction<String, QrBitmap> addressToQr = new AsyncFunction<String, QrBitmap>() {
            @Override
            public ListenableFuture<QrBitmap> apply(final String input) {
                return es.submit(new QrBitmap(input, background_color));

            }
        };
        latestAddresses.put(subaccount, Futures.transform(client.getNewAddress(subaccount), addressToQr, es));
        return latestAddresses.get(new Long(subaccount));
    }

    public ListenableFuture<List<List<String>>> getCurrencyExchangePairs() {
        if (currencyExchangePairs == null) {
            currencyExchangePairs = Futures.transform(client.getAvailableCurrencies(), new Function<Map<?, ?>, List<List<String>>>() {
                @Override
                public List<List<String>> apply(final Map<?, ?> result) {
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
                        public int compare(final List<String> lhs, final List<String> rhs) {
                            return lhs.get(0).compareTo(rhs.get(0));
                        }
                    });
                    return ret;
                }
            }, es);
        }
        return currencyExchangePairs;
    }

    private static byte[] getRandomSeed() {
        final SecureRandom secureRandom = new SecureRandom();
        final byte[] seed = new byte[256 / 8];
        secureRandom.nextBytes(seed);
        return seed;
    }

    public ListenableFuture<String> getMnemonicPassphrase() {
        if (latestMnemonics == null) {
            latestMnemonics = es.submit(new Callable<String>() {
                public String call() throws IOException, MnemonicException.MnemonicLengthException {
                    final InputStream closable = getApplicationContext().getAssets().open("bip39-wordlist.txt");
                    try {
                        return TextUtils.join(" ",
                                new MnemonicCode(closable, null)
                                        .toMnemonic(getRandomSeed()));
                    } finally {
                        closable.close();
                    }
                }
            });
            getQrCodeForMnemonicPassphrase();
        }
        return latestMnemonics;
    }


    public byte[] getEntropyFromMnemonics(final String mnemonics) throws IOException, MnemonicException.MnemonicChecksumException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException {
        final InputStream closable = getApplicationContext().getAssets().open("bip39-wordlist.txt");
        try {
            return
                    new MnemonicCode(closable, null)
                            .toEntropy(Arrays.asList(mnemonics.split(" ")));
        } finally {
            closable.close();
        }
    }

    public ListenableFuture<QrBitmap> getQrCodeForMnemonicPassphrase() {
        if (latestQrBitmapMnemonics == null) {
            Futures.addCallback(latestMnemonics, new FutureCallback<String>() {
                @Override
                public void onSuccess(@Nullable final String mnemonic) {
                    latestQrBitmapMnemonics = es.submit(new QrBitmap(mnemonic, Color.WHITE));
                }

                @Override
                public void onFailure(final Throwable t) {

                }
            }, es);
        }
        return latestQrBitmapMnemonics;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public Map<Long, Observable> getBalanceObservables() {
        Map<Long, Observable> ret = new HashMap<>();
        for (Long key : balanceObservables.keySet()) {
            ret.put(key, balanceObservables.get(key));
        }
        return ret;
    }

    public Observable getNewTransactionsObservable() {
        return newTransactionsObservable;
    }

    public Coin getBalanceCoin(long subaccount) {
        return balancesCoin.get(new Long(subaccount));
    }

    public Fiat getBalanceFiat(long subaccount) {
        return balancesFiat.get(new Long(subaccount));
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

    public Object getAppearanceValue(final String key) {
        return client.getAppearenceValue(key);
    }

    public Map<?, ?> getTwoFacConfig() {
        return twoFacConfig;
    }

    /**
     * @param updateImmediately whether to not wait for server to reply before updating
     *                          the value in local settings dict (set false to wait)
     */
    public void setAppearanceValue(final String key, final Object value, final boolean updateImmediately) {
        client.setAppearanceValue(key, value, updateImmediately);
    }

    public void requestTwoFacCode(final String method, final String action) {
        client.requestTwoFacCode(method, action);
    }

    public ListenableFuture<Map<?, ?>> prepareSweepSocial(final byte[] pubKey, final boolean useElectrum) {
        return client.prepareSweepSocial(pubKey, useElectrum);
    }

    public ListenableFuture<Map<?, ?>> processBip70URL(final String url) {
        return client.processBip70URL(url);
    }

    public ListenableFuture<PreparedTransaction> preparePayreq(final Coin amount, final Map<?, ?> data, final Map<String, Object> privateData) {
        return client.preparePayreq(amount, data, privateData);
    }

    public boolean isBTChip() {
        return client.getHdWallet() instanceof BTChipHWWallet;
    }

    private static class GaObservable extends Observable {
        @Override
        public void setChanged() {
            super.setChanged();
        }
    }
}
