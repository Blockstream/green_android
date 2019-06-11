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

    @FunctionalInterface
    public interface OnAssetSelected {
        void onAssetSelected(String assetSelected);
    }

    public AssetsAdapter(final Map<String, BalanceData> assets, final GaService service,
                         final OnAssetSelected cb) {
        mAssets = assets;
        mService = service;
        mOnAccountSelected = cb;
        mAssetsIds = new ArrayList<>(mAssets.keySet());
        if (mAssetsIds.contains("btc")) {
            // Move btc as first in the list
            mAssetsIds.remove("btc");
            mAssetsIds.add(0,"btc");
        }
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
        final BalanceData balance = mAssets.get(assetId);
        final String label = balance.getAssetInfo() != null ? balance.getAssetInfo().getName() : assetId;
        final String ticker = balance.getAssetInfo() != null ? balance.getAssetInfo().getTicker() : "";
        // TODO: make asset conversion with precision
        final long satoshi = balance.getSatoshi();
        final String amount = mService.getValueString(satoshi, false, false);
        holder.mAssetName.setText("btc".equals(assetId) ? "L-BTC" : label);
        holder.mAssetValue.setText(amount + " " + ("btc".equals(assetId) ? "L-BTC" : ticker));
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
