package com.greenaddress.greenbits.spv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.PreparedTransaction;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.TabbedMainActivity;
import com.subgraph.orchid.TorClient;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.CheckpointManager;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.net.BlockingClientManager;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SPV {

    private final Map<TransactionOutPoint, Coin> countedUtxoValues = new HashMap<>();

    private final String VERIFIED = "verified_utxo_";
    private final String SPENDABLE = "verified_utxo_spendable_value_";

    public void startIfEnabled() {
        isSpvSyncing = false;
        if (gaService.isSPVEnabled()) {
            setUpSPV();

            if (startSpvAfterInit) {
                startSpvSync();
            }
        }
        startSpvAfterInit = false;
        updateUnspentOutputs();
    }

    private void addUtxoToValues(final TransactionOutPoint txOutpoint, final int subaccount, final Coin addValue) {
        if (countedUtxoValues.keySet().contains(txOutpoint)) return;
        countedUtxoValues.put(txOutpoint, addValue);
        if (verifiedBalancesCoin.get(subaccount) == null) {
            verifiedBalancesCoin.put(subaccount, addValue);
        } else {
            verifiedBalancesCoin.put(subaccount,
                    verifiedBalancesCoin.get(subaccount).add(addValue));
        }

    }

    public void resetSpv() {

        // delete all spv data
        final File blockChainFile = new File(gaService.getDir("blockstore_" + gaService.getReceivingId(), Context.MODE_PRIVATE), "blockchain.spvchain");
        if (blockChainFile.exists()) {
            blockChainFile.delete();
        }

        try {
            gaService.cfgInEdit(SPENDABLE).clear().commit();
            gaService.cfgInEdit(VERIFIED).clear().commit();
        } catch (final NullPointerException e) {
            // ignore
        }

        resetUnspent();
    }

    @NonNull
    public final Map<Integer, Coin> verifiedBalancesCoin = new HashMap<>();

    public void updateUnspentOutputs() {
        if (!gaService.isSPVEnabled())
            return;

        Futures.addCallback(gaService.getClient().getAllUnspentOutputs(), new FutureCallback<ArrayList>() {
            @Override
            public void onSuccess(final @Nullable ArrayList result) {
                final Set<TransactionOutPoint> newUtxos = new HashSet<>();
                boolean recalculateBloom = false;

                for (int i = 0; i < result.size(); ++i) {
                    final Map<?, ?> utxo = (Map) result.get(i);
                    final String txhash = (String) utxo.get("txhash");
                    final Integer blockHeight = (Integer) utxo.get("block_height");
                    final Integer pt_idx = ((Integer) utxo.get("pt_idx"));
                    final Sha256Hash sha256hash = Sha256Hash.wrap(Hex.decode(txhash));
                    if (!gaService.cfgIn(VERIFIED).getBoolean(txhash, false)) {
                        recalculateBloom = true;
                        addToBloomFilter(blockHeight, sha256hash, pt_idx, ((Integer) utxo.get("subaccount")),
                                ((Integer) utxo.get("pointer")));
                    } else {
                        // already verified
                        addToUtxo(Sha256Hash.wrap(txhash), pt_idx, ((Integer) utxo.get("subaccount")),
                                ((Integer) utxo.get("pointer")));
                        addUtxoToValues(Sha256Hash.wrap(txhash));
                    }
                    newUtxos.add(new TransactionOutPoint(Network.NETWORK, pt_idx, sha256hash));
                }

                final List<Integer> changedSubaccounts = new ArrayList<>();
                for (final TransactionOutPoint oldUtxo : new HashSet<>(countedUtxoValues.keySet())) {
                    if (!newUtxos.contains(oldUtxo)) {
                        recalculateBloom = true;
                        final Integer subaccount = unspentOutpointsSubaccounts.get(oldUtxo);
                        verifiedBalancesCoin.put(subaccount,
                                verifiedBalancesCoin.get(subaccount).subtract(countedUtxoValues.get(oldUtxo)));
                        changedSubaccounts.add(subaccount);
                        countedUtxoValues.remove(oldUtxo);
                        unspentOutpointsSubaccounts.remove(oldUtxo);
                        unspentOutpointsPointers.remove(oldUtxo);
                        unspentOutputsOutpoints.get(oldUtxo.getHash()).remove(((int) oldUtxo.getIndex()));
                    }
                }

                recalculate(recalculateBloom);

                for (final Integer subaccount : changedSubaccounts) {
                    gaService.fireBalanceChanged(subaccount);
                }
            }

            @Override
            public void onFailure(@NonNull final Throwable t) {
                t.printStackTrace();
            }
        });
    }
    public void resetUnspent() {
        unspentOutpointsSubaccounts.clear();
        unspentOutpointsPointers.clear();
        unspentOutputsOutpoints.clear();
        countedUtxoValues.clear();
        verifiedBalancesCoin.clear();
    }

    public void addUtxoToValues(@NonNull final Sha256Hash txHash) {
        final String txHashStr = txHash.toString();
        final List<Integer> changedSubaccounts = new ArrayList<>();
        boolean missing = false;
        for (final Integer outpoint : unspentOutputsOutpoints.get(txHash)) {
            final String key = txHashStr + ":" + outpoint;
            final long value = gaService.cfgIn(SPENDABLE).getLong(key, -1);
            if (value != -1) {
                final TransactionOutPoint txOutpoint = new TransactionOutPoint(Network.NETWORK, outpoint, txHash);
                final Integer subaccount = unspentOutpointsSubaccounts.get(txOutpoint);
                if (!countedUtxoValues.keySet().contains(txOutpoint))
                    changedSubaccounts.add(subaccount);
                addUtxoToValues(txOutpoint, subaccount, Coin.valueOf(value));
            } else {
                missing = true;
            }
        }
        for (final Integer subaccount : changedSubaccounts) {
            gaService.fireBalanceChanged(subaccount);
        }
        if (!missing) return;
        Futures.addCallback(gaService.getClient().getRawUnspentOutput(txHash), new FutureCallback<Transaction>() {
            @Override
            public void onSuccess(@Nullable final Transaction result) {
                final List<Integer> changedSubaccounts = new ArrayList<>();
                final List<ListenableFuture<Boolean>> futuresList = new ArrayList<>();
                if (result.getHash().equals(txHash)) {
                    for (final Integer outpoint : unspentOutputsOutpoints.get(txHash)) {
                        final TransactionOutPoint txOutpoint = new TransactionOutPoint(Network.NETWORK, outpoint, txHash);
                        if (countedUtxoValues.keySet().contains(txOutpoint))
                            continue;
                        final Integer subaccount = unspentOutpointsSubaccounts.get(txOutpoint);
                        final Integer pointer = unspentOutpointsPointers.get(txOutpoint);

                        futuresList.add(Futures.transform(gaService.verifySpendableBy(result.getOutput(outpoint), subaccount, pointer), new Function<Boolean, Boolean>() {
                            @Nullable
                            @Override
                            public Boolean apply(final @Nullable Boolean input) {
                                if (input) {
                                    final Coin value = result.getOutput(outpoint).getValue();
                                    addUtxoToValues(txOutpoint, subaccount, value);
                                    changedSubaccounts.add(subaccount);
                                    final String key = txHashStr + ":" + outpoint;
                                    gaService.cfgInEdit(SPENDABLE).putLong(key, value.longValue()).apply();
                                } else {
                                    Log.e(TAG,
                                            new Formatter().format("txHash %s outpoint %s not spendable!",
                                                    txHash.toString(), outpoint.toString()).toString());
                                }
                                return input;
                            }
                        }));
                    }
                } else {
                    Log.e(TAG,
                            new Formatter().format("txHash mismatch: expected %s != %s received",
                                    txHash.toString(), result.getHash().toString()).toString());
                }
                Futures.addCallback(Futures.allAsList(futuresList), new FutureCallback<List<Boolean>>() {
                    @Override
                    public void onSuccess(final @Nullable List<Boolean> result) {
                        for (final Integer subaccount : changedSubaccounts) {
                            gaService.fireBalanceChanged(subaccount);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull final Throwable t) {
                        t.printStackTrace();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull final Throwable t) {
                t.printStackTrace();
            }
        });
    }


    public void addToBloomFilter(@Nullable final Integer blockHeight, @Nullable final Sha256Hash txhash, final int pt_idx, final int subaccount, final int pointer) {
        if (blockChain == null) return; // can happen before login (onNewBlock)
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
            final Wallet fakeWallet = new Wallet(Network.NETWORK) {
                @Override
                public int getLastBlockSeenHeight() {
                    return blockHeight - 1;
                }
            };
            blockChain.addWallet(fakeWallet);
            blockChain.removeWallet(fakeWallet);  // can be removed, because the call above
            // should rollback already
        }
    }
    public final Map<Sha256Hash, List<Integer>> unspentOutputsOutpoints = new HashMap<>();
    private final Map<TransactionOutPoint, Integer> unspentOutpointsSubaccounts = new HashMap<>();
    private final Map<TransactionOutPoint, Integer> unspentOutpointsPointers = new HashMap<>();
    private void addToUtxo(final Sha256Hash txhash, final int pt_idx, final int subaccount, final int pointer) {
        unspentOutpointsSubaccounts.put(new TransactionOutPoint(Network.NETWORK, pt_idx, txhash), subaccount);
        unspentOutpointsPointers.put(new TransactionOutPoint(Network.NETWORK, pt_idx, txhash), pointer);
        if (unspentOutputsOutpoints.get(txhash) == null) {
            final ArrayList<Integer> newList = new ArrayList<>();
            newList.add(pt_idx);
            unspentOutputsOutpoints.put(txhash, newList);
        } else {
            unspentOutputsOutpoints.get(txhash).add(pt_idx);
        }
    }

    public boolean spvWiFiDialogShown = false;

    private final GaService gaService;
    @Nullable
    private BlockChainListener blockChainListener;
    @NonNull private final static String TAG = SPV.class.getSimpleName();
    public SPV(final GaService gaService) {
        this.gaService = gaService;
    }
    private enum SPVMode {
        ONION, TRUSTED, NORMAL
    }
    @NonNull
    public ListenableFuture<Coin> validateTxAndCalculateFeeOrAmount(@NonNull final PreparedTransaction transaction, @NonNull final String recipientStr, @NonNull final Coin amount) {
        Address recipientNonFinal = null;
        try {
            recipientNonFinal = new Address(Network.NETWORK, recipientStr);
        } catch (@NonNull final AddressFormatException e) {
        }
        final Address recipient = recipientNonFinal;

        // 1. Find the change output:
        ListenableFuture<List<Boolean>> changeFuture;
        if (transaction.decoded.getOutputs().size() > 1) {
            if (transaction.decoded.getOutputs().size() != 2) {
                throw new IllegalArgumentException("Verification: Wrong number of transaction outputs.");
            }
            final List<ListenableFuture<Boolean>> changeVerifications = new ArrayList<>();
            changeVerifications.add(
                    gaService.verifySpendableBy(transaction.decoded.getOutputs().get(0), transaction.subaccount_pointer, transaction.change_pointer));
            changeVerifications.add(
                    gaService.verifySpendableBy(transaction.decoded.getOutputs().get(1), transaction.subaccount_pointer, transaction.change_pointer));
            changeFuture = Futures.allAsList(changeVerifications);
        } else {
            changeFuture = Futures.immediateFuture(null);
        }

        // 2. Verify the main output value and address, if available:
        return Futures.transform(changeFuture, new Function<List<Boolean>, Coin>() {
            @Nullable
            @Override
            public Coin apply(final @Nullable List<Boolean> input) {
                return Verifier.verify(countedUtxoValues, transaction, recipient, amount, input);
            }
        });
    }

    class Node {
        @NonNull
        final String addr;
        final int port;

        Node(@NonNull final String trusted_addr) {
            final int index_port = trusted_addr.indexOf(":");
            if (index_port != -1) {
                addr = trusted_addr.substring(0, index_port);
                port = Integer.parseInt(trusted_addr.substring(index_port + 1));
            } else {
                addr = trusted_addr;
                port = Network.NETWORK.getPort();
            }
        }
        public String toString(){
            return String.format("%s:%d", addr, port);
        }
    }

    public int getSpvBlocksLeft() {
        if (gaService.isSPVEnabled())
            return spvBlocksLeft;
        return 0;
    }

    @Nullable
    private BlockStore blockStore;
    @Nullable
    private BlockChain blockChain;
    @Nullable
    private PeerGroup peerGroup;
    @Nullable
    private PeerFilterProvider pfProvider;
    @NonNull private final Object startSPVLock = new Object();
    private boolean isSpvSyncing = false, startSpvAfterInit = false, syncStarted = false;

    @Nullable
    public PeerGroup getPeerGroup(){
        return peerGroup;
    }

    private void toastTrustedSPV(final String announcement){
        final String trusted_peer = gaService.cfg("TRUSTED").getString("address", "");
        if(TabbedMainActivity.instance != null && !trusted_peer.isEmpty()){
            TabbedMainActivity.instance.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Toast.makeText(gaService.getApplicationContext(), announcement, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    private void recalculate(final boolean recalculateBloom) {
        if (recalculateBloom && peerGroup != null) {
            peerGroup.recalculateFastCatchupAndFilter(PeerGroup.FilterRecalculateMode.SEND_IF_CHANGED);
        }
    }
    public int getSpvHeight() {
        if (blockChain != null && gaService.isSPVEnabled())
            return blockChain.getBestChainHeight();
        return 0;
    }

    public boolean spvNotSyncing() {
        return !isSpvSyncing;
    }

    public boolean isPeerGroupRunning(){
        return peerGroup != null && peerGroup.isRunning();
    }
    private int spvBlocksLeft = Integer.MAX_VALUE;

    public synchronized void startSpvSync() {
        synchronized (startSPVLock) {
            if (syncStarted)
                return;
            else
                syncStarted = true;
        }
        if (isSpvSyncing) return;
        if (peerGroup == null) {  // disconnected while WiFi got up
            startSpvAfterInit = true;
            return;
        }
        final String trusted_peer = gaService.cfg("TRUSTED").getString("address", "");

        toastTrustedSPV(String.format("Attempting connection to trusted peers: %s", trusted_peer));
        Futures.addCallback(peerGroup.startAsync(), new FutureCallback<Object>() {
            @Override
            public void onSuccess(final @Nullable Object result) {
                peerGroup.startBlockChainDownload(new DownloadProgressTracker() {
                    @Override
                    public void onChainDownloadStarted(final Peer peer, final int blocksLeft) {
                        isSpvSyncing = true;
                        spvBlocksLeft = blocksLeft;
                        final PeerAddress addr = peer.getAddress();
                        toastTrustedSPV(String.format("Downloading chain from trusted peer: %s",
                                addr == null ? "?" : addr.toString()));
                    }

                    @Override
                    public void onBlocksDownloaded(final Peer peer, final Block block, final @Nullable FilteredBlock filteredBlock, final int blocksLeft) {
                        spvBlocksLeft = blocksLeft;
                    }

                });
            }

            @Override
            public void onFailure(@NonNull final Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void setupPeerGroup(final PeerGroup peerGroup, final String trusted_addr) {
        SPVMode mode;
        if (!trusted_addr.isEmpty() && trusted_addr.contains(".")) {
            final String trusted_lower = trusted_addr.toLowerCase();
            if (trusted_lower.contains(".onion")) {
                mode = SPVMode.ONION;
            }
            else {
                mode = SPVMode.TRUSTED;
            }

        } else {
            mode = SPVMode.NORMAL;
        }

        if (Network.NETWORK.getId().equals(NetworkParameters.ID_REGTEST)) {
            try {
                peerGroup.addAddress(new PeerAddress(InetAddress.getByName("192.168.56.1"), 19000));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            peerGroup.setMaxConnections(1);
        }
        else if (mode == SPVMode.NORMAL) {
            peerGroup.addPeerDiscovery(new DnsDiscovery(Network.NETWORK));
        }
        else if (mode == SPVMode.ONION) {

            try {
                final Node n = new Node(trusted_addr);

                final PeerAddress OnionAddr = new PeerAddress(InetAddress.getLocalHost(), n.port ) {
                    @NonNull
                    public InetSocketAddress toSocketAddress() {
                        return InetSocketAddress.createUnresolved(n.addr, n.port);
                    }
                };
                peerGroup.addAddress(OnionAddr);
            } catch (@NonNull final Exception e){
                e.printStackTrace();
            }
        }
        else {
            final Node n = new Node(trusted_addr);
            try {
                peerGroup.addAddress(new PeerAddress(InetAddress.getByName(n.addr), n.port));
            } catch (@NonNull final UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }
    public synchronized void setUpSPV(){
        //teardownSPV must be called if SPV already exists
        //and stopSPV if previous still running.
        if (peerGroup != null) {
            Log.d(TAG, "Must stop and tear down SPV before setting up again!");
            return;
        }

        final File blockChainFile = new File(gaService.getDir("blockstore_" + gaService.getReceivingId(), Context.MODE_PRIVATE), "blockchain.spvchain");

        try {
            blockStore = new SPVBlockStore(Network.NETWORK, blockChainFile);
            final StoredBlock storedBlock = blockStore.getChainHead(); // detect corruptions as early as possible
            if (storedBlock.getHeight() == 0 && !Network.NETWORK.equals(NetworkParameters.fromID(NetworkParameters.ID_REGTEST))) {
                InputStream is = null;
                try {
                    is = gaService.getAssets().open("checkpoints");
                    CheckpointManager.checkpoint(Network.NETWORK, is, blockStore, gaService.getClient().getLoginData().earliest_key_creation_time);
                } catch (@NonNull final IOException e) {
                    // couldn't load checkpoints, log & skip
                    e.printStackTrace();
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                    } catch (@NonNull final IOException e) {
                        // do nothing
                    }
                }
            }
            blockChain = new BlockChain(Network.NETWORK, blockStore);
            blockChain.addListener(blockChainListener = new BlockChainListener(gaService));

            System.setProperty("user.home", gaService.getFilesDir().toString());
            final String trusted_addr = gaService.cfg("TRUSTED").getString("address", "");

            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(gaService);
            final String proxyHost = sharedPref.getString("proxy_host", null);
            final String proxyPort = sharedPref.getString("proxy_port", null);

            if (proxyHost != null && proxyPort != null) {
                System.setProperty("http.proxyHost", proxyHost);
                System.setProperty("http.proxyPort", proxyPort);
                final org.bitcoinj.core.Context context = new org.bitcoinj.core.Context(Network.NETWORK);
                peerGroup = new PeerGroup(context, blockChain, new BlockingClientManager());
            } else {
                System.setProperty("http.proxyHost", "");
                System.setProperty("http.proxyPort", "");
                if (trusted_addr.toLowerCase().contains(".onion")) {
                    try {
                        final org.bitcoinj.core.Context context = new org.bitcoinj.core.Context(Network.NETWORK);
                        peerGroup = PeerGroup.newWithTor(context, blockChain, new TorClient(), false);
                    } catch (@NonNull final Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    peerGroup = new PeerGroup(Network.NETWORK, blockChain);
                }
            }
            peerGroup.addPeerFilterProvider(pfProvider = new PeerFilterProvider(gaService));

            if (trusted_addr.contains(",")) {
                final String[] addresses = trusted_addr.split(",");
                for (final String s: addresses) {
                    setupPeerGroup(peerGroup, s);
                }
                peerGroup.setMaxConnections(addresses.length);
            } else if (!trusted_addr.isEmpty()) {
                setupPeerGroup(peerGroup, trusted_addr);
                peerGroup.setMaxConnections(1);
            } else {
                setupPeerGroup(peerGroup, trusted_addr);
            }
        } catch (@NonNull final BlockStoreException e) {
            e.printStackTrace();
        }
    }

    public synchronized void stopSPVSync(){

        final Intent i = new Intent("PEERGROUP_UPDATED");
        i.putExtra("peergroup", "stopSPVSync");
        gaService.sendBroadcast(i);

        if (peerGroup != null && peerGroup.isRunning()) {
            peerGroup.stop();

        }
        isSpvSyncing = false;
        syncStarted = false;
    }

    public synchronized void tearDownSPV(){
        if (blockChain != null) {
            if (blockChainListener != null) {
                blockChain.removeListener(blockChainListener);
                blockChainListener = null;
            }
        }
        if (peerGroup != null) {
            if (pfProvider != null) {
                peerGroup.removePeerFilterProvider(pfProvider);
                pfProvider = null;
            }
            peerGroup = null;
        }
        if (blockStore != null) {
            try {
                blockStore.close();
                blockStore = null;
            } catch (@NonNull final BlockStoreException x) {
                throw new RuntimeException(x);
            }
        }
    }
}
