package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.bitcoinj.utils.MonetaryFormat;

import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

public class ListBitBoatTxsAdapter extends ArrayAdapter<BitBoatTransaction> {

    final String btcUnit;

    public ListBitBoatTxsAdapter(final Context context, final int resource, final List<BitBoatTransaction> objects, final String btcUnit) {
        super(context, resource, objects);
        this.btcUnit = btcUnit;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final BitBoatTransaction current = getItem(position);
        Holder holder;
        View returnedView;

        if (convertView == null) {
            final LayoutInflater inflater = LayoutInflater.from(getContext());
            returnedView = inflater.inflate(R.layout.list_element_bitboat, parent, false);
            holder = new Holder();
            holder.current = current;
            holder.textFirstBits = (TextView) returnedView.findViewById(R.id.firstBits);
            holder.textValueBtc = (TextView) returnedView.findViewById(R.id.listValueBtc);
            holder.textValueFiat = (TextView) returnedView.findViewById(R.id.listValueFiat);
            holder.textTimeLeft = (TextView) returnedView.findViewById(R.id.listTimeLeft);
            holder.textNumber = (TextView) returnedView.findViewById(R.id.listNumberText);
            holder.textWho = (TextView) returnedView.findViewById(R.id.listWhoText);
            holder.textCF = (TextView) returnedView.findViewById(R.id.listCFText);
            holder.bitcoinScale = (TextView) returnedView.findViewById(R.id.listBitcoinScaleText);
            holder.bitcoinIcon = (TextView) returnedView.findViewById(R.id.listBitcoinIcon);
            returnedView.setTag(holder);
        } else {
            returnedView = convertView;
            holder = (Holder) returnedView.getTag();
        }

        holder.textFirstBits.setText(current.firstbits);
        final MonetaryFormat bitcoinFormat = CurrencyMapper.mapBtcUnitToFormat(btcUnit);
        holder.bitcoinScale.setText(Html.fromHtml(CurrencyMapper.mapBtcUnitToPrefix(btcUnit)));
        if (btcUnit == null || btcUnit.equals("bits")) {
            holder.bitcoinIcon.setText("");
            holder.bitcoinScale.setText("bits ");
        } else {
            holder.bitcoinIcon.setText(Html.fromHtml("&#xf15a; "));
        }
        holder.textValueBtc.setText(bitcoinFormat.noCode().format(current.valueBtc));
        if (current.valueFiat == null) {
            holder.textValueFiat.setText("");
        } else {
            holder.textValueFiat.setText(current.valueFiat.toPlainString());
        }
        int left = Math.max(0, (int) ((3600000 - ((new Date()).getTime() - current.date.getTime())) / 60000));
        holder.textTimeLeft.setText(new Formatter().format(getContext().getResources().getString(R.string.minutesLeft), left).toString());
        holder.textNumber.setText(
                (current.method == BitBoatTransaction.PAYMETHOD_POSTEPAY ? "Postepay" :
                 current.method == BitBoatTransaction.PAYMETHOD_SUPERFLASH ? "Superflash" :
                 current.method == BitBoatTransaction.PAYMETHOD_MANDATCOMPTE ? "Mandat Compte" :
                 "") + " " +
                current.number);
        holder.textWho.setText(current.key);
        holder.textCF.setText(current.cf);

        return returnedView;
    }

    private static class Holder {
        protected BitBoatTransaction current;
        protected TextView textFirstBits;
        protected TextView textValueBtc;
        protected TextView textValueFiat;
        protected TextView textTimeLeft;
        protected TextView textNumber;
        protected TextView textWho;
        protected TextView textCF;
        protected TextView bitcoinScale;
        protected TextView bitcoinIcon;
    }
}
