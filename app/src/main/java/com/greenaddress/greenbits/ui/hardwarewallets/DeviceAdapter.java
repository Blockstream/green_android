package com.greenaddress.greenbits.ui.hardwarewallets;

import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.greenaddress.greenbits.ui.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    public interface OnAdapterInterface {
        public void onItemClick(final Pair<ParcelUuid, BluetoothDevice> info);
    }

    private final List<Pair<ParcelUuid, BluetoothDevice>> mList = new ArrayList<>();
    private OnAdapterInterface mListener;

    // Sort by name
    private static final Comparator<Pair<ParcelUuid, BluetoothDevice>> SORTING_COMPARATOR = (lhs, rhs) ->
            lhs.second.getName().compareTo(rhs.second.getName());

    void setOnAdapterInterface(final OnAdapterInterface listener) {
        this.mListener = listener;
    }

    @NonNull
    @Override
    public DeviceAdapter.DeviceViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_element_device, parent, false);
        return new DeviceViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final DeviceAdapter.DeviceViewHolder holder, final int position) {
        final Pair<ParcelUuid, BluetoothDevice> info = mList.get(position);
        holder.text.setText(info.second.getName());
        holder.itemView.setOnClickListener(view -> mListener.onItemClick(info));
    }

    public void clear() {
        mList.clear();
        notifyDataSetChanged();
    }

    public void add(final Pair<ParcelUuid, BluetoothDevice> info) {
        if (info.first == null || info.second == null || info.second.getName() == null || info.second.getAddress() == null) {
            return;
        }

        // Ignore if already in list
        if (mList.contains(info)) {
            return;
        }

        // Add new device
        mList.add(info);
        Collections.sort(mList, SORTING_COMPARATOR);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {

        protected TextView text;

        public DeviceViewHolder(final View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.text);
        }
    }
}