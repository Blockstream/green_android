package com.greenaddress.greenbits.spv;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
import android.util.Pair;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.PreparedTransaction;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.CB;
import com.greenaddress.greenbits.ui.R;
import com.squareup.okhttp.OkHttpClient;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.BloomFilter;
import org.bitcoinj.core.CheckpointManager;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.core.listeners.TransactionReceivedInBlockListener;
import org.bitcoinj.net.BlockingClientManager;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.net.discovery.HttpDiscovery;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.Wallet;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class SPV {

    private final static String TAG = SPV.class.getSimpleName();

    private final Map<TransactionOutPoint, Coin> mCountedUtxoValues = new HashMap<>();

    private final String VERIFIED = "verified_utxo_";
    private final String SPENDABLE = "verified_utxo_spendable_value_";

    class AccountInfo extends Pair<Integer, Integer> {
        public AccountInfo(Integer subAccount, final Integer pointer) { super(subAccount, pointer); }
        public Integer getSubAccount() { return first; }
        public Integer getPointer() { return second; }
    }

    // We use a single threaded executor to serialise config changes
    // without forcing callers to block.
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Map<Integer, Coin> mVerifiedCoinBalances = new HashMap<>();
    private final Map<Sha256Hash, List<Integer>> mUnspentOutpoints = new HashMap<>();
    private final Map<TransactionOutPoint, AccountInfo> mUnspentDetails = new HashMap<>();
    private final GaService mService;
    private int mBlocksRemaining = Integer.MAX_VALUE;
    private BlockStore mBlockStore;
    private BlockChain mBlockChain;
    private PeerGroup mPeerGroup;
    private final PeerFilterProvider mPeerFilter = new PeerFilterProvider(this);
    private NotificationManager mNotifyManager;
    private Builder mNotificationBuilder;
    private final static int mNotificationId = 1;
    private int mNetWorkType;
    private final Object mStateLock = new Object();

    public SPV(final GaService service) {
        mService = service;
        mNetWorkType = ConnectivityManager.TYPE_DUMMY;
    }

    public GaService getService() {
        return mService;
    }

    private <T> String Var(final String name, final T value) {
        return name + " => " + value.toString() + " ";
    }

    public boolean isEnabled() {
        return !mService.isWatchOnly() && mService.cfg("SPV").getBoolean("enabled", true);
    }

    public void setEnabledAsync(final boolean enabled) {
        mExecutor.execute(new Runnable() { public void run() { setEnabled(enabled); } });
    }

    private void setEnabled(final boolean enabled) {
        synchronized (mStateLock) {
            final boolean current = isEnabled();
            Log.d(TAG, "setEnabled: " + Var("enabled", enabled) + Var("current", current));
            if (enabled == current)
                return;
            mService.cfgEdit("SPV").putBoolean("enabled", enabled).apply();
            // FIXME: Should we delete unspent here?
            reset(false /* deleteAllData */, false /* deleteUnspent */);
        }
    }

    public boolean isSyncOnMobileEnabled() {
        return mService.cfg("SPV").getBoolean("mobileSyncEnabled", false);
    }

    public void setSyncOnMobileEnabledAsync(final boolean enabled) {
        mExecutor.execute(new Runnable() { public void run() { setSyncOnMobileEnabled(enabled); } });
    }

    private void setSyncOnMobileEnabled(final boolean enabled) {
        synchronized (mStateLock) {
            final boolean current = isSyncOnMobileEnabled();
            final boolean currentlyEnabled = isEnabled();
            Log.d(TAG, "setSyncOnMobileEnabled: " + Var("enabled", enabled) + Var("current", current));
            if (enabled == current)
                return; // Setting hasn't changed

            mService.cfgEdit("SPV").putBoolean("mobileSyncEnabled", enabled).apply();

            if (getNetworkType() != ConnectivityManager.TYPE_MOBILE)
                return; // Any change doesn't affect us since we aren't currently on mobile

            if (enabled && currentlyEnabled) {
                if (mPeerGroup == null)
                    setup();
                startSync();
            }
            else
                stopSync();
        }
    }

    public String getTrustedPeers() { return mService.cfg("TRUSTED").getString("address", ""); }

    public void setTrustedPeersAsync(final String peers) {
        mExecutor.execute(new Runnable() { public void run() { setTrustedPeers(peers); } });
    }

    private void setTrustedPeers(final String peers) {
        synchronized (mStateLock) {
            // FIXME: We should check if the peers differ here, instead of in the caller
            final String current = getTrustedPeers();
            Log.d(TAG, "setTrustedPeers: " + Var("peers", peers) + Var("current", current));
            mService.cfgEdit("TRUSTED").putString("address", peers).apply();
            mService.setUserConfig("trusted_peer_addr", peers, true);
            reset(false /* deleteAllData */, false /* deleteUnspent */);
        }
    }

    public PeerGroup getPeerGroup(){
        return mPeerGroup;
    }

    public boolean isVerified(final Sha256Hash txHash) {
        return mService.cfgIn(VERIFIED).getBoolean(txHash.toString(), false);
    }

    public void startAsync() {
        mExecutor.execute(new Runnable() { public void run() { start(); } });
    }

    private void start() {
        synchronized (mStateLock) {
            Log.d(TAG, "start");
            reset(false /* deleteAllData */, true /* deleteUnspent */);
            updateUnspentOutputs();
        }
    }

    public Coin getVerifiedBalance(final int subAccount) {
        return mVerifiedCoinBalances.get(subAccount);
    }

    public int getUnspentOutpointsSize() {
        return mUnspentOutpoints.size();
    }

    public int populateBloomFilter(BloomFilter filter) {
        final Set<Sha256Hash> keys = mUnspentOutpoints.keySet();
        for (final Sha256Hash hash : keys)
            filter.insert(hash.getReversedBytes());
        return keys.size();
    }

    public boolean isUnspentOutpoint(final Sha256Hash txHash) {
        return mUnspentOutpoints.containsKey(txHash);
    }

    private TransactionOutPoint createOutPoint(final Integer index, final Sha256Hash txHash) {
        return new TransactionOutPoint(Network.NETWORK, index, txHash);
    }

    public void updateUnspentOutputs() {
        final boolean currentlyEnabled = isEnabled();
        Log.d(TAG, "updateUnspentOutputs: " + Var("currentlyEnabled", currentlyEnabled));
        if (currentlyEnabled)
            CB.after(mService.getAllUnspentOutputs(0, null), new CB.Op<ArrayList>() {
                @Override
                public void onSuccess(final ArrayList outputs) {
                    updateUnspentOutputs(outputs);
                }
            });
    }

    private void updateUnspentOutputs(final ArrayList outputs) {
        final Set<TransactionOutPoint> newUtxos = new HashSet<>();
        boolean recalculateBloom = false;

        Log.d(TAG, Var("number of outputs", outputs.size()));
        for (final Map<?, ?> utxo : (ArrayList<Map<?, ?>>) outputs) {
            final String txHashHex = (String) utxo.get("txhash");
            final Integer blockHeight = (Integer) utxo.get("block_height");
            final Integer prevIndex = ((Integer) utxo.get("pt_idx"));
            final Integer subaccount = ((Integer) utxo.get("subaccount"));
            final Integer pointer = ((Integer) utxo.get("pointer"));
            final Sha256Hash txHash = Sha256Hash.wrap(txHashHex);

            if (isVerified(txHash)) {
                addToUtxo(txHash, prevIndex, subaccount, pointer);
                addUtxoToValues(txHash, false /* updateVerified */);
            } else {
                recalculateBloom = true;
                addToBloomFilter(blockHeight, txHash, prevIndex, subaccount, pointer);
            }
            newUtxos.add(createOutPoint(prevIndex, txHash));
        }

        final List<Integer> changedSubaccounts = new ArrayList<>();
        for (final TransactionOutPoint oldUtxo : new HashSet<>(mCountedUtxoValues.keySet())) {
            if (!newUtxos.contains(oldUtxo)) {
                recalculateBloom = true;

                final int subAccount = mUnspentDetails.get(oldUtxo).getSubAccount();
                final Coin verifiedBalance = getVerifiedBalance(subAccount);
                mVerifiedCoinBalances.put(subAccount,
                                          verifiedBalance.subtract(mCountedUtxoValues.get(oldUtxo)));
                changedSubaccounts.add(subAccount);
                mCountedUtxoValues.remove(oldUtxo);
                mUnspentDetails.remove(oldUtxo);
                mUnspentOutpoints.get(oldUtxo.getHash()).remove(((int) oldUtxo.getIndex()));
            }
        }

        if (recalculateBloom && mPeerGroup != null)
            mPeerGroup.recalculateFastCatchupAndFilter(PeerGroup.FilterRecalculateMode.SEND_IF_CHANGED);

        fireBalanceChanged(changedSubaccounts);
    }

    private void fireBalanceChanged(final List<Integer> subAccounts) {
        for (final int subAccount : subAccounts)
            mService.fireBalanceChanged(subAccount);
    }

    private void resetUnspent() {
        Log.d(TAG, "resetUnspent");
        mUnspentDetails.clear();
        mUnspentOutpoints.clear();
        mCountedUtxoValues.clear();
        mVerifiedCoinBalances.clear();
    }

    private void updateBalance(final TransactionOutPoint txOutpoint, final int subAccount, final Coin addValue) {
        if (mCountedUtxoValues.containsKey(txOutpoint))
           return;
        mCountedUtxoValues.put(txOutpoint, addValue);
        final Coin verifiedBalance = getVerifiedBalance(subAccount);
        if (verifiedBalance == null)
            mVerifiedCoinBalances.put(subAccount, addValue);
        else
            mVerifiedCoinBalances.put(subAccount, verifiedBalance.add(addValue));
    }

    public void addUtxoToValues(final Sha256Hash txHash, final boolean updateVerified) {
        final String txHashHex = txHash.toString();

        if (updateVerified)
            mService.cfgInEdit(VERIFIED).putBoolean(txHashHex, true).apply();

        final List<Integer> changedSubaccounts = new ArrayList<>();
        boolean missing = false;
        for (final Integer outpoint : mUnspentOutpoints.get(txHash)) {
            final String key = txHashHex + ":" + outpoint;
            final long value = mService.cfgIn(SPENDABLE).getLong(key, -1);
            if (value == -1) {
                missing = true;
                continue;
            }
            final TransactionOutPoint txOutpoint = createOutPoint(outpoint, txHash);
            final int subAccount = mUnspentDetails.get(txOutpoint).getSubAccount();
            if (!mCountedUtxoValues.containsKey(txOutpoint))
                changedSubaccounts.add(subAccount);
            updateBalance(txOutpoint, subAccount, Coin.valueOf(value));
        }
        fireBalanceChanged(changedSubaccounts);

        if (!missing) return;
        CB.after(mService.getRawUnspentOutput(txHash), new CB.Op<Transaction>() {
            @Override
            public void onSuccess(final Transaction result) {
                final List<Integer> changedSubaccounts = new ArrayList<>();
                final List<ListenableFuture<Boolean>> futuresList = new ArrayList<>();
                if (!result.getHash().equals(txHash)) {
                    Log.e(TAG, "txHash mismatch: expected " + txHashHex +
                               ", got " + result.getHash().toString());
                    return;
                }

                for (final Integer outpoint : mUnspentOutpoints.get(txHash)) {
                    final TransactionOutPoint txOutpoint = createOutPoint(outpoint, txHash);
                    if (mCountedUtxoValues.containsKey(txOutpoint))
                        continue;

                    final AccountInfo accountInfo = mUnspentDetails.get(txOutpoint);
                    final int subAccount = accountInfo.getSubAccount();
                    final int pointer = accountInfo.getPointer();

                    final ListenableFuture<Boolean> verifyFn;
                    verifyFn = mService.verifySpendableBy(result.getOutput(outpoint), subAccount, pointer);
                    futuresList.add(Futures.transform(verifyFn, new Function<Boolean, Boolean>() {
                        @Override
                        public Boolean apply(final Boolean input) {
                            final String key = txHashHex + ":" + outpoint;
                            if (!input)
                                Log.e(TAG, "txHash " + key + " not spendable!");
                            else {
                                final Coin value = result.getOutput(outpoint).getValue();
                                updateBalance(txOutpoint, subAccount, value);
                                changedSubaccounts.add(subAccount);
                                mService.cfgInEdit(SPENDABLE).putLong(key, value.longValue()).apply();
                            }
                            return input;
                        }
                    }));
                }
                CB.after(Futures.allAsList(futuresList), new CB.Op<List<Boolean>>() {
                    @Override
                    public void onSuccess(final List<Boolean> result) {
                        fireBalanceChanged(changedSubaccounts);
                    }
                });
            }
        });
    }

    public void onNewBlock(final int blockHeight) {
        Log.d(TAG, "onNewBlock: " + Var("blockHeight", blockHeight) +
              Var("isEnabled", isEnabled()));
        if (isEnabled())
            addToBloomFilter(blockHeight, null, -1, -1, -1);
    }

    private void addToBloomFilter(final Integer blockHeight, final Sha256Hash txHash, final int prevIndex, final int subAccount, final int pointer) {
        if (mBlockChain == null)
            return; // can happen before login (onNewBlock)
        if (txHash != null)
            addToUtxo(txHash, prevIndex, subAccount, pointer);

        if (blockHeight != null && blockHeight <= mBlockChain.getBestChainHeight() &&
            (txHash == null || !mUnspentOutpoints.containsKey(txHash))) {
            // new tx or block notification with blockHeight <= current blockHeight means we might've [1]
            // synced the height already while we haven't seen the tx, so we need to re-sync to be able
            // to verify it.
            // [1] - "might've" in case of txHash == null (block height notification),
            //       because it depends on the order of notifications
            //     - "must've" in case of txHash != null, because this means the tx arrived only after
            //       requesting it manually and we already had higher blockHeight
            //
            // We do it using the special case in bitcoinj for VM crashed because of
            // a transaction received.
            try {
                Log.d(TAG, "Creating fake wallet for re-sync");
                final Wallet fakeWallet = new Wallet(Network.NETWORK) {
                    @Override
                    public int getLastBlockSeenHeight() {
                        return blockHeight - 1;
                    }
                };
                mBlockChain.addWallet(fakeWallet);
                mBlockChain.removeWallet(fakeWallet);  // can be removed, because the call above
                // should rollback already
            } catch (final Exception e) {
                e.printStackTrace();
                Log.w(TAG, "fakeWallet exception: " + e.toString());
            }
        }
    }

    private void addToUtxo(final Sha256Hash txHash, final Integer prevIndex, final int subAccount, final int pointer) {
        mUnspentDetails.put(createOutPoint(prevIndex, txHash),
                            new AccountInfo(subAccount, pointer));
        if (mUnspentOutpoints.get(txHash) == null)
            mUnspentOutpoints.put(txHash, Lists.newArrayList(prevIndex));
        else
            mUnspentOutpoints.get(txHash).add(prevIndex);
    }

    private ListenableFuture<Boolean>
    verifyOutputSpendable(final PreparedTransaction ptx, final int index) {
        return mService.verifySpendableBy(ptx.mDecoded.getOutputs().get(index),
                                          ptx.mSubAccount, ptx.mChangePointer);
    }

    public ListenableFuture<Coin>
    validateTx(final PreparedTransaction ptx, final String recipientStr, final Coin amount) {
        Address recipient = null;
        try {
            recipient = Address.fromBase58(Network.NETWORK, recipientStr);
        } catch (final AddressFormatException e) {
        }

        // 1. Find the change output:
        ListenableFuture<List<Boolean>> changeFn = Futures.immediateFuture(null);

        if (ptx.mDecoded.getOutputs().size() == 2) {
            changeFn = Futures.allAsList(Lists.newArrayList(verifyOutputSpendable(ptx, 0),
                                                            verifyOutputSpendable(ptx, 1)));
        }
        else if (ptx.mDecoded.getOutputs().size() > 2)
            throw new IllegalArgumentException("Verification: Wrong number of transaction outputs.");

        // 2. Verify the main output value and address, if available:
        final Address recipientAddr = recipient;
        return Futures.transform(changeFn, new Function<List<Boolean>, Coin>() {
            @Override
            public Coin apply(final List<Boolean> input) {
                return Verifier.verify(mCountedUtxoValues, ptx, recipientAddr, amount, input);
            }
        });
    }

    public int getSPVBlocksRemaining() {
        if (isEnabled())
            return mBlocksRemaining;
        return 0;
    }

    public int getSPVHeight() {
        if (mBlockChain != null && isEnabled())
            return mBlockChain.getBestChainHeight();
        return 0;
    }

    private void startSync() {
        synchronized (mStateLock) {
            Log.d(TAG, "startSync: " + Var("mPeerGroup.isRunning", mPeerGroup.isRunning()));
            if (mPeerGroup.isRunning())
                 return;

            if (mNotifyManager == null) {
                mNotifyManager = (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationBuilder = new NotificationCompat.Builder(mService);
                mNotificationBuilder.setContentTitle("GreenBits SPV Sync")
                                    .setSmallIcon(R.drawable.ic_sync_black_24dp);
            }

            mNotificationBuilder.setContentText("Connecting to peer(s)...");
            mNotifyManager.notify(mNotificationId, mNotificationBuilder.build());

            CB.after(mPeerGroup.startAsync(), new FutureCallback<Object>() {
                @Override
                public void onSuccess(final Object result) {
                    mPeerGroup.startBlockChainDownload(new DownloadProgressTracker() {
                        @Override
                        public void onChainDownloadStarted(final Peer peer, final int blocksLeft) {
                            // Note that this method may be called multiple times if syncing
                            // switches peers while downloading.
                            Log.d(TAG, "onChainDownloadStarted: " + Var("blocksLeft", blocksLeft));
                            mBlocksRemaining = blocksLeft;
                            super.onChainDownloadStarted(peer, blocksLeft);
                        }

                        @Override
                        public void onBlocksDownloaded(final Peer peer, final Block block, final FilteredBlock filteredBlock, final int blocksLeft) {
                            //Log.d(TAG, "onBlocksDownloaded: " + Var("blocksLeft", blocksLeft));
                            mBlocksRemaining = blocksLeft;
                            super.onBlocksDownloaded(peer, block, filteredBlock, blocksLeft);
                        }

                        @Override
                        protected void startDownload(int blocks) {
                            Log.d(TAG, "startDownload");
                            updateUI(100, 0);
                        }

                        @Override
                        protected void progress(double percent, int blocksSoFar, Date date) {
                            //Log.d(TAG, "progress: " + Var("percent", percent));
                            mNotificationBuilder.setContentText("Sync in progress...");
                            updateUI(100, (int) percent);
                        }

                        @Override
                        protected void doneDownload() {
                            Log.d(TAG, "doneDownLoad");
                            mNotifyManager.cancel(mNotificationId);
                        }

                        private void updateUI(final int total, final int soFar) {
                            mNotificationBuilder.setProgress(total, soFar, false);
                            mNotifyManager.notify(mNotificationId, mNotificationBuilder.build());
                        }
                    });
                }

                @Override
                public void onFailure(final Throwable t) {
                    t.printStackTrace();
                    mNotifyManager.cancel(mNotificationId);
                }
            });
        }
    }

    private PeerAddress getPeerAddress(final String address) throws URISyntaxException, UnknownHostException {
        final URI uri = new URI("btc://" + address);
        final String host = uri.getHost();
        final int port = uri.getPort() == -1? Network.NETWORK.getPort() : uri.getPort();

        if (!mService.isProxyEnabled())
            return new PeerAddress(Network.NETWORK, InetAddress.getByName(host), port);

        return new PeerAddress(Network.NETWORK, host, port) {
            @Override
            public InetSocketAddress toSocketAddress() {
                return InetSocketAddress.createUnresolved(host, port);
            }

            @Override
            public String toString() {
                return String.format("%s:%s", host, port);
            }

            @Override
            public int hashCode() {
                return host.hashCode() ^ port;
            }
        };
    }

    private void addPeer(final String address) throws URISyntaxException, UnknownHostException {

        if (address.isEmpty()) {
            // Blank - Use the built in list, resolving via DNS
            if (!mService.isProxyEnabled())
                mPeerGroup.addPeerDiscovery(new DnsDiscovery(Network.NETWORK));
            else {
                final OkHttpClient httpClient = new OkHttpClient();
                httpClient.setSocketFactory(new Socks5SocketFactory(mService.getProxyHost(), mService.getProxyPort()));
                mPeerGroup.addPeerDiscovery(new HttpDiscovery(Network.NETWORK,
                        new HttpDiscovery.Details(
                                ECKey.fromPublicOnly(Utils.HEX.decode("0238746c59d46d5408bf8b1d0af5740fe1a6e1703fcb56b2953f0b965c740d256f")),
                                URI.create("http://httpseed.bitcoin.schildbach.de/peers")
                        ), httpClient));
            }
            return;
        }
        try {
            mPeerGroup.addAddress(getPeerAddress(address));
        } catch (final UnknownHostException e) {
            // FIXME: Should report this error: one the host here couldn't be resolved
            e.printStackTrace();
        }
    }

    final TransactionReceivedInBlockListener mTxListner = new TransactionReceivedInBlockListener() {
        @Override
        public void receiveFromBlock(final Transaction tx, final StoredBlock block, final BlockChain.NewBlockType blockType, final int relativityOffset) throws VerificationException {
            getService().notifyObservers(tx.getHash());
        }

        @Override
        public boolean notifyTransactionIsInBlock(final Sha256Hash txHash, final StoredBlock block, final BlockChain.NewBlockType blockType, final int relativityOffset) throws VerificationException {
            getService().notifyObservers(txHash);
            return isUnspentOutpoint(txHash);
        }
    };

    private void setup(){
        synchronized (mStateLock) {
            Log.d(TAG, "setup: " + Var("mPeerGroup != null", mPeerGroup != null));

            if (mPeerGroup != null) {
                // FIXME: Make sure this can never happen
                Log.e(TAG, "Must stop and tear down SPV before setting up again!");
                return;
            }

            try {
                Log.d(TAG, "Creating block store");
                mBlockStore = new SPVBlockStore(Network.NETWORK, mService.getSPVChainFile());
                final StoredBlock storedBlock = mBlockStore.getChainHead(); // detect corruptions as early as possible
                if (storedBlock.getHeight() == 0 && !Network.NETWORK.equals(NetworkParameters.fromID(NetworkParameters.ID_REGTEST))) {
                    InputStream is = null;
                    try {
                        is = mService.getAssets().open("checkpoints");
                        CheckpointManager.checkpoint(Network.NETWORK, is, mBlockStore, mService.getLoginData().earliest_key_creation_time);
                    } catch (final IOException e) {
                        // couldn't load checkpoints, log & skip
                        e.printStackTrace();
                    } finally {
                        try {
                            if (is != null)
                                is.close();
                        } catch (final IOException e) {
                            // do nothing
                        }
                    }
                }
                Log.d(TAG, "Creating block chain");
                mBlockChain = new BlockChain(Network.NETWORK, mBlockStore);
                mBlockChain.addTransactionReceivedListener(mTxListner);

                System.setProperty("user.home", mService.getFilesDir().toString());
                final String peers = getTrustedPeers();

                Log.d(TAG, "Creating peer group");
                if (!mService.isProxyEnabled())
                    mPeerGroup = new PeerGroup(Network.NETWORK, mBlockChain);
                else {
                    final String proxyHost = mService.getProxyHost();
                    final String proxyPort = mService.getProxyPort();
                    final Socks5SocketFactory sf = new Socks5SocketFactory(proxyHost, proxyPort);
                    final BlockingClientManager bcm = new BlockingClientManager(sf);
                    bcm.setConnectTimeoutMillis(60000);
                    mPeerGroup = new PeerGroup(Network.NETWORK, mBlockChain, bcm);
                    mPeerGroup.setConnectTimeoutMillis(60000);
                }

                mPeerGroup.addPeerFilterProvider(mPeerFilter);

                Log.d(TAG, "Adding peers");
                final ArrayList<String> addresses;
                addresses = new ArrayList<>(Arrays.asList(peers.split(",")));
                if (addresses.isEmpty())
                    addresses.add(Network.DEFAULT_PEER); // Usually empty, set for regtest
                for (final String address: addresses)
                    addPeer(address);


            } catch (final BlockStoreException | UnknownHostException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopSyncAsync() {
        mExecutor.execute(new Runnable() { public void run() { stopSync(); } });
    }

    public void stopSync() {
        synchronized (mStateLock) {
            Log.d(TAG, "stopSync: " + Var("isEnabled", isEnabled()));

            if (mPeerGroup != null && mPeerGroup.isRunning()) {
                Log.d(TAG, "Stopping peer group");
                final Intent i = new Intent("PEERGROUP_UPDATED");
                i.putExtra("peergroup", "stopSPVSync");
                mService.sendBroadcast(i);
                mPeerGroup.stop();
            }

            if (mNotifyManager != null)
                mNotifyManager.cancel(mNotificationId);

            if (mBlockChain != null) {
                Log.d(TAG, "Disposing of block chain");
                mBlockChain.removeTransactionReceivedListener(mTxListner);
                mBlockChain = null;
            }

            if (mPeerGroup != null) {
                Log.d(TAG, "Deleting peer group");
                mPeerGroup.removePeerFilterProvider(mPeerFilter);
                mPeerGroup = null;
            }

            if (mBlockStore != null) {
                Log.d(TAG, "Closing block store");
                try {
                    mBlockStore.close();
                    mBlockStore = null;
                } catch (final BlockStoreException x) {
                    throw new RuntimeException(x);
                }
            }
        }
    }

    // We only care about mobile vs non-mobile so treat others as ethernet
    private int getNetworkType(final NetworkInfo info) {
        if (info == null)
            return ConnectivityManager.TYPE_DUMMY;
        final int type = info.getType();
        return type == ConnectivityManager.TYPE_MOBILE ? type : ConnectivityManager.TYPE_ETHERNET;
    }

    private int getNetworkType() { return getNetworkType(mService.getNetworkInfo()); }

    // Handle changes to network connectivity.
    // Note that this only handles mobile/non-mobile transitions
    // FIXME: - Move the impl to Async and synchronise it
    //        - Call setup() before startSync if needed
    public void onNetConnectivityChangedAsync(final NetworkInfo info) {
        mExecutor.execute(new Runnable() { public void run() { onNetConnectivityChanged(info); } });
    }

    private void onNetConnectivityChanged(final NetworkInfo info) {
        synchronized (mStateLock) {
            final int oldType = mNetWorkType;
            final int newType = getNetworkType(info);
            mNetWorkType = newType;

            if (!isEnabled() || newType == oldType)
                return; // No change

            Log.d(TAG, "onNetConnectivityChanged: " + Var("newType", newType) +
                  Var("oldType", oldType) + Var("isSyncOnMobileEnabled", isSyncOnMobileEnabled()));

            if (newType == ConnectivityManager.TYPE_MOBILE) {
                if (!isSyncOnMobileEnabled())
                    stopSync(); // Mobile network and we have sync mobile disabled
            } else if (oldType == ConnectivityManager.TYPE_MOBILE) {
                if (isSyncOnMobileEnabled())
                    startSync(); // Non-Mobile network and we have sync mobile enabled
            }
        }
    }

    public void resetAsync() {
        mExecutor.execute(new Runnable() {
            public void run() {
                reset(true /* deleteAllData */, true /* deleteUnspent */);
            }
        });
    }

    public void reset(final boolean deleteAllData, final boolean deleteUnspent) {
        synchronized (mStateLock) {
            Log.d(TAG, "reset: " + Var("deleteAllData", deleteAllData) +
                  Var("deleteUnspent", deleteUnspent));
            stopSync();

            if (deleteAllData) {
                Log.d(TAG, "Deleting chain file");
                mService.getSPVChainFile().delete();

                try {
                    Log.d(TAG, "Clearing verified and spendable transactions");
                    mService.cfgInEdit(SPENDABLE).clear().commit();
                    mService.cfgInEdit(VERIFIED).clear().commit();
                } catch (final NullPointerException e) {
                    // ignore
                }
            }

            if (deleteUnspent) {
                Log.d(TAG, "Resetting unspent outputs");
                resetUnspent();
            }

            if (isEnabled()) {
                setup();

                // We might race with our network callbacks, so fetch the network type
                // if its unknown.
                if (mNetWorkType == ConnectivityManager.TYPE_DUMMY)
                    mNetWorkType = getNetworkType();

                if (isSyncOnMobileEnabled() || mNetWorkType != ConnectivityManager.TYPE_MOBILE)
                    startSync();
            }
            Log.d(TAG, "Finished reset");
        }
    }
}
