package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.MonetaryFormat;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.List;

class ListTransactionsAdapter extends ArrayAdapter<Transaction> {
    private final String btcUnit;

    public ListTransactionsAdapter(final Context context, final int resource, @NonNull final List<Transaction> objects, final String btcUnit) {
        super(context, resource, objects);
        this.btcUnit = btcUnit;
    }

    @Nullable
    @Override
    public View getView(final int position, @Nullable final View convertView, final ViewGroup parent) {

        final Transaction current = getItem(position);
        Holder holder;

        View returnedView;
        if (convertView == null) {
            final LayoutInflater inflater = LayoutInflater.from(getContext());
            returnedView = inflater.inflate(R.layout.list_element_transaction, parent, false);
            holder = new Holder();
            holder.textValue = (TextView) returnedView.findViewById(R.id.listValueText);
            holder.textValueQuestionMark = (TextView) returnedView.findViewById(R.id.listValueQuestionMark);
            holder.textWhen = (TextView) returnedView.findViewById(R.id.listWhenText);
            holder.textReplaceable = (TextView) returnedView.findViewById(R.id.listReplaceableText);
            holder.textWho = (TextView) returnedView.findViewById(R.id.listWhoText);
            holder.inOutIcon = (TextView) returnedView.findViewById(R.id.listInOutIcon);
            holder.mainLayout = (RelativeLayout) returnedView.findViewById(R.id.list_item_layout);
            holder.bitcoinIcon = (TextView) returnedView.findViewById(R.id.listBitcoinIcon);
            holder.bitcoinScale = (TextView) returnedView.findViewById(R.id.listBitcoinScaleText);
            holder.listNumberConfirmation = (TextView) returnedView.findViewById(R.id.listNumberConfirmation);
            returnedView.setTag(holder);

        } else {
            returnedView = convertView;
            holder = (Holder) returnedView.getTag();
        }

        final long val = current.amount;
        final Coin coin = Coin.valueOf(val);
        final MonetaryFormat bitcoinFormat = CurrencyMapper.mapBtcUnitToFormat(btcUnit);
        holder.bitcoinScale.setText(Html.fromHtml(CurrencyMapper.mapBtcUnitToPrefix(btcUnit)));
        if (btcUnit == null || btcUnit.equals("bits")) {
            holder.bitcoinIcon.setText("");
            holder.bitcoinScale.setText("bits ");
        } else {
            holder.bitcoinIcon.setText(Html.fromHtml("&#xf15a; "));
        }

        final String btcBalance = bitcoinFormat.noCode().format(coin).toString();
        final DecimalFormat formatter = new DecimalFormat("#,###.########");
        try {
            holder.textValue.setText(formatter.format(formatter.parse(btcBalance)));
        } catch (@NonNull final ParseException e) {
            holder.textValue.setText(btcBalance);
        }

        if (!getContext().getSharedPreferences("SPV", Context.MODE_PRIVATE).getBoolean("enabled", true) ||
                current.spvVerified || current.isSpent || current.type.equals(Transaction.TYPE.OUT)) {
            holder.textValueQuestionMark.setVisibility(View.GONE);
        } else {
            holder.textValueQuestionMark.setVisibility(View.VISIBLE);
        }

        if (current.doubleSpentBy == null) {
            holder.textWhen.setTextColor(getContext().getResources().getColor(R.color.tertiaryTextColor));
            holder.textWhen.setText(TimeAgo.fromNow(current.date.getTime(), getContext()));
        } else {
            switch (current.doubleSpentBy) {
                case "malleability":
                    holder.textWhen.setTextColor(Color.parseColor("#FF8000"));
                    holder.textWhen.setText(getContext().getResources().getText(R.string.malleated));
                    break;
                case "update":
                    holder.textWhen.setTextColor(Color.parseColor("#FF8000"));
                    holder.textWhen.setText(getContext().getResources().getText(R.string.updated));
                    break;
                default:
                    holder.textWhen.setTextColor(Color.RED);
                    holder.textWhen.setText(getContext().getResources().getText(R.string.doubleSpend));
            }
        }

        if (!current.replaceable) {
            holder.textReplaceable.setVisibility(View.GONE);
        } else {
            holder.textReplaceable.setVisibility(View.VISIBLE);
        }

        String message;
        if (current.type.equals(Transaction.TYPE.OUT) && current.counterparty != null && current.counterparty.length() > 0) {
            message = current.counterparty;
        } else {
            message = getTypeString(current.type);
        }
        holder.textWho.setText(String.format("%s%s", message, current.memo != null ? " *" : ""));

        holder.mainLayout.setBackgroundColor(val > 0 ?
                        getContext().getResources().getColor(R.color.superLightGreen) :
                        getContext().getResources().getColor(R.color.superLightPink)

        );

        if (current.hasEnoughConfirmations()) {
            holder.inOutIcon.setText(val > 0 ?
                            Html.fromHtml("&#xf090;") :
                            Html.fromHtml("&#xf08b;")
            );
            holder.listNumberConfirmation.setVisibility(View.GONE);
        } else {
            holder.inOutIcon.setText(Html.fromHtml("&#xf017;"));
            holder.listNumberConfirmation.setVisibility(View.VISIBLE);
            holder.listNumberConfirmation.setText(String.valueOf(current.getConfirmations()));

        }

        returnedView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Intent transactionActivity = new Intent(getContext(), TransactionActivity.class);

                transactionActivity.putExtra("TRANSACTION", current);
                getContext().startActivity(transactionActivity);
            }
        });

        return returnedView;
    }

    private String getTypeString(@NonNull final Transaction.TYPE type) {
        switch (type) {
            case IN:
                return getContext().getString(R.string.txTypeIn);
            case OUT:
                return getContext().getString(R.string.txTypeOut);
            case REDEPOSIT:
                return getContext().getString(R.string.txTypeRedeposit);
            default:
                return "No type";
        }
    }

    private static class Holder {
        public TextView listNumberConfirmation;
        TextView textValue;
        TextView textWhen;
        TextView textReplaceable;
        TextView bitcoinIcon;
        TextView textWho;
        TextView inOutIcon;
        TextView bitcoinScale;
        TextView textValueQuestionMark;
        RelativeLayout mainLayout;
    }
}
