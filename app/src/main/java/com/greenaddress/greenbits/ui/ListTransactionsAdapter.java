package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Coin;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        holder.textValue.setText(UI.formatCoinValueWithUnit(mService, coin));

        // Hide question mark if we know this tx is verified
        // (or we are in watch only mode and so have no SPV_SYNCRONIZATION to verify it with)
        final boolean spvVerified = txItem.spvVerified || txItem.isSpent ||
                                 txItem.type == TransactionItem.TYPE.OUT ||
                                 !mService.isSPVEnabled();

        holder.spvUnconfirmed.setVisibility(spvVerified ? View.GONE : View.VISIBLE);

        final Resources res = mActivity.getResources();

        if (txItem.doubleSpentBy == null) {
            holder.textWhen.setTextColor(ContextCompat.getColor(mActivity, R.color.tertiaryTextColor));
            holder.textWhen.setText(getTxTime(txItem));
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

        UI.showIf(txItem.replaceable, holder.textReplaceable);

        final boolean humanCpty = txItem.type == TransactionItem.TYPE.OUT &&
                                  txItem.counterparty != null && !txItem.counterparty.isEmpty() /*&&
                                                                                                   !GaService.isValidAddress(txItem.counterparty, mService.getNetwork())*/;

        final String message;
        if (TextUtils.isEmpty(txItem.memo)) {
            if (txItem.type == TransactionItem.TYPE.REDEPOSIT)
                message = mActivity.getString(R.string.id_redeposited);
            else if (txItem.type == TransactionItem.TYPE.IN)
                message = mActivity.getString(R.string.id_received);
            else
                message = txItem.counterparty;
        } else {
            if (txItem.type == TransactionItem.TYPE.REDEPOSIT)
                message = String.format("%s %s", mActivity.getString(R.string.id_redeposited),
                                        txItem.memo);
            else if (humanCpty)
                message = String.format("%s %s", txItem.counterparty, txItem.memo);
            else
                message = txItem.memo;
        }
        holder.textWho.setText(message);

        final String confirmations;
        final int confirmationsColor;
        if (txItem.getConfirmations() == 0) {
            confirmations = mActivity.getString(R.string.id_unconfirmed);
            confirmationsColor = R.color.red;
        } else if (!txItem.hasEnoughConfirmations()) {
            confirmations = mActivity.getString(R.string.id_d6_confirmations, txItem.getConfirmations());
            confirmationsColor = R.color.grey_light;
        } else {
            confirmations = mActivity.getString(R.string.id_completed);
            confirmationsColor = R.color.grey_light;
        }
        holder.listNumberConfirmation.setText(confirmations);
        holder.listNumberConfirmation.setTextColor(getColor(confirmationsColor));

        final int amountColor;
        if (txItem.amount > 0)
            amountColor = R.color.green;
        else
            amountColor = R.color.white;
        holder.textValue.setTextColor(getColor(amountColor));

        holder.mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Intent transactionActivity = new Intent(mActivity, TransactionActivity.class);
                transactionActivity.putExtra("TRANSACTION", txItem);
                mActivity.startActivityForResult(transactionActivity, REQUEST_TX_DETAILS);
            }
        });
    }

    @NonNull
    private String getTxTime(TransactionItem txItem) {
        final SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.US);
        final DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
        final Date txDate = txItem.date;
        final Date now = new Date();
        final String formatTxDate = dateFormatter.format(txDate);
        final String formatNow = dateFormatter.format(now);

        if (formatNow.equals(formatTxDate)) {
            return TimeAgo.fromNow(txDate.getTime(), mActivity);  //eg "7 minutes ago"
        } else {
            if (yearFormat.format(txDate).equals(yearFormat.format(now))) {
                int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR;
                String monthAndDayText = DateUtils.formatDateTime(mActivity, txDate.getTime(), flags);
                return monthAndDayText;  //eg "October 29"
            } else {
                return formatTxDate; //eg "January 29, 2017"
            }
        }
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
        public final TextView textReplaceable;
        public final TextView spvUnconfirmed;
        public final FontAwesomeTextView unitText;
        public final TextView textWho;
        public final LinearLayout mainLayout;

        public ViewHolder(final View v) {

            super(v);

            textValue = UI.find(v, R.id.listValueText);
            textWhen = UI.find(v, R.id.listWhenText);
            textReplaceable = UI.find(v, R.id.listReplaceableText);
            spvUnconfirmed = UI.find(v, R.id.spvUnconfirmed);
            textWho = UI.find(v, R.id.listWhoText);
            mainLayout = UI.find(v, R.id.list_item_layout);
            // TODO: For multiasset, enable unitText
            //if (GaService.IS_ELEMENTS)
            //    unitText = UI.find(v, R.id.listBitcoinUnitText);
            unitText = null;
            listNumberConfirmation = UI.find(v, R.id.listNumberConfirmation);
        }
    }
}
