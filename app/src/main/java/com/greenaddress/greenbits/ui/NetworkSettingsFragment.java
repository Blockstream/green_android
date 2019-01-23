package com.greenaddress.greenbits.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class NetworkSettingsFragment extends DialogFragment {

    class NetworksViewAdapter extends RecyclerView.Adapter<NetworksViewAdapter.ViewHolder> {

        private List<NetworkData> mData;
        private int mSelectedItem;

        NetworksViewAdapter(final Context context, final List<NetworkData> data, NetworkData selectedItem) {
            mData = data;
            mSelectedItem = data.indexOf(selectedItem);
        }

        @Override
        public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            LinearLayout ll = new LinearLayout(getContext());
            RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT);
            ll.setPadding(8, 8, 8, 8);
            ll.setLayoutParams(layoutParams);
            return new ViewHolder(ll);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
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
            private Button mButton;

            ViewHolder(final View itemView) {
                super(itemView);
                mButton = new Button(new ContextThemeWrapper(getContext(), R.style.networkButton));
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

            public void setSelected(boolean selected) {
                if (selected) {
                    mButton.setPressed(true);
                } else {
                    mButton.setPressed(false);
                }
            }
        }
    }

    interface Listener {
        void onSelectNetwork();
    }

    private Listener mListener;

    public void setListener(final Listener listener) {
        this.mListener = listener;
    }

    private LinearLayout mProxySection;
    private Switch mSwitchTor;
    private Switch mSwitchProxy;
    private EditText mSocks5Host;
    private EditText mSocks5Port;

    NetworksViewAdapter mNetworksViewAdapter;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        getDialog().getWindow().setGravity(Gravity.BOTTOM);
    }

    private GreenAddressApplication mApp;

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);

        mApp = (GreenAddressApplication) getActivity().getApplication();
    }

    protected GaService getGAService() {
        return mApp.mService;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_networksettings, container, false);

        mProxySection = UI.find(v, R.id.proxySection);
        mSwitchTor = UI.find(v, R.id.switchEnableTor);
        mSocks5Host = UI.find(v, R.id.socks5Host);
        mSocks5Port = UI.find(v, R.id.socks5Port);
        mSwitchProxy = UI.find(v, R.id.switchEnableProxySettings);

        final RecyclerView recyclerView = UI.find(v, R.id.networksRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));

        final List<NetworkData> networks = GDKSession.getNetworks();

        mNetworksViewAdapter = new NetworksViewAdapter(getContext(), networks, getGAService().getNetwork());
        recyclerView.setAdapter(mNetworksViewAdapter);

        mSwitchProxy.setOnCheckedChangeListener(this::onProxyChange);
        initProxy();
        mSwitchTor.setOnCheckedChangeListener(this::onTorChange);
        initTor(mNetworksViewAdapter.getSelected());

        final View selectButton = v.findViewById(R.id.selectNetworkButton);
        selectButton.setOnClickListener(view -> {
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
                    UI.toast(getActivity(), e.getMessage(), Toast.LENGTH_LONG);
                }
            } else {
                getPrefOfSelected().edit()
                .putString(PrefKeys.PROXY_HOST, socksHost)
                .putString(PrefKeys.PROXY_PORT, socksPort)
                .apply();
                getGAService().getConnectionManager().setProxyHostAndPort(socksHost, socksPort);
            }
            getGAService().getConnectionManager().resetAttempts();
            getGAService().setCurrentNetworkId(networkName);
            mListener.onSelectNetwork();
            dismiss();
        });

        return v;
    }

    private SharedPreferences getPrefOfSelected() {
        return getActivity().getSharedPreferences(mNetworksViewAdapter.getSelected().getNetwork(), MODE_PRIVATE);
    }

    private void initProxy() {
        final boolean isProxyEnabled = getPrefOfSelected().getBoolean(PrefKeys.PROXY_ENABLED, false);
        Log.d("NETDLG", "initProxy " + mNetworksViewAdapter.getSelected().getNetwork() + " " + isProxyEnabled);
        mSocks5Host.setText(getPrefOfSelected().getString(PrefKeys.PROXY_HOST,""));
        mSocks5Port.setText(getPrefOfSelected().getString(PrefKeys.PROXY_PORT,""));
        mSwitchProxy.setChecked(isProxyEnabled);
        mProxySection.setVisibility(isProxyEnabled ? View.VISIBLE : View.GONE);
    }

    private void onProxyChange(CompoundButton compoundButton, boolean b) {
        Log.d("NETDLG", "onProxyChange " + mNetworksViewAdapter.getSelected().getNetwork() + " " + b);
        getPrefOfSelected().edit().putBoolean(PrefKeys.PROXY_ENABLED, b).apply();
        getGAService().getConnectionManager().setProxyEnabled(b);
        mProxySection.setVisibility(b ? View.VISIBLE : View.GONE);
    }

    private void initTor(final NetworkData selectedNetwork) {
        final boolean torChecked = getPrefOfSelected().getBoolean(PrefKeys.TOR_ENABLED, false);
        Log.d("NETDLG", "initTor " + mNetworksViewAdapter.getSelected().getNetwork() + " " + torChecked);
        mSwitchTor.setChecked(torChecked);
        mSwitchTor.setEnabled(!TextUtils.isEmpty(selectedNetwork.getWampOnionUrl()));
    }

    private void onTorChange(CompoundButton compoundButton, boolean b) {
        Log.d("NETDLG", "onTorChange " + mNetworksViewAdapter.getSelected().getNetwork() + " " + b);
        getPrefOfSelected().edit().putBoolean(PrefKeys.TOR_ENABLED, b).apply();
        getGAService().getConnectionManager().setTorEnabled(b);
    }
}
