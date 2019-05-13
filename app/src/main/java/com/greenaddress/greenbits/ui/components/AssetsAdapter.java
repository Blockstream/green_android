package com.greenaddress.greenbits.ui.components;

import android.app.Activity;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AssetsAdapter extends RecyclerView.Adapter<AssetsAdapter.Item> {

    private final Map<String, BalanceData> mAssets;
    private final List<String> mAssetsIds;
    private final OnAssetSelected mOnAccountSelected;
    private final GaService mService;
    private final Resources mResources;
    private final Activity mActivity;

    public interface OnAssetSelected {
        void onAssetSelected(String assetSelected);
    }

    public AssetsAdapter(final Map<String, BalanceData> assets, final GaService service,
                         final OnAssetSelected cb, final Resources resources, final Activity activity) {
        mAssets = assets;
        mAssetsIds = new ArrayList<>(assets.keySet()); // TODO custom ordering?
        mService = service;
        mOnAccountSelected = cb;
        mResources = resources;
        mActivity = activity;
    }

    @Override
    public Item onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                          .inflate(R.layout.list_element_asset, parent, false);
        return new Item(view);
    }

    @Override
    public void onBindViewHolder(final Item holder, final int position) {
        final String assetId = mAssetsIds.get(position);
        holder.mAssetLayout.setOnClickListener(v -> mOnAccountSelected.onAssetSelected(assetId));
        holder.mAssetName.setText(mService.getAssetName(assetId));
        long satoshi = mAssets.get(assetId).getSatoshi();
        holder.mAssetValue.setText(mService.getValueString(satoshi, false, "btc".equals(assetId)));
    }

    @Override
    public int getItemCount() {
        return mAssets.size();
    }

    static class Item extends RecyclerView.ViewHolder {

        final LinearLayout mAssetLayout;
        final TextView mAssetName;
        final TextView mAssetValue;

        Item(final View v) {
            super(v);
            mAssetLayout = v.findViewById(R.id.assetLayout);
            mAssetName = v.findViewById(R.id.assetName);
            mAssetValue = v.findViewById(R.id.assetValue);
        }
    }
}
