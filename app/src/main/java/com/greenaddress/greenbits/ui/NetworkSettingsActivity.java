package com.greenaddress.greenbits.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.ui.accounts.NetworkSwitchListener;
import com.greenaddress.greenbits.ui.accounts.SwitchNetworkAdapter;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class NetworkSettingsActivity extends GaActivity implements Observer, NetworkSwitchListener {

    private LinearLayout mProxySection;
    private Switch mSwitchTor;
    private Switch mSwitchProxy;
    private EditText mSocks5Host;
    private EditText mSocks5Port;
    private SwitchNetworkAdapter mSwitchNetworkAdapter;

    private boolean mIsLoggedIn = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_networksettings);
        getSupportActionBar().hide();
        mProxySection = UI.find(this, R.id.proxySection);
        mSwitchTor = UI.find(this, R.id.switchEnableTor);
        mSocks5Host = UI.find(this, R.id.socks5Host);
        mSocks5Port = UI.find(this, R.id.socks5Port);
        mSwitchProxy = UI.find(this, R.id.switchEnableProxySettings);

        final RecyclerView recyclerView = UI.find(this, R.id.networksRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));

        final List<NetworkData> networks = GDKSession.getNetworks();
        final NetworkData networkData = getNetwork();
        mSwitchNetworkAdapter = new SwitchNetworkAdapter(this, networks, networkData,
                                                         networkData.getLiquid(), this);
        recyclerView.setAdapter(mSwitchNetworkAdapter);

        final View closeButton = UI.find(this, R.id.close_network_settings);
        closeButton.setOnClickListener(this::onCloseClick);

        mSwitchProxy.setOnCheckedChangeListener(this::onProxyChange);
        initProxy();
        mSwitchTor.setOnCheckedChangeListener(this::onTorChange);
        initTor(mSwitchNetworkAdapter.getSelected());

        final Button selectButton = UI.find(this, R.id.selectNetworkButton);
        selectButton.setOnClickListener(this::onClick);

        if (getConnectionManager() == null ||
            getConnectionManager().isDisconnectedOrLess()) {
            selectButton.setText(R.string.id_save);
        } else {
            mIsLoggedIn = true;
            selectButton.setText(R.string.id_logout_and_switch);

            // Register the observer since we came in from the settings page
            getConnectionManager().addObserver(this);
        }
    }

    private void cancelAndExit() {
        setResult(RESULT_CANCELED);
        finishOnUiThread();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getConnectionManager() == null) {
            return;
        }

        if (mIsLoggedIn) {
            // the connection was closed while this activity was paused, exit immediately
            if (getConnectionManager().isDisconnectedOrLess()) {
                cancelAndExit();
                return;
            }

            // still connected, add back the observer
            getConnectionManager().addObserver(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getConnectionManager() == null) {
            return;
        }

        if (mIsLoggedIn) {
            getConnectionManager().deleteObserver(this);
        }
    }

    @Override
    public void update(Observable observable, Object arg) {
        if (observable instanceof ConnectionManager) {
            cancelAndExit();
        }
    }

    private void onCloseClick(final View view) {
        cancelAndExit();
    }

    private void onClick(final View view) {
        final NetworkData selectedNetwork = mSwitchNetworkAdapter.getSelected();
        String networkName = selectedNetwork.getNetwork();
        final String socksHost = UI.getText(mSocks5Host);
        final String socksPort = UI.getText(mSocks5Port);

        // Prevent settings that won't allow connection
        if (mSwitchProxy.isChecked() && (socksHost.isEmpty() || socksPort.isEmpty())) {
            UI.toast(this, R.string.id_socks5_proxy_and_port_must_be, Toast.LENGTH_LONG);
            return;
        }

        if (socksHost.startsWith("{")) {
            try {
                final NetworkData newNetwork = (new ObjectMapper()).readValue(socksHost, NetworkData.class);
                GDKSession.registerNetwork(newNetwork.getName(), socksHost);
                networkName = newNetwork.getNetwork();
            } catch (final Exception e) {
                UI.toast(this, e.getMessage(), Toast.LENGTH_LONG);
                return;
            }
        } else {
            getPrefOfSelected().edit()
            .putString(PrefKeys.PROXY_HOST, socksHost)
            .putString(PrefKeys.PROXY_PORT, socksPort)
            .apply();
            getConnectionManager().setProxyHostAndPort(socksHost, socksPort);
        }
        getGAApp().setCurrentNetwork(networkName);
        getConnectionManager().setNetwork(networkName);
        setResult(RESULT_OK);
        finishOnUiThread();
    }

    private SharedPreferences getPrefOfSelected() {
        return getSharedPreferences(mSwitchNetworkAdapter.getSelected().getNetwork(), MODE_PRIVATE);
    }

    private void initProxy() {
        final boolean isProxyEnabled = getPrefOfSelected().getBoolean(PrefKeys.PROXY_ENABLED, false);
        Log.d("NETDLG", "initProxy " + mSwitchNetworkAdapter.getSelected().getNetwork() + " " + isProxyEnabled);
        mSocks5Host.setText(getPrefOfSelected().getString(PrefKeys.PROXY_HOST,""));
        mSocks5Port.setText(getPrefOfSelected().getString(PrefKeys.PROXY_PORT,""));
        mSwitchProxy.setChecked(isProxyEnabled);
        mProxySection.setVisibility(isProxyEnabled ? View.VISIBLE : View.GONE);
    }

    private void onProxyChange(final CompoundButton compoundButton, final boolean b) {
        Log.d("NETDLG", "onProxyChange " + mSwitchNetworkAdapter.getSelected().getNetwork() + " " + b);
        getPrefOfSelected().edit().putBoolean(PrefKeys.PROXY_ENABLED, b).apply();
        getConnectionManager().setProxyEnabled(b);
        mProxySection.setVisibility(b ? View.VISIBLE : View.GONE);
    }

    private void initTor(final NetworkData selectedNetwork) {
        final boolean torChecked = getPrefOfSelected().getBoolean(PrefKeys.TOR_ENABLED, false);
        Log.d("NETDLG", "initTor " + mSwitchNetworkAdapter.getSelected().getNetwork() + " " + torChecked);
        mSwitchTor.setChecked(torChecked);
        mSwitchTor.setEnabled(!TextUtils.isEmpty(selectedNetwork.getWampOnionUrl()));
    }

    private void onTorChange(final CompoundButton compoundButton, final boolean b) {
        Log.d("NETDLG", "onTorChange " + mSwitchNetworkAdapter.getSelected().getNetwork() + " " + b);
        getPrefOfSelected().edit().putBoolean(PrefKeys.TOR_ENABLED, b).apply();
        getConnectionManager().setTorEnabled(b);
    }

    @Override
    public void onNetworkClick(final NetworkData networkData) {
        initProxy();
        initTor(networkData);
    }
}
