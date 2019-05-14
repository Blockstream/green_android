package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.util.List;

public class NetworkSettingsActivity extends GaActivity {

    class NetworksViewAdapter extends RecyclerView.Adapter<NetworksViewAdapter.ViewHolder> {

        private final List<NetworkData> mData;
        private int mSelectedItem;

        NetworksViewAdapter(final Context context, final List<NetworkData> data, final NetworkData selectedItem) {
            mData = data;
            mSelectedItem = data.indexOf(selectedItem);
        }

        @Override
        public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            final LinearLayout ll = new LinearLayout(NetworkSettingsActivity.this);
            final RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT);
            ll.setPadding(8, 8, 8, 8);
            ll.setLayoutParams(layoutParams);
            return new ViewHolder(ll);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            final NetworkData networkData = mData.get(position);
            holder.setText(networkData.getName());
            holder.setIcon(networkData.getIcon());
            holder.setSelected(position == mSelectedItem);
            holder.itemView.setOnClickListener(view -> {
                mSelectedItem = holder.getAdapterPosition();
                notifyItemRangeChanged(0, mData.size());
                initProxy();
                initTor(mData.get(position));
            });
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        public NetworkData getSelected() {
            return mData.get( mSelectedItem );
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private final Button mButton;

            ViewHolder(final View itemView) {
                super(itemView);
                mButton = new Button(new ContextThemeWrapper(NetworkSettingsActivity.this, R.style.networkButton));
                mButton.setBackgroundResource(R.drawable.material_button_selection);

                final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
                mButton.setTextSize(16);
                mButton.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                mButton.setLayoutParams(layoutParams);

                mButton.setClickable(false);
                final LinearLayout linearLayout=(LinearLayout)itemView;

                linearLayout.addView(mButton);
            }

            public void setText(final String text) {
                mButton.setText(text);
            }

            public void setIcon(final int resource) {
                final Drawable top = getResources().getDrawable(resource);
                mButton.setCompoundDrawablesWithIntrinsicBounds(top, null, null, null);
            }

            public void setSelected(final boolean selected) {
                if (selected) {
                    mButton.setPressed(true);
                } else {
                    mButton.setPressed(false);
                }
            }
        }
    }

    private LinearLayout mProxySection;
    private Switch mSwitchTor;
    private Switch mSwitchProxy;
    private EditText mSocks5Host;
    private EditText mSocks5Port;
    private NetworksViewAdapter mNetworksViewAdapter;

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
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

        mNetworksViewAdapter = new NetworksViewAdapter(this, networks, mService.getNetwork());
        recyclerView.setAdapter(mNetworksViewAdapter);

        mSwitchProxy.setOnCheckedChangeListener(this::onProxyChange);
        initProxy();
        mSwitchTor.setOnCheckedChangeListener(this::onTorChange);
        initTor(mNetworksViewAdapter.getSelected());

        final View selectButton = UI.find(this, R.id.selectNetworkButton);
        selectButton.setOnClickListener(this::onClick);
    }

    private void onClick(final View view) {
        final NetworkData selectedNetwork = mNetworksViewAdapter.getSelected();
        String networkName = selectedNetwork.getNetwork();
        final String socksHost = UI.getText(mSocks5Host);
        final String socksPort = UI.getText(mSocks5Port);

        // Prevent settings that won't allow connection
        if (mSwitchProxy.isChecked() && (socksHost.isEmpty() || socksPort.isEmpty())) {
            UI.toast(this, R.string.id_socks5_proxy_and_port_must_be, Toast.LENGTH_LONG);
            return;
        }
        if (mSwitchTor.isChecked() && (socksHost.isEmpty() || !mSwitchProxy.isChecked())) {
            UI.toast(this, R.string.id_please_set_and_enable_socks5, Toast.LENGTH_LONG);
            return;
        }
        if (selectedNetwork.getLiquid()) {
            new MaterialDialog.Builder(this)
            .title(R.string.id_warning)
            .content(getResources().getString(R.string.id_liquid_is_a_bitcoin_sidechain))
            .positiveText(android.R.string.ok)
            .onPositive((dlg, which) -> save())
            .show();
            return;
        }
        save();
    }

    private void save() {
        final NetworkData selectedNetwork = mNetworksViewAdapter.getSelected();
        String networkName = selectedNetwork.getNetwork();
        final String socksHost = UI.getText(mSocks5Host);
        final String socksPort = UI.getText(mSocks5Port);
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
            mService.getConnectionManager().setProxyHostAndPort(socksHost, socksPort);
        }
        mService.setCurrentNetworkId(networkName);
        mService.getConnectionManager().setNetwork(networkName);
        setResult(RESULT_OK);
        finishOnUiThread();
    }

    private SharedPreferences getPrefOfSelected() {
        return getSharedPreferences(mNetworksViewAdapter.getSelected().getNetwork(), MODE_PRIVATE);
    }

    private void initProxy() {
        final boolean isProxyEnabled = getPrefOfSelected().getBoolean(PrefKeys.PROXY_ENABLED, false);
        Log.d("NETDLG", "initProxy " + mNetworksViewAdapter.getSelected().getNetwork() + " " + isProxyEnabled);
        mSocks5Host.setText(getPrefOfSelected().getString(PrefKeys.PROXY_HOST,""));
        mSocks5Port.setText(getPrefOfSelected().getString(PrefKeys.PROXY_PORT,""));
        mSwitchProxy.setChecked(isProxyEnabled);
        mProxySection.setVisibility(isProxyEnabled ? View.VISIBLE : View.GONE);
    }

    private void onProxyChange(final CompoundButton compoundButton, final boolean b) {
        Log.d("NETDLG", "onProxyChange " + mNetworksViewAdapter.getSelected().getNetwork() + " " + b);
        getPrefOfSelected().edit().putBoolean(PrefKeys.PROXY_ENABLED, b).apply();
        mService.getConnectionManager().setProxyEnabled(b);
        mProxySection.setVisibility(b ? View.VISIBLE : View.GONE);
    }

    private void initTor(final NetworkData selectedNetwork) {
        final boolean torChecked = getPrefOfSelected().getBoolean(PrefKeys.TOR_ENABLED, false);
        Log.d("NETDLG", "initTor " + mNetworksViewAdapter.getSelected().getNetwork() + " " + torChecked);
        mSwitchTor.setChecked(torChecked);
        mSwitchTor.setEnabled(!TextUtils.isEmpty(selectedNetwork.getWampOnionUrl()));
    }

    private void onTorChange(final CompoundButton compoundButton, final boolean b) {
        Log.d("NETDLG", "onTorChange " + mNetworksViewAdapter.getSelected().getNetwork() + " " + b);
        getPrefOfSelected().edit().putBoolean(PrefKeys.TOR_ENABLED, b).apply();
        mService.getConnectionManager().setTorEnabled(b);
    }
}
