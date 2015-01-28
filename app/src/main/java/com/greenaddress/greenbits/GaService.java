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
import com.google.common.collect.ImmutableList;
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
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.BlockChainListener;
import org.bitcoinj.core.BloomFilter;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DownloadListener;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.PeerFilterProvider;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
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
import java.util.Formatter;
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
    private Map<Long, Coin> verifiedBalancesCoin = new HashMap<>();
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

    private Map<TransactionOutPoint, Long> unspentOutpointsSubaccounts;
    private Map<TransactionOutPoint, Long> unspentOutpointsPointers;
    private Map<TransactionOutPoint, Coin> countedUtxoValues;
    private Map<Sha256Hash, List<Long>> unspentOutputsOutpoints;
    private Map<Long, DeterministicKey> gaDeterministicKeys;
    private String receivingId;
    private byte[] gaitPath;

    private FutureCallback<LoginData> handleLoginData = new FutureCallback<LoginData>() {
        @Override
        public void onSuccess(@Nullable final LoginData result) {
            fiatCurrency = result.currency;
            fiatExchange = result.exchange;
            subaccounts = result.subaccounts;
            receivingId = result.receiving_id;
            gaitPath = Hex.decode(result.gait_path);

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

            unspentOutpointsSubaccounts = new HashMap<>();
            unspentOutpointsPointers = new HashMap<>();
            unspentOutputsOutpoints = new HashMap<>();
            countedUtxoValues = new HashMap<>();
            verifiedBalancesCoin = new HashMap<>();
            gaDeterministicKeys = new HashMap<>();
            setUpSPV();
            updateUnspentOutputs();
            connectionObservable.setState(ConnectivityObservable.State.LOGGEDIN);
        }

        @Override
        public void onFailure(final Throwable t) {
            t.printStackTrace();
            connectionObservable.setState(ConnectivityObservable.State.CONNECTED);
        }
    };

    private void updateUnspentOutputs() {
        Futures.addCallback(client.getAllUnspentOutputs(), new FutureCallback<ArrayList>() {
            @Override
            public void onSuccess(@Nullable ArrayList result) {
                Set<TransactionOutPoint> newUtxos = new HashSet<>();
                boolean recalculateBloom = false;

                for (int i = 0; i < result.size(); ++i) {
                    Map<?, ?> utxo = (Map) result.get(i);
                    String txhash = (String) utxo.get("txhash");
                    Integer blockHeight = (Integer) utxo.get("block_height");
                    final Long pt_idx = ((Number) utxo.get("pt_idx")).longValue();
                    final Sha256Hash sha256hash = new Sha256Hash(Hex.decode(txhash));
                    if (!getSharedPreferences("verified_utxo_" + receivingId, MODE_PRIVATE).getBoolean(txhash, false)) {
                        recalculateBloom = true;
                        addToBloomFilter(blockHeight, sha256hash, pt_idx, ((Number)utxo.get("subaccount")).longValue(),
                                ((Number)utxo.get("pointer")).longValue());
                    } else {
                        // already verified
                        addToUtxo(new Sha256Hash(txhash), pt_idx, ((Number)utxo.get("subaccount")).longValue(),
                                ((Number)utxo.get("pointer")).longValue());
                        addUtxoToValues(new Sha256Hash(txhash));
                    }
                    newUtxos.add(new TransactionOutPoint(Network.NETWORK, pt_idx, sha256hash));
                }

                List<Long> changedSubaccounts = new ArrayList<>();
                for (TransactionOutPoint oldUtxo : new HashSet<>(countedUtxoValues.keySet())) {
                    if (!newUtxos.contains(oldUtxo)) {
                        recalculateBloom = true;
                        Long subaccount = unspentOutpointsSubaccounts.get(oldUtxo);
                        verifiedBalancesCoin.put(subaccount,
                                verifiedBalancesCoin.get(subaccount).subtract(countedUtxoValues.get(oldUtxo)));
                        changedSubaccounts.add(subaccount);
                        countedUtxoValues.remove(oldUtxo);
                        unspentOutpointsSubaccounts.remove(oldUtxo);
                        unspentOutpointsPointers.remove(oldUtxo);
                        unspentOutputsOutpoints.get(oldUtxo.getHash()).remove(oldUtxo.getIndex());
                    }
                }

                if (recalculateBloom) {
                    peerGroup.recalculateFastCatchupAndFilter(PeerGroup.FilterRecalculateMode.SEND_IF_CHANGED);
                }

                for (Long subaccount : changedSubaccounts) {
                    fireBalanceChanged(subaccount);
                }

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
    }

    private int spvBlocksLeft;

    private PeerFilterProvider makePeerFilterProvider() {
        pfProvider = new PeerFilterProvider() {
            @Override
            public long getEarliestKeyCreationTime() {
                return 1393628400;
            }

            @Override
            public int getBloomFilterElementCount() {
                return unspentOutputsOutpoints.size();
            }

            @Override
            public BloomFilter getBloomFilter(int size, double falsePositiveRate, long nTweak) {
                final byte[][] hashes = new byte[unspentOutputsOutpoints.size()][];

                int i = 0;
                for (Sha256Hash hash : unspentOutputsOutpoints.keySet()) {
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
                addToBloomFilter((int) count, null, -1, -1, -1);
                newTransactionsObservable.setChanged();
                newTransactionsObservable.notifyObservers();
            }

            @Override
            public void onNewTransaction(final long wallet_id, final long[] subaccounts, final long value, final String txhash) {
                Log.i("GaService", "onNewTransactions");
                updateUnspentOutputs();
                newTransactionsObservable.setChanged();
                newTransactionsObservable.notifyObservers();
                for (long subaccount : subaccounts) {
                    updateBalance(subaccount);
                }
            }


            @Override
            public void onConnectionClosed(final int code) {
                gaDeterministicKeys = null;

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
                return unspentOutputsOutpoints.keySet().contains(tx.getHash());
            }

            @Override
            public void receiveFromBlock(Transaction tx, StoredBlock block, AbstractBlockChain.NewBlockType blockType, int relativityOffset) throws VerificationException {
                // FIXME: later spent outputs can be purged
                SharedPreferences verified_utxo = getSharedPreferences("verified_utxo_"+receivingId, MODE_PRIVATE);
                SharedPreferences.Editor editor = verified_utxo.edit();
                editor.putBoolean(tx.getHash().toString(), true);
                editor.commit();
                addUtxoToValues(tx.getHash());
                newTxVerifiedObservable.setChanged();
                newTxVerifiedObservable.notifyObservers();
            }

            @Override
            public boolean notifyTransactionIsInBlock(Sha256Hash txHash, StoredBlock block, AbstractBlockChain.NewBlockType blockType, int relativityOffset) throws VerificationException {
                // FIXME: later spent outputs can be purged
                SharedPreferences verified_utxo = getSharedPreferences("verified_utxo_"+receivingId, MODE_PRIVATE);
                SharedPreferences.Editor editor = verified_utxo.edit();
                editor.putBoolean(txHash.toString(), true);
                editor.commit();
                addUtxoToValues(txHash);
                newTxVerifiedObservable.setChanged();
                newTxVerifiedObservable.notifyObservers();
                return unspentOutputsOutpoints.keySet().contains(txHash);
            }
        };
        return blockChainListener;
    }

    private void addUtxoToValues(final TransactionOutPoint txOutpoint, long subaccount, Coin addValue) {
        if (countedUtxoValues.keySet().contains(txOutpoint)) return;
        countedUtxoValues.put(txOutpoint, addValue);
        if (verifiedBalancesCoin.get(subaccount) == null) {
            verifiedBalancesCoin.put(subaccount, addValue);
        } else {
            verifiedBalancesCoin.put(subaccount,
                    verifiedBalancesCoin.get(subaccount).add(addValue));
        }

    }

    private void addUtxoToValues(final Sha256Hash txHash) {
        final String txHashStr = txHash.toString();
        final List<Long> changedSubaccounts = new ArrayList<>();
        boolean missing = false;
        for (final Long outpoint : unspentOutputsOutpoints.get(txHash)) {
            String outPointStr = txHashStr + ":" + outpoint;
            if (getSharedPreferences("verified_utxo_spendable_value_" + receivingId, MODE_PRIVATE).getLong(outPointStr, -1) != -1) {
                final long value = getSharedPreferences("verified_utxo_spendable_value_" + receivingId, MODE_PRIVATE).getLong(outPointStr, -1);
                final TransactionOutPoint txOutpoint = new TransactionOutPoint(Network.NETWORK, outpoint, txHash);
                final Long subaccount = unspentOutpointsSubaccounts.get(txOutpoint);
                if (!countedUtxoValues.keySet().contains(txOutpoint)) changedSubaccounts.add(subaccount);
                addUtxoToValues(txOutpoint, subaccount, Coin.valueOf(value));
            } else {
                missing = true;
            }
        }
        for (final Long subaccount : changedSubaccounts) {
            fireBalanceChanged(subaccount);
        }
        if (!missing) return;
        Futures.addCallback(client.getRawUnspentOutput(txHash), new FutureCallback<Transaction>() {
            @Override
            public void onSuccess(@Nullable final Transaction result) {
                final List<Long> changedSubaccounts = new ArrayList<>();
                final List<ListenableFuture<Boolean>> futuresList = new ArrayList<>();
                if (result.getHash().equals(txHash)) {
                    for (final Long outpoint : unspentOutputsOutpoints.get(txHash)) {
                        final TransactionOutPoint txOutpoint = new TransactionOutPoint(Network.NETWORK, outpoint, txHash);
                        if (countedUtxoValues.keySet().contains(txOutpoint)) continue;
                        final Long subaccount = unspentOutpointsSubaccounts.get(txOutpoint);
                        final Long pointer = unspentOutpointsPointers.get(txOutpoint);

                        futuresList.add(Futures.transform(verifySpendableBy(result.getOutput(outpoint.intValue()), subaccount, pointer), new Function<Boolean, Boolean>() {
                            @Nullable
                            @Override
                            public Boolean apply(@Nullable Boolean input) {
                                if (input.booleanValue()) {
                                    final Coin coinValue = result.getOutput(outpoint.intValue()).getValue();
                                    addUtxoToValues(txOutpoint, subaccount, coinValue);
                                    changedSubaccounts.add(subaccount);
                                    SharedPreferences.Editor e = getSharedPreferences("verified_utxo_spendable_value_" + receivingId, MODE_PRIVATE).edit();
                                    e.putLong(txHashStr + ":" + outpoint, coinValue.longValue());
                                    e.commit();
                                } else {
                                    Log.e("SPV",
                                            new Formatter().format("txHash %s outpoint %s not spendable!",
                                                    txHash.toString(), outpoint.toString()).toString());
                                }
                                return input;
                            }
                        }));
                    }
                } else {
                    Log.e("SPV",
                            new Formatter().format("txHash mismatch: expected %s != %s received",
                                    txHash.toString(), result.getHash().toString()).toString());
                }
                Futures.addCallback(Futures.allAsList(futuresList), new FutureCallback<List<Boolean>>() {
                    @Override
                    public void onSuccess(@Nullable List<Boolean> result) {
                        for (Long subaccount : changedSubaccounts) {
                            fireBalanceChanged(subaccount);
                        }
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
    }

    private ListenableFuture<Boolean> verifyP2SHSpendableBy(final Script scriptHash, final Long subaccount, final Long pointer) {
        if (!scriptHash.isPayToScriptHash()) return Futures.immediateFuture(false);
        final byte[] gotP2SH = scriptHash.getPubKeyHash();

        final List<ECKey> pubkeys = new ArrayList<>();
        DeterministicKey gaWallet = getGaDeterministicKey(subaccount);
        ECKey gaKey = HDKeyDerivation.deriveChildKey(gaWallet, new ChildNumber(pointer.intValue()));
        pubkeys.add(gaKey);

        ISigningWallet userWallet = client.getHdWallet();
        if (subaccount.longValue() != 0) {
            userWallet = userWallet.deriveChildKey(new ChildNumber(3, true));
            userWallet = userWallet.deriveChildKey(new ChildNumber(subaccount.intValue(), true));
        }
        userWallet = userWallet.deriveChildKey(new ChildNumber(1));
        userWallet = userWallet.deriveChildKey(new ChildNumber(pointer.intValue()));

        return Futures.transform(userWallet.getPubKey(), new Function<ECKey, Boolean>() {
            @Nullable
            @Override
            public Boolean apply(@Nullable ECKey input) {
                pubkeys.add(input);

                String twoOfThreeBackupChaincode = null, twoOfThreeBackupPubkey = null;
                for (Object subaccount_ : subaccounts) {
                    Map <String, ?> subaccountMap = (Map) subaccount_;
                    if (subaccountMap.get("type").equals("2of3") && subaccountMap.get("pointer").equals(subaccount.intValue())) {
                        twoOfThreeBackupChaincode = (String) subaccountMap.get("2of3_backup_chaincode");
                        twoOfThreeBackupPubkey = (String) subaccountMap.get("2of3_backup_pubkey");
                    }
                }

                if (twoOfThreeBackupChaincode != null) {
                    DeterministicKey backupWallet = new DeterministicKey(
                            new ImmutableList.Builder<ChildNumber>().build(),
                            Hex.decode(twoOfThreeBackupChaincode),
                            ECKey.fromPublicOnly(Hex.decode(twoOfThreeBackupPubkey)).getPubKeyPoint(),
                            null, null);
                    backupWallet = HDKeyDerivation.deriveChildKey(backupWallet, new ChildNumber(1));
                    backupWallet = HDKeyDerivation.deriveChildKey(backupWallet, new ChildNumber(pointer.intValue()));
                    pubkeys.add(backupWallet);
                }

                byte[] expectedP2SH = Utils.sha256hash160(Script.createMultiSigOutputScript(2, pubkeys));
                return Arrays.equals(gotP2SH, expectedP2SH);
            }
        });
    }

    private ListenableFuture<Boolean> verifySpendableBy(final TransactionOutput txOutput, final Long subaccount, final Long pointer) {
        return verifyP2SHSpendableBy(txOutput.getScriptPubKey(), subaccount, pointer);
    }

    private DeterministicKey getGaDeterministicKey(Long subaccount) {
        if (gaDeterministicKeys.keySet().contains(subaccount)) {
            return gaDeterministicKeys.get(subaccount);
        }
        DeterministicKey gaWallet = new DeterministicKey(
                new ImmutableList.Builder<ChildNumber>().build(),
                Hex.decode(Network.depositChainCode),
                ECKey.fromPublicOnly(Hex.decode(Network.depositPubkey)).getPubKeyPoint(),
                null, null);
        if (subaccount.longValue() != 0) {
            gaWallet = HDKeyDerivation.deriveChildKey(gaWallet, new ChildNumber(3));
        } else {
            gaWallet = HDKeyDerivation.deriveChildKey(gaWallet, new ChildNumber(1));
        }
        int childNum;
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
            gaWallet = HDKeyDerivation.deriveChildKey(gaWallet, new ChildNumber(childNum));
        }
        if (subaccount.longValue() != 0 ) {
            gaWallet = HDKeyDerivation.deriveChildKey(gaWallet, new ChildNumber(subaccount.intValue(), false));
        }
        gaDeterministicKeys.put(subaccount, gaWallet);
        return gaWallet;
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
        if (balancesCoin != null && getBalanceCoin(subaccount) != null) {  // can be null if called from addUtxoToValues before balance is fetched
            balanceObservables.get(subaccount).setChanged();
            balanceObservables.get(subaccount).notifyObservers();
        }
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

    public ListenableFuture<Map<?, ?>> getMyTransactions(final int subaccount) {
        return client.getMyTransactions(subaccount);
    }

    private void addToBloomFilter(final Integer blockHeight, Sha256Hash txhash, final long pt_idx, final long subaccount, final long pointer) {
        if (txhash != null) {
            addToUtxo(txhash, pt_idx, subaccount, pointer);
        }
        if (blockHeight != null && blockHeight <= blockChain.getBestChainHeight() &&
                (txhash == null || !unspentOutputsOutpoints.keySet().contains(txhash))) {
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

    private void addToUtxo(Sha256Hash txhash, long pt_idx, long subaccount, long pointer) {
        unspentOutpointsSubaccounts.put(new TransactionOutPoint(Network.NETWORK, pt_idx, txhash), subaccount);
        unspentOutpointsPointers.put(new TransactionOutPoint(Network.NETWORK, pt_idx, txhash), pointer);
        if (unspentOutputsOutpoints.get(txhash) == null) {
            ArrayList<Long> newList = new ArrayList<>();
            newList.add(pt_idx);
            unspentOutputsOutpoints.put(txhash, newList);
        } else {
            unspentOutputsOutpoints.get(txhash).add(pt_idx);
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

    public ListenableFuture<QrBitmap> getNewAddress(final long subaccount) {
        final AsyncFunction<Map, String> verifyAddress = new AsyncFunction<Map, String>() {
            @Override
            public ListenableFuture<String> apply(Map input) throws Exception {
                final int pointer = ((Number) input.get("pointer")).intValue();
                final byte[] scriptHash = Utils.sha256hash160(Hex.decode((String) input.get("script")));
                return Futures.transform(verifyP2SHSpendableBy(
                        ScriptBuilder.createP2SHOutputScript(scriptHash),
                        subaccount, Long.valueOf(pointer)), new Function<Boolean, String>() {
                    @Nullable
                    @Override
                    public String apply(@Nullable Boolean input) {
                        if (input.booleanValue()) {
                            return Address.fromP2SHHash(Network.NETWORK, scriptHash).toString();
                        } else {
                            throw new IllegalArgumentException("Address validation failed");
                        }
                    }
                });
            }
        };
        final AsyncFunction<String, QrBitmap> addressToQr = new AsyncFunction<String, QrBitmap>() {
            @Override
            public ListenableFuture<QrBitmap> apply(final String input) {
                return es.submit(new QrBitmap(input, background_color));

            }
        };
        ListenableFuture<String> verifiedAddress = Futures.transform(client.getNewAddress(subaccount), verifyAddress, es);
        latestAddresses.put(subaccount, Futures.transform(verifiedAddress, addressToQr, es));
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

    public Coin getVerifiedBalanceCoin(int subaccount) {
        return verifiedBalancesCoin.get(new Long(subaccount));
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
