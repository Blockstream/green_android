package com.greenaddress.greenbits.ui.transactions;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.greenapi.data.AssetInfoData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.TransactionData;
import com.greenaddress.greenapi.data.TransactionData.TYPE;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenbits.spv.SPV;
import com.greenaddress.greenbits.ui.R.color;
import com.greenaddress.greenbits.ui.R.drawable;
import com.greenaddress.greenbits.ui.R.id;
import com.greenaddress.greenbits.ui.R.layout;
import com.greenaddress.greenbits.ui.R.string;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.components.FontAwesomeTextView;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;
import com.greenaddress.greenbits.ui.transactions.ListTransactionsAdapter.ViewHolder;

import java.io.IOException;
import java.text.DateFormat;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;
import static com.greenaddress.gdk.GDKSession.getSession;

public class ListTransactionsAdapter extends
    Adapter<ViewHolder> {

    private static final int REQUEST_TX_DETAILS = 4;
    private static final ObjectMapper mObjectMapper = new ObjectMapper();

    private final List<TransactionData> mTxItems;
    private final Activity mActivity;
    private final NetworkData mNetworkData;
    private final Model mModel;
    private final SPV mSPV;
    private final boolean isSpvEnabled;

    public ListTransactionsAdapter(final Activity activity,
                                   final NetworkData networkData,
                                   final List<TransactionData> txItems,
                                   final Model model,
                                   final SPV spv) {
        mTxItems = txItems;
        mActivity = activity;
        mNetworkData = networkData;
        mModel = model;
        final SharedPreferences preferences = activity.getSharedPreferences(mNetworkData.getNetwork(), MODE_PRIVATE);
        isSpvEnabled = preferences.getBoolean(PrefKeys.SPV_ENABLED, false);
        mSPV = spv;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                              .inflate(layout.list_element_transaction, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        if (position >= mTxItems.size()) {
            return;
        }
        final TransactionData txItem = mTxItems.get(position);

        // Remove to display fee as amount in liquid
        final Long btc = txItem.getSatoshi().get("btc");
        final Long fee = txItem.getFee();
        if (btc != null && btc.equals(fee) && txItem.getSatoshi().size() > 1) {
            txItem.getSatoshi().remove("btc");
        }

        // show assets amount or assets number
        final int assetsNumber = txItem.getSatoshi().size();
        if (assetsNumber == 1) {
            final String assetId = txItem.getSatoshi().keySet().iterator().next();
            holder.textValue.setText(getAmountWithUnit(txItem, mModel, assetId));
        } else {
            holder.textValue.setText(mActivity.getString(string.id_d_assets, assetsNumber));
        }

        // Hide question mark if we know this tx is verified
        // (or we are in watch only mode and so have no SPV_SYNCRONIZATION to verify it with)
        final boolean isSpvVerified = txItem.isSpent() ||
                                      txItem.getTxType() == TYPE.OUT ||
                                      !isSpvEnabled || (isSpvEnabled &&
                                                        mSPV.isSPVVerified(txItem.getTxhash()));

        holder.spvUnconfirmed.setVisibility(isSpvVerified ? View.GONE : View.VISIBLE);
        holder.textWhen.setTextColor(ContextCompat.getColor(mActivity, color.tertiaryTextColor));
        holder.textWhen.setText(txItem.getLocalizedDate(DateFormat.MEDIUM));

        final boolean replaceable = !mNetworkData.getLiquid() &&
                                    txItem.getCanRbf() && txItem.getTxType() != TYPE.IN;
        UI.showIf(replaceable && !mNetworkData.getLiquid(), holder.imageReplaceable);

        final String message;
        if (TextUtils.isEmpty(txItem.getMemo())) {
            if (mNetworkData.getLiquid() && assetsNumber > 1)
                message = mActivity.getString(string.id_multiple_assets);
            else if (mNetworkData.getLiquid() && txItem.isAsset()) {
                final String assetId =
                    txItem.getSatoshi().keySet().iterator().next();
                final AssetInfoData assetInfo = mModel.getAssetsObservable().getAssetsInfos().get(assetId);
                message = assetInfo != null ? assetInfo.getEntity().getDomain() : assetId;
            } else if (txItem.getTxType() == TYPE.REDEPOSIT)
                message = String.format("%s %s", mActivity.getString(
                                            string.id_redeposited),
                                        txItem.isAsset() ? mActivity.getString(string.id_asset) : "");
            else if (txItem.getTxType() == TYPE.IN)
                message = String.format("%s %s", mActivity.getString(
                                            string.id_received),
                                        txItem.isAsset() ? mActivity.getString(string.id_asset) : "");
            else
                message = mNetworkData.getLiquid() ? String.format("%s %s", mActivity.getString(
                                                                       string.id_sent),
                                                                   txItem.isAsset() ? mActivity.getString(
                                                                       string.id_asset) : "") : txItem.getAddressee();
        } else {
            if (txItem.getTxType() == TYPE.REDEPOSIT)
                message = String.format("%s %s", mActivity.getString(string.id_redeposited), txItem.getMemo());
            else
                message = txItem.getMemo();
        }
        holder.textWho.setText(message);

        final String confirmations;
        final int confirmationsColor;
        final int currentBlock = mModel.getBlockchainHeightObservable().getHeight();
        if (txItem.getConfirmations(currentBlock) == 0) {
            confirmations = mActivity.getString(string.id_unconfirmed);
            confirmationsColor = color.red;
        } else if (mNetworkData.getLiquid() && txItem.getConfirmations(currentBlock) < 2) {
            confirmations = mActivity.getString(string.id_12_confirmations);
            confirmationsColor = color.grey_light;
        } else if (!mNetworkData.getLiquid() && !txItem.hasEnoughConfirmations(currentBlock)) {
            confirmations = mActivity.getString(string.id_d6_confirmations, txItem.getConfirmations(currentBlock));
            confirmationsColor = color.grey_light;
        } else {
            confirmations = mActivity.getString(string.id_completed);
            confirmationsColor = color.grey_light;
        }

        holder.listNumberConfirmation.setText(confirmations);
        holder.listNumberConfirmation.setTextColor(getColor(confirmationsColor));

        final int amountColor;
        final int sentOrReceive;
        if (txItem.getTxType() == TYPE.IN) {
            amountColor = mNetworkData.getLiquid() ? color.liquidDark : color.green;
            sentOrReceive= drawable.ic_received;
        } else {
            amountColor = color.white;
            sentOrReceive= drawable.ic_sent;
        }
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
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

    public String getAmountWithUnit(final TransactionData tx, final Model model, final String assetId) {
        try {
            if (tx.getTxType() == TYPE.REDEPOSIT) {
                final String feeAmount = amountToString(tx.getFee(), model.getUnitKey(), null);
                return String.format("-%s %s", feeAmount, model.getBitcoinOrLiquidUnit());
            }

            AssetInfoData info = model.getAssetsObservable().getAssetsInfos().get(assetId);
            if (info == null)
                info = new AssetInfoData(assetId);
            final String amount = amountToString(tx.getSatoshi().get(assetId),
                                                 tx.isAsset() ? assetId : model.getUnitKey(),
                                                 tx.isAsset() ? info : null);
            final String denom =
                tx.isAsset() ? info.getTicker() !=
                null ? info.getTicker() : "" : model.getBitcoinOrLiquidUnit();
            return String.format("%s%s %s", tx.getTxType() == TYPE.OUT ? "-" : "", amount, denom);
        } catch (final RuntimeException | IOException e) {
            Log.e("", "Conversion error: " + e.getLocalizedMessage());
            return "";
        }
    }

    private String amountToString(final long satoshi, final String assetId,
                                  final AssetInfoData info) throws IOException {
        final ObjectNode details = mObjectMapper.createObjectNode();
        details.put("satoshi", satoshi);
        if (info != null) {
            details.set("asset_info", info.toObjectNode());
        }
        final ObjectNode converted = getSession().convert(details);
        return converted.get(assetId).asText();
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

            textValue = UI.find(v, id.listValueText);
            textWhen = UI.find(v, id.listWhenText);
            imageReplaceable = UI.find(v, id.listReplaceableIcon);
            spvUnconfirmed = UI.find(v, id.spvUnconfirmed);
            textWho = UI.find(v, id.listWhoText);
            mainLayout = UI.find(v, id.list_item_layout);
            sentOrReceive = UI.find(v, id.imageSentOrReceive);
            // TODO: For multiasset, enable unitText
            //if (GaService.IS_ELEMENTS)
            //    unitText = UI.find(v, R.id.listBitcoinUnitText);
            unitText = null;
            listNumberConfirmation = UI.find(v, id.listNumberConfirmation);
        }
    }
}
