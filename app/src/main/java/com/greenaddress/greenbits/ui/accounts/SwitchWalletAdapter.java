package com.greenaddress.greenbits.ui.accounts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import java.util.HashMap;
import java.util.List;


public class SwitchWalletAdapter extends RecyclerView.Adapter<SwitchWalletAdapter.ViewHolder> {

    private final List<HashMap<String, String>> mWalletList;
    private final Context mContext;
    private final WalletSwitchListener mWalletSwitchListener;

    public SwitchWalletAdapter(final Context context, final List<HashMap<String, String>> walletList,
                               final WalletSwitchListener networkSwitchListener) {
        mWalletList = walletList;
        mContext = context;
        mWalletSwitchListener = networkSwitchListener;
    }

    @Override
    public SwitchWalletAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        return new SwitchWalletAdapter.ViewHolder(LayoutInflater.from(parent.getContext())
                                                   .inflate(R.layout.list_element_switch_network, parent, false));
    }

    public int getIcon(String network) {
        if (network.equals("mainnet") || network.equals("electrum-mainnet"))
            return com.blockstream.crypto.R.drawable.ic_btc;
        if (network.equals("testnet") || network.equals("electrum-testnet"))
            return com.blockstream.crypto.R.drawable.ic_testnet_btc;
        if (network.equals("localtest-liquid"))
            return com.blockstream.crypto.R.drawable.ic_liquid;
        if (network.equals("liquid"))
            return com.blockstream.crypto.R.drawable.ic_liquid;
        return com.blockstream.crypto.R.drawable.ic_testnet_btc;
    }

    @Override
    public void onBindViewHolder(final SwitchWalletAdapter.ViewHolder holder, final int position) {

        final HashMap<String, String> wallet = mWalletList.get(position);
        holder.setText(wallet.get("name"));
        holder.setIcon(getIcon(wallet.get("network")));
        Boolean isActive = "true".equals(wallet.get("active"));
        holder.setSelected(isActive);
        holder.mButton.setOnClickListener(view -> {
            mWalletSwitchListener.onWalletClick(isActive ? null : Long.parseLong(wallet.get("id")));
        });
        UI.showIf(isActive, holder.mLogout);
    }

    @Override
    public int getItemCount() {
        return mWalletList == null ? 0 : mWalletList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final Button mButton;
        private final TextView mLogout;

        ViewHolder(final View itemView) {
            super(itemView);
            mButton = UI.find(itemView, R.id.switchNetworkButton);
            mLogout = UI.find(itemView, R.id.switchNetworkLogout);
        }

        public void setText(final String text) {
            mButton.setText(text);
        }

        public void setIcon(final int resource) {
            final Drawable icon = mContext.getResources().getDrawable(resource);
            mButton.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }

        public void setSelected(final boolean selected) {
            mButton.setPressed(selected);
        }
    }
}

