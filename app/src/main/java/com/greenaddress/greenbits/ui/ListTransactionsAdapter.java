package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.content.Intent;
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

public class ListTransactionsAdapter extends ArrayAdapter<Transaction> {
    private String btcUnit;

    public ListTransactionsAdapter(final Context context, final int resource, final List<Transaction> objects, final String btcUnit) {
        super(context, resource, objects);
        this.btcUnit = btcUnit;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {

        final Transaction current = getItem(position);
        Holder holder;

        //Log.i("hi", "getView " + position + " " + convertView);
        View returnedView;
        if (convertView == null) {
            final LayoutInflater inflater = LayoutInflater.from(getContext());
            returnedView = inflater.inflate(R.layout.list_element_transaction, parent, false);
            holder = new Holder();
            holder.current = current;
            holder.textValue = (TextView) returnedView.findViewById(R.id.listValueText);
            holder.textValueQuestionMark = (TextView) returnedView.findViewById(R.id.listValueQuestionMark);
            holder.textWhen = (TextView) returnedView.findViewById(R.id.listWhenText);
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
        holder.bitcoinScale.setText(Html.fromHtml(CurrencyMapper.mapBtcUnitToPrefix(btcUnit) ));
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
        } catch (final ParseException e) {
            holder.textValue.setText(btcBalance);
        }

        if (!getContext().getSharedPreferences("SPV", Context.MODE_PRIVATE).getBoolean("enabled", true) ||
                current.spvVerified || current.isSpent || current.type == Transaction.TYPE_OUT) {
            holder.textValueQuestionMark.setVisibility(View.GONE);
        } else {
            holder.textValueQuestionMark.setVisibility(View.VISIBLE);
        }

        holder.textWhen.setText(TimeAgo.fromNow(current.date.getTime(), getContext()));

        String message;
        if (current.type == Transaction.TYPE_OUT && current.counterparty != null && current.counterparty.length() > 0) {
            message = current.counterparty;
        }
        else {
            message = getTypeString(current.type);
        }
        holder.textWho.setText(message + (current.memo != null ? " *" : ""));

        holder.mainLayout.setBackgroundColor( val > 0 ?
                        getContext().getResources().getColor(R.color.superLightGreen) :
                        getContext().getResources().getColor(R.color.superLightPink)

        );

        if(current.hasEnoughConfirmations()) {
            holder.inOutIcon.setText( val > 0 ?
                            Html.fromHtml("&#xf090;") :
                            Html.fromHtml("&#xf08b;")
            );
            holder.listNumberConfirmation.setVisibility(View.GONE);
        } else {
            holder.inOutIcon.setText( Html.fromHtml("&#xf017;") );
            holder.listNumberConfirmation.setVisibility(View.VISIBLE);
            holder.listNumberConfirmation.setText( String.valueOf(current.getConfirmations()) );

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

    private String getTypeString(final int type) {
        if (type == Transaction.TYPE_IN)
            return getContext().getString(R.string.txTypeIn);
        else if (type == Transaction.TYPE_OUT)
            return getContext().getString(R.string.txTypeOut);
        else if (type == Transaction.TYPE_REDEPOSIT)
            return getContext().getString(R.string.txTypeRedeposit);
        else
            return "No type";
    }

    private static class Holder {
        protected Transaction current;
        protected TextView textValue;
        protected TextView textWhen;
        protected TextView bitcoinIcon;
        protected TextView textWho;
        protected TextView inOutIcon;
        protected TextView bitcoinScale;
        protected TextView textValueQuestionMark;

        protected RelativeLayout mainLayout;
        public TextView listNumberConfirmation;
    }
}