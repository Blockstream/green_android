package com.greenaddress.greenbits.ui.hardwarewallets;

import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.greenaddress.greenbits.ui.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    public interface OnAdapterInterface {
        public void onItemClick(final DeviceEntry entry);
    }

    private final List<DeviceEntry> mList = new ArrayList<>();
    private OnAdapterInterface mListener;

    // Sort by name
    private static final Comparator<DeviceEntry> SORTING_COMPARATOR = (lhs, rhs) ->
            lhs.device.getName().compareTo(rhs.device.getName());

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
        final DeviceEntry entry = mList.get(position);
        holder.text.setText(entry.device.getName());
        holder.image.setImageResource(entry.imageResource);
        holder.itemView.setOnClickListener(view -> mListener.onItemClick(entry));
    }

    void clear() {
        mList.clear();
        notifyDataSetChanged();
    }

    void add(final ParcelUuid serviceId, final int imageResource, final BluetoothDevice device, final long ts) {
        if (serviceId == null || device == null || device.getName() == null || device.getAddress() == null) {
            return;
        }

        // If already in list, just update timestamp
        for (final DeviceEntry entry : mList) {
            if (entry.device == device) {
                entry.setTimestamp(ts);
                return;
            }
        }

        // If not, add new device
        mList.add(new DeviceEntry(serviceId, imageResource, device, ts));
        Collections.sort(mList, SORTING_COMPARATOR);
        notifyDataSetChanged();
    }

    void removeStale(final long tsLimit) {
        boolean updated = false;
        final ListIterator<DeviceEntry> iter = mList.listIterator();
        while(iter.hasNext()){
            final DeviceEntry entry = iter.next();
            if(entry.timestamp < tsLimit){
                updated = true;
                iter.remove();
            }
        }

        if (updated) {
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {

        final TextView text;
        final ImageView image;

        DeviceViewHolder(final View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.text);
            image = itemView.findViewById(R.id.icon);
        }
    }

    static class DeviceEntry {
        final ParcelUuid serviceId;
        final int imageResource;
        final BluetoothDevice device;
        private long timestamp;

        DeviceEntry(final ParcelUuid serviceId, final int imageResource, final BluetoothDevice device, final long ts) {
            this.serviceId = serviceId;
            this.imageResource = imageResource;
            this.device = device;
            this.timestamp = ts;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(final long timestamp) {
            this.timestamp = timestamp;
        }
    }
}