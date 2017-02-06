package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Coin;

import java.util.List;

public class ListTransactionsAdapter extends
        RecyclerView.Adapter<ListTransactionsAdapter.ViewHolder> {

    private final static int REQUEST_TX_DETAILS = 4;

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
        final Coin coin = Coin.valueOf(txItem.amount);
        UI.setCoinText(mService, holder.unitText, holder.textValue, coin);

        // Hide question mark if we know this tx is verified
        // (or we are in watch only mode and so have no SPV to verify it with)
        final boolean verified = txItem.spvVerified || txItem.isSpent ||
                                 txItem.type == TransactionItem.TYPE.OUT ||
                                 !mService.isSPVEnabled();
        UI.hideIf(verified, holder.textValueQuestionMark);

        final Resources res = mActivity.getResources();

        if (txItem.doubleSpentBy == null) {
            holder.textWhen.setTextColor(res.getColor(R.color.tertiaryTextColor));
            holder.textWhen.setText(TimeAgo.fromNow(txItem.date.getTime(), mActivity));
        } else {
            switch (txItem.doubleSpentBy) {
                case "malleability":
                    holder.textWhen.setTextColor(Color.parseColor("#FF8000"));
                    holder.textWhen.setText(res.getText(R.string.malleated));
                    break;
                case "update":
                    holder.textWhen.setTextColor(Color.parseColor("#FF8000"));
                    holder.textWhen.setText(res.getText(R.string.updated));
                    break;
                default:
                    holder.textWhen.setTextColor(Color.RED);
                    holder.textWhen.setText(res.getText(R.string.doubleSpend));
            }
        }

        UI.showIf(txItem.replaceable, holder.textReplaceable);

        final boolean humanCpty = txItem.type == TransactionItem.TYPE.OUT &&
                txItem.counterparty != null && txItem.counterparty.length() > 0 &&
                !GaService.isValidAddress(txItem.counterparty);

        final String message;
        if (TextUtils.isEmpty(txItem.memo)) {
            if (humanCpty)
                message = txItem.counterparty;
            else
                message = getTypeString(txItem.type);
        } else {
            if (humanCpty)
                message = String.format("%s %s", txItem.counterparty, txItem.memo);
            else
                message = txItem.memo;
        }

        holder.textWho.setText(message);

        final int color = txItem.amount > 0 ? R.color.superLightGreen : R.color.superLightPink;
        holder.mainLayout.setBackgroundColor(res.getColor(color));

        if (txItem.hasEnoughConfirmations()) {
            final int glyph = txItem.amount > 0 ? R.string.fa_sign_in : R.string.fa_sign_out;
            holder.inOutIcon.setText(glyph);
            UI.hide(holder.listNumberConfirmation);
        } else {
            holder.inOutIcon.setText(R.string.fa_clock_o);
            UI.show(holder.listNumberConfirmation);
            holder.listNumberConfirmation.setText(String.valueOf(txItem.getConfirmations()));
        }

        holder.mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Intent transactionActivity = new Intent(mActivity, TransactionActivity.class);
                transactionActivity.putExtra("TRANSACTION", txItem);
                mActivity.startActivityForResult(transactionActivity, REQUEST_TX_DETAILS);
            }
        });
    }

    private String getTypeString(final TransactionItem.TYPE type) {
        switch (type) {
            case IN:
                return mActivity.getString(R.string.txTypeIn);
            case OUT:
                return mActivity.getString(R.string.txTypeOut);
            case REDEPOSIT:
                return mActivity.getString(R.string.txTypeRedeposit);
            default:
                return "No type";
        }
    }

    @Override
    public int getItemCount() {
        return mTxItems == null ? 0 : mTxItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView listNumberConfirmation;
        public final TextView textValue;
        public final TextView textWhen;
        public final TextView textReplaceable;
        public final TextView unitText;
        public final TextView textWho;
        public final TextView inOutIcon;
        public final TextView textValueQuestionMark;
        public final LinearLayout mainLayout;

        public ViewHolder(final View v) {

            super(v);

            textValue = UI.find(v, R.id.listValueText);
            textValueQuestionMark = UI.find(v, R.id.listValueQuestionMark);
            textWhen = UI.find(v, R.id.listWhenText);
            textReplaceable = UI.find(v, R.id.listReplaceableText);
            textWho = UI.find(v, R.id.listWhoText);
            inOutIcon = UI.find(v, R.id.listInOutIcon);
            mainLayout = UI.find(v, R.id.list_item_layout);
            unitText = UI.find(v, R.id.listBitcoinUnitText);
            listNumberConfirmation = UI.find(v, R.id.listNumberConfirmation);
        }
    }
}
