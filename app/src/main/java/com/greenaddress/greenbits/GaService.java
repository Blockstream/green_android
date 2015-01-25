package com.greenaddress.greenbits;

import android.app.Service;
import android.content.Context;
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

import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.BlockChainListener;
import org.bitcoinj.core.BloomFilter;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DownloadListener;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.PeerFilterProvider;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.utils.Fiat;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nullable;


public class GaService extends Service {

    public final ListeningExecutorService es = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(3));

    private final IBinder mBinder = new GaBinder(this);
    final private Map<Long, GaObservable> balanceObservables = new HashMap<>();
    final private GaObservable newTransactionsObservable = new GaObservable();
    final private GaObservable newTxVerifiedObservable = new GaObservable();
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
    private BlockStore blockStore;
    private BlockChain blockChain;
    private BlockChainListener blockChainListener;
    private PeerGroup peerGroup;
    private PeerFilterProvider pfProvider;
    private Set<Sha256Hash> unspentOutputs;
    private String receivingId;

    private FutureCallback<LoginData> handleLoginData = new FutureCallback<LoginData>() {
        @Override
        public void onSuccess(@Nullable final LoginData result) {
            fiatCurrency = result.currency;
            fiatExchange = result.exchange;
            subaccounts = result.subaccounts;
            receivingId = result.receiving_id;

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

            unspentOutputs = new HashSet<>();
            setUpSPV();
            Futures.addCallback(client.getAllUnspentOutputs(), new FutureCallback<ArrayList>() {
                @Override
                public void onSuccess(@Nullable ArrayList result) {

                    final int count = result.size();

                    for (int i = 0; i < count; ++i) {
                        Map<?, ?> utxo = (Map) result.get(i);
                        unspentOutputs.add(new Sha256Hash(Hex.decode((String) utxo.get("txhash"))));
                    }
                    peerGroup.recalculateFastCatchupAndFilter(PeerGroup.FilterRecalculateMode.SEND_IF_CHANGED);

                    Futures.addCallback(peerGroup.start(), new FutureCallback<Object>() {
                        @Override
                        public void onSuccess(@Nullable Object result) {
                            peerGroup.startBlockChainDownload(new DownloadListener() {
                                @Override
                                public void onChainDownloadStarted(Peer peer, int blocksLeft) {
                                    spvBlocksLeft = blocksLeft;
                                }

                                @Override
                                public void onBlocksDownloaded(Peer peer, Block block, int blocksLeft) {
                                    spvBlocksLeft = blocksLeft;
                                }
                            });
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            t.printStackTrace();
                        }
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    t.printStackTrace();
                }
            });

            connectionObservable.setState(ConnectivityObservable.State.LOGGEDIN);
        }

        @Override
        public void onFailure(final Throwable t) {
            t.printStackTrace();
            connectionObservable.setState(ConnectivityObservable.State.CONNECTED);
        }
    };
    private int spvBlocksLeft;

    private PeerFilterProvider makePeerFilterProvider() {
        pfProvider = new PeerFilterProvider() {
            @Override
            public long getEarliestKeyCreationTime() {
                return 1393628400;
            }

            @Override
            public int getBloomFilterElementCount() {
                return unspentOutputs.size();
            }

            @Override
            public BloomFilter getBloomFilter(int size, double falsePositiveRate, long nTweak) {
                final byte[][] hashes = new byte[unspentOutputs.size()][];

                int i = 0;
                for (Sha256Hash hash : unspentOutputs) {
                    hashes[i++] = Utils.reverseBytes(hash.getBytes());
                }

                BloomFilter res = new BloomFilter(size, falsePositiveRate, nTweak);
                for (i = 0; i < hashes.length; ++i) {
                    res.insert(hashes[i]);
                }
                return res;
            }

            @Override
            public boolean isRequiringUpdateAllBloomFilter() {
                return false;
            }

            @Override
            public Lock getLock() {
                return new ReentrantLock();
            }
        };
        return pfProvider;
    }

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
                addToBloomFilter(null, new Sha256Hash(txhash));
                newTransactionsObservable.setChanged();
                newTransactionsObservable.notifyObservers();
                for (long subaccount : subaccounts) {
                    updateBalance(subaccount);
                }
            }


            @Override
            public void onConnectionClosed(final int code) {
                if (blockChain != null) {
                    blockChain.removeListener(blockChainListener);
                    blockChainListener = null;

                    peerGroup.removePeerFilterProvider(pfProvider);
                    pfProvider = null;

                    peerGroup.stopAsync();
                    peerGroup.awaitTerminated();
                    peerGroup = null;

                    try {
                        blockStore.close();
                        blockStore = null;
                    } catch (final BlockStoreException x) {
                        throw new RuntimeException(x);
                    }
                }


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

    private void setUpSPV() {
        File blockChainFile = new File(getDir("blockstore_"+receivingId, Context.MODE_PRIVATE), "blockchain.spvchain");

        try {
            blockStore = new SPVBlockStore(Network.NETWORK, blockChainFile);
            blockStore.getChainHead(); // detect corruptions as early as possible

            blockChain = new BlockChain(Network.NETWORK, blockStore);
            blockChain.addListener(makeBlockChainListener());

            peerGroup = new PeerGroup(Network.NETWORK, blockChain);
            peerGroup.addPeerFilterProvider(makePeerFilterProvider());

            if (Network.NETWORK.getId().equals(NetworkParameters.ID_REGTEST)) {
                try {
                    peerGroup.addAddress(new PeerAddress(InetAddress.getByName("192.168.56.1"), 19000));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                peerGroup.setMaxConnections(1);
            } else {
                peerGroup.addPeerDiscovery(new DnsDiscovery(Network.NETWORK));
            }
        } catch (BlockStoreException e) {
            e.printStackTrace();
        }

    }

    private BlockChainListener makeBlockChainListener() {
        blockChainListener = new BlockChainListener() {
            @Override
            public void notifyNewBestBlock(StoredBlock block) throws VerificationException {

            }

            @Override
            public void reorganize(StoredBlock splitPoint, List<StoredBlock> oldBlocks, List<StoredBlock> newBlocks) throws VerificationException {

            }

            @Override
            public boolean isTransactionRelevant(Transaction tx) throws ScriptException {
                return unspentOutputs.contains(tx.getHash());
            }

            @Override
            public void receiveFromBlock(Transaction tx, StoredBlock block, AbstractBlockChain.NewBlockType blockType, int relativityOffset) throws VerificationException {
                // FIXME: atm addToBloomFilter is called on tx notification even for outgoing txs,
                // so verified_utxo can contain unnecessary outputs too
                SharedPreferences verified_utxo = getSharedPreferences("verified_utxo_"+receivingId, MODE_PRIVATE);
                SharedPreferences.Editor editor = verified_utxo.edit();
                editor.putBoolean(tx.getHash().toString(), true);
                editor.commit();
                newTxVerifiedObservable.setChanged();
                newTxVerifiedObservable.notifyObservers();
            }

            @Override
            public boolean notifyTransactionIsInBlock(Sha256Hash txHash, StoredBlock block, AbstractBlockChain.NewBlockType blockType, int relativityOffset) throws VerificationException {
                // FIXME: atm addToBloomFilter is called on tx notification even for outgoing txs,
                // so verified_utxo can contain unnecessary outputs too
                SharedPreferences verified_utxo = getSharedPreferences("verified_utxo_"+receivingId, MODE_PRIVATE);
                SharedPreferences.Editor editor = verified_utxo.edit();
                editor.putBoolean(txHash.toString(), true);
                editor.commit();
                newTxVerifiedObservable.setChanged();
                newTxVerifiedObservable.notifyObservers();
                return unspentOutputs.contains(txHash);
            }
        };
        return blockChainListener;
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
        return Futures.transform(client.getMyTransactions(subaccount), new Function<Map<?, ?>, Map<?, ?>>() {
            @Nullable
            @Override
            public Map<?, ?> apply(@Nullable Map<?, ?> input) {
                final List resultList = (List) input.get("list");

                if (resultList != null) {
                    for (int i = 0; i < resultList.size(); ++i) {
                        Map<String, Object> txJSON = (Map<String, Object>) resultList.get(i);
                        final Integer blockHeight = txJSON.containsKey("block_height") && txJSON.get("block_height") != null?
                                (int) txJSON.get("block_height"): null;
                        final Sha256Hash txhash = new Sha256Hash((String) txJSON.get("txhash"));
                        addToBloomFilter(blockHeight, txhash);
                    }

                }

                return input;
            }
        });
    }

    private void addToBloomFilter(final Integer blockHeight, Sha256Hash txhash) {
        if (txhash != null) {
            unspentOutputs.add(txhash);
        }
        peerGroup.recalculateFastCatchupAndFilter(PeerGroup.FilterRecalculateMode.SEND_IF_CHANGED);
        if (blockHeight != null && blockHeight <= blockChain.getBestChainHeight() &&
                (txhash == null || !unspentOutputs.contains(txhash))) {
            // new tx or block notification with blockHeight <= current blockHeight means we might've [1]
            // synced the height already while we haven't seen the tx, so we need to re-sync to be able
            // to verify it.
            // [1] - "might've" in case of txhash == null (block height notification),
            //       because it depends on the order of notifications
            //     - "must've" in case of txhash != null, because this means the tx arrived only after
            //       requesting it manually and we already had higher blockHeight
            //
            // We do it using the special case in bitcoinj for VM crashed because of
            // a transaction received.
            Wallet fakeWallet = new Wallet(Network.NETWORK) {
                @Override
                public int getLastBlockSeenHeight() {
                    return blockHeight.intValue() - 1;
                }
            };
            blockChain.addWallet(fakeWallet);
            blockChain.removeWallet(fakeWallet);  // can be removed, because the call above
                                                  // should rollback already
        }
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

    public Observable getNewTxVerifiedObservable() {
        return newTxVerifiedObservable;
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

    public String getReceivingId() {
        return receivingId;
    }

    public int getSpvBlocksLeft() {
        return spvBlocksLeft;
    }

    private static class GaObservable extends Observable {
        @Override
        public void setChanged() {
            super.setChanged();
        }
    }
}
