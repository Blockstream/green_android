package com.greenaddress.greenbits.ui.transactions;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.greenaddress.greenapi.data.AssetInfoData;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.components.FontAwesomeTextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ListTransactionsAdapter extends
    RecyclerView.Adapter<ListTransactionsAdapter.ViewHolder> {

    private static final int REQUEST_TX_DETAILS = 4;

    private final List<TransactionItem> mTxItems;
    private final Activity mActivity;
    private final GaService mService;

    public ListTransactionsAdapter(final Activity activity, final GaService service,
                                   final List<TransactionItem> txItems) {
        mTxItems = txItems;
        mActivity = activity;
        mService = service;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                              .inflate(R.layout.list_element_transaction, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        final TransactionItem txItem = mTxItems.get(position);
        final int assetsNumber = txItem.getAssetBalances().size();
        if (assetsNumber > 1) {
            holder.textValue.setText(mActivity.getString(R.string.id_d_assets,assetsNumber));
        } else {
            final String assetId = txItem.mAssetBalances.keySet().toArray(new String[0])[0];
            holder.textValue.setText(txItem.getAmountWithUnit(mService, assetId));
        }
        // Hide question mark if we know this tx is verified
        // (or we are in watch only mode and so have no SPV_SYNCRONIZATION to verify it with)
        final boolean spvVerified = txItem.spvVerified || txItem.isSpent ||
                                    txItem.type == TransactionItem.TYPE.OUT ||
                                    !mService.isSPVEnabled();

        holder.spvUnconfirmed.setVisibility(spvVerified ? View.GONE : View.VISIBLE);

        final Resources res = mActivity.getResources();

        if (txItem.doubleSpentBy == null) {
            holder.textWhen.setTextColor(ContextCompat.getColor(mActivity, R.color.tertiaryTextColor));
            holder.textWhen.setText(txItem.getLocalizedDate(DateFormat.MEDIUM));
        } else {
            switch (txItem.doubleSpentBy) {
            case "malleability":
                holder.textWhen.setTextColor(Color.parseColor("#FF8000"));
                holder.textWhen.setText(res.getText(R.string.id_malleated));
                break;
            case "update":
                holder.textWhen.setTextColor(Color.parseColor("#FF8000"));
                holder.textWhen.setText(res.getText(R.string.id_updated));
                break;
            default:
                holder.textWhen.setTextColor(Color.RED);
                holder.textWhen.setText(res.getText(R.string.id_double_spend));
            }
        }

        UI.showIf(txItem.replaceable && !mService.isLiquid(), holder.imageReplaceable);

        final String message;
        if (TextUtils.isEmpty(txItem.memo)) {
            if (mService.isLiquid() && assetsNumber > 1)
                message = mActivity.getString(R.string.id_multiple_assets);
            else if (mService.isLiquid() && txItem.isAsset) {
                final String assetId =
                    txItem.mAssetBalances.keySet().toArray(new String[0])[0];
                final AssetInfoData assetInfo = mService.getModel().getAssetsObservable().getAssetsInfos().get(assetId);
                message = assetInfo != null ? assetInfo.getEntity().getDomain() : assetId;
            } else if (txItem.type == TransactionItem.TYPE.REDEPOSIT)
                message = String.format("%s %s", mActivity.getString(
                                            R.string.id_redeposited),
                                        txItem.isAsset ? mActivity.getString(R.string.id_asset) : "");
            else if (txItem.type == TransactionItem.TYPE.IN)
                message = String.format("%s %s", mActivity.getString(
                                            R.string.id_received),
                                        txItem.isAsset ? mActivity.getString(R.string.id_asset) : "");
            else
                message = mService.isLiquid() ? String.format("%s %s", mActivity.getString(
                                                                  R.string.id_sent),
                                                              txItem.isAsset ? mActivity.getString(
                                                                  R.string.id_asset) : "") : txItem.counterparty;
        } else {
            if (txItem.type == TransactionItem.TYPE.REDEPOSIT)
                message = String.format("%s %s", mActivity.getString(R.string.id_redeposited), txItem.memo);
            else
                message = txItem.memo;
        }
        holder.textWho.setText(message);

        final String confirmations;
        final int confirmationsColor;
        if (txItem.getConfirmations() == 0) {
            confirmations = mActivity.getString(R.string.id_unconfirmed);
            confirmationsColor = R.color.red;
        } else if (mService.isLiquid() && txItem.getConfirmations() < 2) {
            confirmations = mActivity.getString(R.string.id_12_confirmations);
            confirmationsColor = R.color.grey_light;
        } else if (!mService.isLiquid() && !txItem.hasEnoughConfirmations()) {
            confirmations = mActivity.getString(R.string.id_d6_confirmations, txItem.getConfirmations());
            confirmationsColor = R.color.grey_light;
        } else {
            confirmations = mActivity.getString(R.string.id_completed);
            confirmationsColor = R.color.grey_light;
        }

        holder.listNumberConfirmation.setText(confirmations);
        holder.listNumberConfirmation.setTextColor(getColor(confirmationsColor));

        final int amountColor;
        final int sentOrReceive;
        if (txItem.type == TransactionItem.TYPE.IN) {
            amountColor = mService.isLiquid() ? R.color.liquidDark : R.color.green;
            sentOrReceive=R.drawable.ic_received;
        } else {
            amountColor = R.color.white;
            sentOrReceive=R.drawable.ic_sent;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.sentOrReceive.setImageDrawable(mActivity.getResources().getDrawable(sentOrReceive,
                                                                                       mActivity.getTheme()));
        } else {
            holder.sentOrReceive.setImageDrawable(mActivity.getResources().getDrawable(sentOrReceive));
        }
        holder.textValue.setTextColor(getColor(amountColor));

        holder.mainLayout.setOnClickListener(v -> {
            final Intent transactionActivity = new Intent(mActivity, TransactionActivity.class);
            transactionActivity.putExtra("TRANSACTION", txItem);
            mActivity.startActivityForResult(transactionActivity, REQUEST_TX_DETAILS);
        });
    }


    private int getColor(final int resource) {
        return mActivity.getResources().getColor(resource);
    }

    @Override
    public int getItemCount() {
        return mTxItems == null ? 0 : mTxItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView listNumberConfirmation;
        public final TextView textValue;
        public final TextView textWhen;
        public final ImageView imageReplaceable;
        public final TextView spvUnconfirmed;
        public final FontAwesomeTextView unitText;
        public final TextView textWho;
        public final LinearLayout mainLayout;
        public final ImageView sentOrReceive;

        public ViewHolder(final View v) {

            super(v);

            textValue = UI.find(v, R.id.listValueText);
            textWhen = UI.find(v, R.id.listWhenText);
            imageReplaceable = UI.find(v, R.id.listReplaceableIcon);
            spvUnconfirmed = UI.find(v, R.id.spvUnconfirmed);
            textWho = UI.find(v, R.id.listWhoText);
            mainLayout = UI.find(v, R.id.list_item_layout);
            sentOrReceive = UI.find(v, R.id.imageSentOrReceive);
            // TODO: For multiasset, enable unitText
            //if (GaService.IS_ELEMENTS)
            //    unitText = UI.find(v, R.id.listBitcoinUnitText);
            unitText = null;
            listNumberConfirmation = UI.find(v, R.id.listNumberConfirmation);
        }
    }
}
