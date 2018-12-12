package com.greenaddress.greenbits.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
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
import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.util.List;

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
            holder.setText("     " + networkData.getName());
            holder.setIcon(networkData.getIcon());
            holder.setSelected(position == mSelectedItem);
            holder.itemView.setOnClickListener(view -> {
                mSelectedItem = holder.getAdapterPosition();
                notifyItemRangeChanged(0, mData.size());
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

        final RecyclerView recyclerView = UI.find(v, R.id.networksRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));

        final List<NetworkData> networks = GDKSession.getNetworks();

        mNetworksViewAdapter = new NetworksViewAdapter(getContext(), networks, getGAService().getNetwork());
        recyclerView.setAdapter(mNetworksViewAdapter);

        final EditText socks5HostText = UI.find(v, R.id.socks5Host);
        final EditText socks5PortText = UI.find(v, R.id.socks5Port);

        final Switch switchEnableProxySettings = UI.find(v, R.id.switchEnableProxySettings);
        switchEnableProxySettings.setOnCheckedChangeListener(this::onProxyChange);
        initTor(mNetworksViewAdapter.getSelected());
        mSwitchTor.setOnCheckedChangeListener(this::onSwitchTor);

        final View selectButton = v.findViewById(R.id.selectNetworkButton);
        selectButton.setOnClickListener(view -> {
            final NetworkData selectedNetwork = mNetworksViewAdapter.getSelected();
            String networkName = selectedNetwork.getNetwork();
            final String socksHost = UI.getText(socks5HostText);
            //final String socksPort = UI.getText(socks5PortText);

            if (socksHost.startsWith("{")) {
                try {
                    final NetworkData newNetwork = (new ObjectMapper()).readValue(socksHost, NetworkData.class);
                    GDKSession.registerNetwork(newNetwork.getName(), socksHost);
                    networkName = newNetwork.getNetwork();
                } catch (final Exception e) {
                    UI.toast(getActivity(), e.getMessage(), Toast.LENGTH_LONG);
                }
            }
            // FIXME: Use host and port to connect if given
            getGAService().setCurrentNetworkId(networkName);
            getGAService().reconnect();  // FIXME another thread
            mListener.onSelectNetwork();
            dismiss();
        });

        return v;
    }

    private void onProxyChange(CompoundButton compoundButton, boolean b) {
        mProxySection.setVisibility(b ? View.VISIBLE : View.GONE);
    }

    private void onSwitchTor(CompoundButton compoundButton, boolean b) {
        getGAService().cfg().edit().putBoolean(PrefKeys.TOR_ENABLED, b).apply();
        getGAService().getConnectionManager().setTorEnabled(b);
    }

    private void initTor(final NetworkData selectedNetwork) {
        mSwitchTor.setChecked(getGAService().cfg().getBoolean(PrefKeys.TOR_ENABLED, false));
        mSwitchTor.setEnabled(!TextUtils.isEmpty(selectedNetwork.getWampOnionUrl()));
    }

}
