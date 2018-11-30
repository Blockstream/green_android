package com.greenaddress.greenbits.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

import java.util.Arrays;
import java.util.HashMap;

public class NetworkSettingsFragment extends DialogFragment {

    class NetworksViewAdapter extends RecyclerView.Adapter<NetworksViewAdapter.ViewHolder> {

        private HashMap<String, NetworkData> mData;
        private String[] mKeys;
        private int mSelectedItem = -1;
        private LayoutInflater mInflater;

        NetworksViewAdapter(final Context context, final HashMap<String, NetworkData> data, String selectedItem) {
            mInflater = LayoutInflater.from(context);
            mData = data;
            mKeys = mData.keySet().toArray(new String[data.size()]);
            mSelectedItem = Arrays.asList(mKeys).indexOf(selectedItem);
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
            final String networkId = mKeys[position];
            final NetworkData networkData = mData.get(networkId);
            holder.setText(networkData.getName());
            holder.setIcon(networkData.getIcon());
            holder.setSelected(position == mSelectedItem);
            holder.itemView.setOnClickListener(view -> {
                mSelectedItem = holder.getAdapterPosition();
                notifyItemRangeChanged(0, mKeys.length);
            });
        }

        @Override
        public int getItemCount() {
            return mData.size();
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
                mButton.setCompoundDrawablesWithIntrinsicBounds(null, top, null, null);
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

    protected GaActivity getGaActivity() {
        return (GaActivity) getActivity();
    }

    protected GaService getGAService() {
        return mApp.mService;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_networksettings, container, false);

        final RecyclerView recyclerView = UI.find(v, R.id.networksRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        final HashMap<String, NetworkData> networks = GDKSession.getNetworks();

        mNetworksViewAdapter = new NetworksViewAdapter(getContext(), networks, getNetworksDefault());
        recyclerView.setAdapter(mNetworksViewAdapter);

        final EditText socks5HostText = UI.find(v, R.id.socks5Host);
        final EditText socks5PortText = UI.find(v, R.id.socks5Port);

        final View selectButton = v.findViewById(R.id.selectNetworkButton);
        selectButton.setOnClickListener(view -> {
            String networkName = mNetworksViewAdapter.mKeys[mNetworksViewAdapter.mSelectedItem];
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

    private String getNetworksDefault(){
        return getGAService().getNetwork().getNetwork();
    }

}
