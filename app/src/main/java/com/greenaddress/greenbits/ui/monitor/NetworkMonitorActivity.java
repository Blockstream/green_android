package com.greenaddress.greenbits.ui.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.greenaddress.greenapi.Network;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.FirstScreenActivity;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.listeners.PeerConnectedEventListener;
import org.bitcoinj.core.listeners.PeerDisconnectedEventListener;

import java.util.ArrayList;
import java.util.Iterator;

public final class NetworkMonitorActivity extends GaActivity implements PeerConnectedEventListener, PeerDisconnectedEventListener
{
    private final ArrayList<PrettyPeer> peerList = new ArrayList<>();
    private ArrayAdapter<PrettyPeer> peerListAdapter;
    private String mBloomInfo = "";

    @Override
    protected int getMainViewId() { return R.layout.activity_network; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        final ListView peerList = UI.find(this, R.id.peerlistview);
        final TextView emptyText = UI.find(this, R.id.empty_list_view);
        peerList.setEmptyView(emptyText);
    }

    @Override
    public void onPauseWithService() {
        final GaService service = mService;

        unregisterReceiver(uiUpdated);
        final PeerGroup peerGroup = service.spv.getPeerGroup();
        if (peerGroup != null) {
            peerGroup.removeConnectedEventListener(this);
            peerGroup.removeDisconnectedEventListener(this);
        }

        peerList.clear();
    }

    @Override
    public void onResumeWithService() {
        final GaService service = mService;

        registerReceiver(uiUpdated, new IntentFilter("PEERGROUP_UPDATED"));

        if (service.isForcedOff()) {
            // FIXME: Should pass flag to activity so it shows it was forced logged out
            startActivity(new Intent(this, FirstScreenActivity.class));
            finish();
            return;
        }

        peerList.clear();

        final PeerGroup peerGroup = service.spv.getPeerGroup();
        if (peerGroup == null || !peerGroup.isRunning())
            return;

        for (final Peer peer : peerGroup.getConnectedPeers())
            peerList.add(new PrettyPeer(peer));

        if (peerList.size() > 0)
            mBloomInfo = peerList.get(0).peer.getBloomFilter().toString();
        else
            mBloomInfo = getString(R.string.network_monitor_bloom_info);

        final int currentBlock = service.getCurrentBlock();
        final int spvHeight = service.spv.getSpvHeight();
        final TextView bloomInfoText = UI.find(this, R.id.bloominfo);
        bloomInfoText.setText(getString(R.string.network_monitor_banner, mBloomInfo, currentBlock - spvHeight));

        peerListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, peerList);
        final ListView peerList = UI.find(this, R.id.peerlistview);
        peerList.setAdapter(peerListAdapter);

        peerGroup.addConnectedEventListener(this);
        peerGroup.addDisconnectedEventListener(this);
    }

    @Override
    public synchronized void onPeerConnected(final Peer peer, final int peerCount) {
        final PrettyPeer new_ppeer = new PrettyPeer(peer);
        final TextView bloomInfoText = UI.find(this, R.id.bloominfo);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                peerList.add(new_ppeer);
                peerListAdapter.notifyDataSetChanged();

                mBloomInfo = peer.getBloomFilter().toString();
                bloomInfoText.setText(mBloomInfo);
            }
        });
    }

    @Override
    public synchronized void onPeerDisconnected(final Peer peer, int peerCount) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final PrettyPeer new_ppeer = new PrettyPeer(peer);
                for (Iterator<PrettyPeer> it = peerList.iterator(); it.hasNext(); ) {
                    final PrettyPeer ppeer = it.next();
                    if (new_ppeer.peer == ppeer.peer) {
                        it.remove();
                    }
                }
                peerListAdapter.notifyDataSetChanged();
            }
        });
    }

    private final BroadcastReceiver uiUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {

            final String peerGroupIntent = intent.getExtras().getString("peergroup");
            if (peerGroupIntent != null && peerGroupIntent.equals("stopSPVSync")) {
                peerList.clear();
                peerListAdapter.notifyDataSetChanged();
            }
        }
    };

    private class PrettyPeer {
        final Peer peer;
        public PrettyPeer(final Peer peer) {
            this.peer = peer;
        }

        public String toString(){
            final GaService service = mService;
            String ipAddr = peer.toString();
            if (ipAddr.length() >= 11 && ipAddr.substring(0,11).equals("[127.0.0.1]")) {
                // FIXME: This is obviously not right
                ipAddr = service.getTrustedPeers();
                if (!ipAddr.isEmpty())
                    ipAddr = new Node(ipAddr).toString();
            }
            return String.format("%s\n%s\n%s\n%s", getString(R.string.network_monitor_peer_addr, ipAddr),
                    getString(R.string.network_monitor_peer_version, peer.getPeerVersionMessage().subVer),
                    getString(R.string.network_monitor_peer_block, peer.getBestHeight()),
                    getString(R.string.network_monitor_peer_ping, peer.getLastPingTime()));
        }
    }

    class Node {
        final String addr;
        final int port;

        Node(final String trusted_addr) {
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
}
