package com.greenaddress.greenbits.ui.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.greenaddress.greenbits.ui.authentication.FirstScreenActivity;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.listeners.PeerConnectedEventListener;
import org.bitcoinj.core.listeners.PeerDisconnectedEventListener;

import java.util.ArrayList;
import java.util.Iterator;

public final class NetworkMonitorActivity extends GaActivity implements PeerConnectedEventListener,
       PeerDisconnectedEventListener
{
    private final ArrayList<PrettyPeer> mPeers = new ArrayList<>();
    private ArrayAdapter<PrettyPeer> mPeerListAdapter;
    private Handler mRefreshHandler;
    private Runnable mRefreshCallback;

    private ListView mPeerList;
    private TextView mBloomInfoText;

    @Override
    protected int getMainViewId() { return R.layout.activity_network; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        mService.enableSPVPingMonitoring();

        mPeerList = UI.find(this, R.id.peerlistview);
        mBloomInfoText = UI.find(this, R.id.bloominfo);
        mPeerListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mPeers);

        mPeerList.setEmptyView(UI.find(this, R.id.empty_list_view));

        mRefreshHandler = new Handler();
        mRefreshCallback = () -> {
            // Redraw the list view and update again in 2 seconds
            mPeerListAdapter.notifyDataSetChanged();
            mRefreshHandler.postDelayed(mRefreshCallback, 2000);
        };
    }

    @Override
    public void onPauseWithService() {

        mService.disableSPVPingMonitoring();
        mRefreshHandler.removeCallbacks(mRefreshCallback);

        unregisterReceiver(uiUpdated);
        final PeerGroup peerGroup = mService.getSPVPeerGroup();
        if (peerGroup != null) {
            peerGroup.removeConnectedEventListener(this);
            peerGroup.removeDisconnectedEventListener(this);
        }

        mPeers.clear();
    }

    @Override
    public void onResumeWithService() {

        registerReceiver(uiUpdated, new IntentFilter("PEERGROUP_UPDATED"));

        if (mService.getConnectionManager().isDisconnectedOrLess()) {
            // FIXME: Should pass flag to activity so it shows it was forced logged out
            startActivity(new Intent(this, FirstScreenActivity.class));
            finish();
            return;
        }

        mPeers.clear();

        final PeerGroup peerGroup = mService.getSPVPeerGroup();
        if (peerGroup == null || !peerGroup.isRunning())
            return;

        mService.enableSPVPingMonitoring();

        for (final Peer peer : peerGroup.getConnectedPeers())
            mPeers.add(new PrettyPeer(peer));

        final String bloomDetails;
        if (!mPeers.isEmpty())
            bloomDetails = mPeers.get(0).mPeer.getBloomFilter().toString();
        else
            bloomDetails = getString(R.string.id_information_not_available);

        final int currentBlock = mService.getModel().getCurrentBlock();
        final int spvHeight = mService.getSPVHeight();
        mBloomInfoText.setText(getString(R.string.id_1s_blocks_left_2d, bloomDetails, currentBlock - spvHeight));

        mPeerList.setAdapter(mPeerListAdapter);

        peerGroup.addConnectedEventListener(this);
        peerGroup.addDisconnectedEventListener(this);
        mRefreshCallback.run();
    }

    @Override
    public synchronized void onPeerConnected(final Peer peer, final int peerCount) {
        runOnUiThread(() -> {
            mPeers.add(new PrettyPeer(peer));
            mPeerListAdapter.notifyDataSetChanged();
            mBloomInfoText.setText(peer.getBloomFilter().toString());
        });
    }

    @Override
    public synchronized void onPeerDisconnected(final Peer peer, final int peerCount) {
        runOnUiThread(() -> {
            for (final Iterator<PrettyPeer> it = mPeers.iterator(); it.hasNext(); ) {
                if (peer == it.next().mPeer)
                    it.remove();
            }
            mPeerListAdapter.notifyDataSetChanged();
        });
    }

    private final BroadcastReceiver uiUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {

            final String peerGroupIntent = intent.getExtras().getString("peergroup");
            if (peerGroupIntent == null || !peerGroupIntent.equals("stopSPVSync"))
                return;
            runOnUiThread(() -> {
                mPeers.clear();
                mPeerListAdapter.notifyDataSetChanged();
            });
        }
    };

    private class PrettyPeer {
        final Peer mPeer;

        PrettyPeer(final Peer peer) {
            mPeer = peer;
        }

        public String toString(){
            long pingTime = mPeer.getLastPingTime();
            if (pingTime == Long.MAX_VALUE)
                pingTime = 0;
            return String.format("%s\n%s\n%s\n%s", getString(R.string.id_address_1s, mPeer.toString()),
                                 getString(R.string.id_version_1s, mPeer.getPeerVersionMessage().subVer),
                                 getString(R.string.id_block_height_1d, mPeer.getBestHeight()),
                                 getString(R.string.id_last_ping_1d_ms, pingTime));
        }
    }
}
