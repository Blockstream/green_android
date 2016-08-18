package com.greenaddress.greenbits.spv;

import android.content.Intent;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.blockstream.libwally.Wally;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.PreparedTransaction;
import com.greenaddress.greenbits.GaService;
import com.subgraph.orchid.TorClient;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.CheckpointManager;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.net.BlockingClientManager;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.Wallet;

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

    private final static String TAG = SPV.class.getSimpleName();

    private final Map<TransactionOutPoint, Coin> countedUtxoValues = new HashMap<>();

    private final String VERIFIED = "verified_utxo_";
    private final String SPENDABLE = "verified_utxo_spendable_value_";

    private final Map<Integer, Coin> mVerifiedCoinBalances = new HashMap<>();
    private final Map<Sha256Hash, List<Integer>> unspentOutputsOutpoints = new HashMap<>();
    private final Map<TransactionOutPoint, Integer> unspentOutpointsSubaccounts = new HashMap<>();
    private final Map<TransactionOutPoint, Integer> unspentOutpointsPointers = new HashMap<>();
    public final GaService gaService;
    private BlockChainListener blockChainListener;
    private int spvBlocksLeft = Integer.MAX_VALUE;
    private BlockStore blockStore;
    private BlockChain blockChain;
    private PeerGroup peerGroup;
    private PeerFilterProvider pfProvider;

    private enum SPVMode {
        ONION, TRUSTED, NORMAL
    }

    public SPV(final GaService gaService) {
        this.gaService = gaService;
    }

    public void onTrustedPeersChanged() {
        new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(Object[] params) {
                final boolean alreadySyncing = stopSPVSync();
                setUpSPV();
                if (alreadySyncing)
                    startSPVSync();
                return null;
            }
        }.execute();
    }

    public void setEnabled(final boolean enabled) {

        new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(final Object[] params) {
                setEnabledImpl(enabled);
                return null;
            }
        }.execute();
    }

    public void setEnabledImpl(final boolean enabled) {

        if (enabled != gaService.isSPVEnabled()) {
            gaService.cfgEdit("SPV").putBoolean("enabled", enabled).apply();
            if (enabled) {
                setUpSPV();
                startSPVSync();
            } else
                stopSPVSync();
        }
    }

    public void setSyncOnMobileEnabled(final boolean enabled) {

        new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(final Object[] params) {
                setSyncOnMobileEnabledImpl(enabled);
                return null;
            }
        }.execute();
    }


    public void setSyncOnMobileEnabledImpl(final boolean enabled) {

        if (enabled != gaService.isSPVSyncOnMobileEnabled()) {
            gaService.cfgEdit("SPV").putBoolean("mobileSyncEnabled", enabled).apply();
            onNetConnectivityChanged(gaService.getNetworkInfo());
        }
    }

    public void startIfEnabled() {
        resetUnspent();

        if (gaService.isSPVEnabled()) {
            setUpSPV();
            startSPVSync();
        }
        updateUnspentOutputs();
    }

    public Coin getVerifiedCoinBalance(final int subAccount) {
        return mVerifiedCoinBalances.get(subAccount);
    }

    public Map<Sha256Hash, List<Integer>> getUnspentOutputsOutpoints() {
        return unspentOutputsOutpoints;
    }

    public void updateUnspentOutputs() {
        if (!gaService.isSPVEnabled())
            return;

        Futures.addCallback(gaService.getAllUnspentOutputs(0, null), new FutureCallback<ArrayList>() {
            @Override
            public void onSuccess(final ArrayList result) {
                final Set<TransactionOutPoint> newUtxos = new HashSet<>();
                boolean recalculateBloom = false;

                for (int i = 0; i < result.size(); ++i) {
                    final Map<?, ?> utxo = (Map) result.get(i);
                    final String txhash = (String) utxo.get("txhash");
                    final Integer blockHeight = (Integer) utxo.get("block_height");
                    final Integer pt_idx = ((Integer) utxo.get("pt_idx"));
                    final Sha256Hash sha256hash = Sha256Hash.wrap(Wally.hex_to_bytes(txhash));
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
                        final int subAccount = unspentOutpointsSubaccounts.get(oldUtxo);
                        final Coin verifiedBalance = getVerifiedCoinBalance(subAccount);
                        mVerifiedCoinBalances.put(subAccount,
                                                  verifiedBalance.subtract(countedUtxoValues.get(oldUtxo)));
                        changedSubaccounts.add(subAccount);
                        countedUtxoValues.remove(oldUtxo);
                        unspentOutpointsSubaccounts.remove(oldUtxo);
                        unspentOutpointsPointers.remove(oldUtxo);
                        unspentOutputsOutpoints.get(oldUtxo.getHash()).remove(((int) oldUtxo.getIndex()));
                    }
                }

                if (recalculateBloom && peerGroup != null)
                    peerGroup.recalculateFastCatchupAndFilter(PeerGroup.FilterRecalculateMode.SEND_IF_CHANGED);

                for (final int subAccount : changedSubaccounts)
                    gaService.fireBalanceChanged(subAccount);
            }

            @Override
            public void onFailure(final Throwable t) {
                t.printStackTrace();
            }
        });
    }

    public void resetUnspent() {
        unspentOutpointsSubaccounts.clear();
        unspentOutpointsPointers.clear();
        unspentOutputsOutpoints.clear();
        countedUtxoValues.clear();
        mVerifiedCoinBalances.clear();
    }

    private void updateBalance(final TransactionOutPoint txOutpoint, final int subAccount, final Coin addValue) {
        if (countedUtxoValues.containsKey(txOutpoint))
           return;
        countedUtxoValues.put(txOutpoint, addValue);
        final Coin verifiedBalance = getVerifiedCoinBalance(subAccount);
        if (verifiedBalance == null)
            mVerifiedCoinBalances.put(subAccount, addValue);
        else
            mVerifiedCoinBalances.put(subAccount, verifiedBalance.add(addValue));
    }

    public void addUtxoToValues(final Sha256Hash txHash) {
        final String txHashStr = txHash.toString();
        final List<Integer> changedSubaccounts = new ArrayList<>();
        boolean missing = false;
        for (final Integer outpoint : unspentOutputsOutpoints.get(txHash)) {
            final String key = txHashStr + ":" + outpoint;
            final long value = gaService.cfgIn(SPENDABLE).getLong(key, -1);
            if (value != -1) {
                final TransactionOutPoint txOutpoint = new TransactionOutPoint(Network.NETWORK, outpoint, txHash);
                final int subAccount = unspentOutpointsSubaccounts.get(txOutpoint);
                if (!countedUtxoValues.containsKey(txOutpoint))
                    changedSubaccounts.add(subAccount);
                updateBalance(txOutpoint, subAccount, Coin.valueOf(value));
            } else {
                missing = true;
            }
        }
        for (final int subAccount : changedSubaccounts)
            gaService.fireBalanceChanged(subAccount);

        if (!missing) return;
        Futures.addCallback(gaService.getRawUnspentOutput(txHash), new FutureCallback<Transaction>() {
            @Override
            public void onSuccess(final Transaction result) {
                final List<Integer> changedSubaccounts = new ArrayList<>();
                final List<ListenableFuture<Boolean>> futuresList = new ArrayList<>();
                if (result.getHash().equals(txHash)) {
                    for (final Integer outpoint : unspentOutputsOutpoints.get(txHash)) {
                        final TransactionOutPoint txOutpoint = new TransactionOutPoint(Network.NETWORK, outpoint, txHash);
                        if (countedUtxoValues.containsKey(txOutpoint))
                            continue;
                        final int subAccount = unspentOutpointsSubaccounts.get(txOutpoint);
                        final Integer pointer = unspentOutpointsPointers.get(txOutpoint);

                        futuresList.add(Futures.transform(gaService.verifySpendableBy(result.getOutput(outpoint), subAccount, pointer), new Function<Boolean, Boolean>() {
                            @Override
                            public Boolean apply(final Boolean input) {
                                if (input) {
                                    final Coin value = result.getOutput(outpoint).getValue();
                                    updateBalance(txOutpoint, subAccount, value);
                                    changedSubaccounts.add(subAccount);
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
                    public void onSuccess(final List<Boolean> result) {
                        for (final int subAccount : changedSubaccounts)
                            gaService.fireBalanceChanged(subAccount);
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        t.printStackTrace();
                    }
                });
            }

            @Override
            public void onFailure(final Throwable t) {
                t.printStackTrace();
            }
        });
    }


    public void addToBloomFilter(final Integer blockHeight, final Sha256Hash txhash, final int pt_idx, final int subAccount, final int pointer) {
        if (blockChain == null)
            return; // can happen before login (onNewBlock)
        if (txhash != null) {
            addToUtxo(txhash, pt_idx, subAccount, pointer);
        }
        if (blockHeight != null && blockHeight <= blockChain.getBestChainHeight() &&
                (txhash == null || !unspentOutputsOutpoints.containsKey(txhash))) {
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
            try {
                final Wallet fakeWallet = new Wallet(Network.NETWORK) {
                    @Override
                    public int getLastBlockSeenHeight() {
                        return blockHeight - 1;
                    }
                };
                blockChain.addWallet(fakeWallet);
                blockChain.removeWallet(fakeWallet);  // can be removed, because the call above
                // should rollback already
            } catch (final Exception e) {
                // FIXME: Seems this often happens, at least on initial startup
                Log.w(TAG, "fakeWallet exception: " + e.toString());
            }
        }
    }

    private void addToUtxo(final Sha256Hash txhash, final int pt_idx, final int subAccount, final int pointer) {
        unspentOutpointsSubaccounts.put(new TransactionOutPoint(Network.NETWORK, pt_idx, txhash), subAccount);
        unspentOutpointsPointers.put(new TransactionOutPoint(Network.NETWORK, pt_idx, txhash), pointer);
        if (unspentOutputsOutpoints.get(txhash) == null) {
            final ArrayList<Integer> newList = new ArrayList<>();
            newList.add(pt_idx);
            unspentOutputsOutpoints.put(txhash, newList);
        } else {
            unspentOutputsOutpoints.get(txhash).add(pt_idx);
        }
    }

    private ListenableFuture<Boolean>
    verifyOutputSpendable(final PreparedTransaction ptx, final int index) {
        return gaService.verifySpendableBy(ptx.decoded.getOutputs().get(index),
                                           ptx.subAccount, ptx.change_pointer);
    }

    public ListenableFuture<Coin>
    validateTx(final PreparedTransaction ptx, final String recipientStr, final Coin amount) {
        Address recipient = null;
        try {
            recipient = Address.fromBase58(Network.NETWORK, recipientStr);
        } catch (final AddressFormatException e) {
        }

        // 1. Find the change output:
        ListenableFuture<List<Boolean>> changeFuture = Futures.immediateFuture(null);

        if (ptx.decoded.getOutputs().size() == 2) {
            final List<ListenableFuture<Boolean>> changeVerifications = new ArrayList<>();
            changeVerifications.add(verifyOutputSpendable(ptx, 0));
            changeVerifications.add(verifyOutputSpendable(ptx, 1));
            changeFuture = Futures.allAsList(changeVerifications);
        }
        else if (ptx.decoded.getOutputs().size() > 2)
            throw new IllegalArgumentException("Verification: Wrong number of transaction outputs.");

        // 2. Verify the main output value and address, if available:
        final Address recipientAddr = recipient;
        return Futures.transform(changeFuture, new Function<List<Boolean>, Coin>() {
            @Override
            public Coin apply(final List<Boolean> input) {
                return Verifier.verify(countedUtxoValues, ptx, recipientAddr, amount, input);
            }
        });
    }

    class Node {
        final String addr;
        final int port;

        Node(final String trustedAddr) {
            final int index_port = trustedAddr.indexOf(":");
            if (index_port != -1) {
                addr = trustedAddr.substring(0, index_port);
                port = Integer.parseInt(trustedAddr.substring(index_port + 1));
            } else {
                addr = trustedAddr;
                port = Network.NETWORK.getPort();
            }
        }
        public String toString(){
            return String.format("%s:%d", addr, port);
        }
    }

    public int getSPVBlocksLeft() {
        if (gaService.isSPVEnabled())
            return spvBlocksLeft;
        return 0;
    }

    public PeerGroup getPeerGroup(){
        return peerGroup;
    }

    public int getSPVHeight() {
        if (blockChain != null && gaService.isSPVEnabled())
            return blockChain.getBestChainHeight();
        return 0;
    }

    private synchronized void startSPVSync() {
        if (peerGroup.isRunning())
             return;

        Futures.addCallback(peerGroup.startAsync(), new FutureCallback<Object>() {
            @Override
            public void onSuccess(final Object result) {
                peerGroup.startBlockChainDownload(new DownloadProgressTracker() {
                    @Override
                    public void onChainDownloadStarted(final Peer peer, final int blocksLeft) {
                        spvBlocksLeft = blocksLeft;
                    }

                    @Override
                    public void onBlocksDownloaded(final Peer peer, final Block block, final FilteredBlock filteredBlock, final int blocksLeft) {
                        spvBlocksLeft = blocksLeft;
                    }
                });
            }

            @Override
            public void onFailure(final Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private boolean isOnion(final String addr) { return addr.toLowerCase().contains(".onion"); }

    private void setupPeerGroup(final PeerGroup peerGroup, final String trustedAddr) {
        SPVMode mode = SPVMode.NORMAL;
        if (trustedAddr.contains("."))
            mode = isOnion(trustedAddr) ? SPVMode.ONION : SPVMode.TRUSTED;

        if (Network.NETWORK.getId().equals(NetworkParameters.ID_REGTEST)) {
            try {
                peerGroup.addAddress(new PeerAddress(InetAddress.getByName("192.168.56.1"), 19000));
            } catch (final UnknownHostException e) {
                e.printStackTrace();
            }
            peerGroup.setMaxConnections(1);
        }
        else if (mode == SPVMode.NORMAL) {
            peerGroup.addPeerDiscovery(new DnsDiscovery(Network.NETWORK));
        }
        else if (mode == SPVMode.ONION) {

            try {
                final Node n = new Node(trustedAddr);

                final PeerAddress OnionAddr = new PeerAddress(InetAddress.getLocalHost(), n.port ) {
                    public InetSocketAddress toSocketAddress() {
                        return InetSocketAddress.createUnresolved(n.addr, n.port);
                    }
                };
                peerGroup.addAddress(OnionAddr);
            } catch (final Exception e){
                e.printStackTrace();
            }
        }
        else {
            final Node n = new Node(trustedAddr);
            try {
                peerGroup.addAddress(new PeerAddress(InetAddress.getByName(n.addr), n.port));
            } catch (final UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void setUpSPV(){
        //teardownSPV must be called if SPV already exists
        //and stopSPV if previous still running.
        if (peerGroup != null) {
            Log.d(TAG, "Must stop and tear down SPV before setting up again!");
            return;
        }

        try {
            blockStore = new SPVBlockStore(Network.NETWORK, gaService.getSPVChainFile());
            final StoredBlock storedBlock = blockStore.getChainHead(); // detect corruptions as early as possible
            if (storedBlock.getHeight() == 0 && !Network.NETWORK.equals(NetworkParameters.fromID(NetworkParameters.ID_REGTEST))) {
                InputStream is = null;
                try {
                    is = gaService.getAssets().open("checkpoints");
                    CheckpointManager.checkpoint(Network.NETWORK, is, blockStore, gaService.getLoginData().earliest_key_creation_time);
                } catch (final IOException e) {
                    // couldn't load checkpoints, log & skip
                    e.printStackTrace();
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                    } catch (final IOException e) {
                        // do nothing
                    }
                }
            }
            blockChain = new BlockChain(Network.NETWORK, blockStore);
            if (blockChainListener != null)
                blockChainListener.onDispose();
            blockChainListener = new BlockChainListener(this);
            blockChain.addListener(blockChainListener);

            System.setProperty("user.home", gaService.getFilesDir().toString());
            final String peers = gaService.getTrustedPeers();

            String proxyHost = gaService.getProxyHost();
            String proxyPort = gaService.getProxyPort();

            if (proxyHost == null || proxyPort == null)
                proxyHost = proxyPort = "";

            System.setProperty("socksProxyHost", proxyHost);
            System.setProperty("socksProxyPort", proxyPort);

            if (!proxyHost.isEmpty() && !proxyPort.isEmpty()) {
                final org.bitcoinj.core.Context context = new org.bitcoinj.core.Context(Network.NETWORK);
                peerGroup = new PeerGroup(context, blockChain, new BlockingClientManager());
            } else if (isOnion(peers)) {
                try {
                    final org.bitcoinj.core.Context context = new org.bitcoinj.core.Context(Network.NETWORK);
                    peerGroup = PeerGroup.newWithTor(context, blockChain, new TorClient(), false);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            } else {
                peerGroup = new PeerGroup(Network.NETWORK, blockChain);
            }

            if (pfProvider != null)
                pfProvider.onDispose();
            pfProvider = new PeerFilterProvider(this);
            peerGroup.addPeerFilterProvider(pfProvider);

            final String[] addresses = peers.split(",");
            if (addresses.length == 0) {
                setupPeerGroup(peerGroup, "");
                return;
            }
            for (final String s: addresses)
                setupPeerGroup(peerGroup, s);
            peerGroup.setMaxConnections(addresses.length);

        } catch (final BlockStoreException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean stopSPVSync() {

        final boolean isRunning = peerGroup != null && peerGroup.isRunning();

        if (isRunning) {
            final Intent i = new Intent("PEERGROUP_UPDATED");
            i.putExtra("peergroup", "stopSPVSync");
            gaService.sendBroadcast(i);

            peerGroup.stop();
        }

        if (blockChain != null && blockChainListener != null) {
            blockChain.removeListener(blockChainListener);
            blockChainListener.onDispose();
            blockChainListener = null;
        }

        if (peerGroup != null) {
            if (pfProvider != null) {
                peerGroup.removePeerFilterProvider(pfProvider);
                pfProvider.onDispose();
                pfProvider = null;
            }
            peerGroup = null;
        }

        if (blockStore != null) {
            try {
                blockStore.close();
                blockStore = null;
            } catch (final BlockStoreException x) {
                throw new RuntimeException(x);
            }
        }

        return isRunning;
    }

    public void onNetConnectivityChanged(final NetworkInfo info) {
        // FIXME
    }

    public void reset() {
        final boolean enabled = gaService.isSPVEnabled();
        if (enabled) {
            // Stop SPV
            try {
                stopSPVSync();
            } catch (final NullPointerException e) {
                // FIXME: Why would we get an NPE here
                // ignore
            }
        }

        // Delete all data
        final File blockChainFile = gaService.getSPVChainFile();
        if (blockChainFile.exists())
            blockChainFile.delete();

        try {
            gaService.cfgInEdit(SPENDABLE).clear().commit();
            gaService.cfgInEdit(VERIFIED).clear().commit();
        } catch (final NullPointerException e) {
            // ignore
        }

        resetUnspent();

        if (enabled) {
            // Restart SPV
            setUpSPV();
            // FIXME: enabled under WiFi only
            startSPVSync();
        }
    }
}
