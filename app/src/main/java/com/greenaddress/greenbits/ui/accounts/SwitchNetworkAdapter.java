package com.greenaddress.greenbits.ui.accounts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import java.util.List;
import androidx.recyclerview.widget.RecyclerView;

public class SwitchNetworkAdapter extends RecyclerView.Adapter<SwitchNetworkAdapter.ViewHolder> {

    private final List<NetworkData> mNetworkList;
    private final Context mContext;
    private int mSelectedItem;
    private final NetworkSwitchListener mNetworkSwitchListener;

    public SwitchNetworkAdapter(final Context context, final List<NetworkData> networkList,
                                final NetworkData selectedItem,
                                final NetworkSwitchListener networkSwitchListener) {
        mNetworkList = networkList;
        mContext = context;
        mSelectedItem = networkList.indexOf(selectedItem);
        mNetworkSwitchListener = networkSwitchListener;
    }

    @Override
    public SwitchNetworkAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        return new SwitchNetworkAdapter.ViewHolder(LayoutInflater.from(parent.getContext())
                                                   .inflate(R.layout.list_element_switch_network, parent, false));
    }

    @Override
    public void onBindViewHolder(final SwitchNetworkAdapter.ViewHolder holder, final int position) {
        final NetworkData networkData = mNetworkList.get(position);
        holder.setText(networkData.getName());
        holder.setIcon(networkData.getIcon());
        holder.setSelected(position == mSelectedItem);
        holder.mButton.setOnClickListener(view -> {
            mSelectedItem = holder.getAdapterPosition();
            notifyItemRangeChanged(0, mNetworkList.size());
            mNetworkSwitchListener.onNetworkClick(networkData);
        });
    }

    @Override
    public int getItemCount() {
        return mNetworkList == null ? 0 : mNetworkList.size();
    }

    public NetworkData getSelected() {
        return mNetworkList.get( mSelectedItem );
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final Button mButton;

        ViewHolder(final View itemView) {
            super(itemView);
            mButton = UI.find(itemView, R.id.switchNetworkButton);
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

        public String getText() {
            return mButton.getText().toString();
        }
    }
}

