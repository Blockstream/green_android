package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.greenaddress.greenapi.Network;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.MonetaryFormat;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Observable;
import java.util.Observer;


public class TransactionActivity extends ActionBarActivity implements Observer {

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getGAService() == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_transaction);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_transaction, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {

        final int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_share) {
            final Transaction t = (Transaction) getIntent().getSerializableExtra("TRANSACTION");
            final Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, Network.BLOCKEXPLORER + t.txhash);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void update(final Observable observable, final Object data) {

    }

    @Override
    public void onResume() {
        super.onResume();
        if (getGAService() == null) {
            finish();
            return;
        }
        getGAApp().getConnectionObservable().addObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getGAApp().getConnectionObservable().deleteObserver(this);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends GAFragment {

        @Override
        public View onGACreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                                   final Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_transaction, container, false);

            final TextView hashText = (TextView) rootView.findViewById(R.id.txHashText);

            final TextView amount = (TextView) rootView.findViewById(R.id.txAmountText);
            final TextView bitcoinScale = (TextView) rootView.findViewById(R.id.txBitcoinScale);
            final TextView bitcoinUnit = (TextView) rootView.findViewById(R.id.txBitcoinUnit);

            final TextView dateText = (TextView) rootView.findViewById(R.id.txDateText);
            final TextView memoText = (TextView) rootView.findViewById(R.id.txMemoText);
            final TextView memoTitle = (TextView) rootView.findViewById(R.id.txMemoTitle);

            final TextView recipientText = (TextView) rootView.findViewById(R.id.txRecipientText);
            final TextView recipientTitle = (TextView) rootView.findViewById(R.id.txRecipientTitle);

            final TextView receivedOnText = (TextView) rootView.findViewById(R.id.txReceivedOnText);
            final TextView receivedOnTitle = (TextView) rootView.findViewById(R.id.txReceivedOnTitle);

            final TextView unconfirmedText = (TextView) rootView.findViewById(R.id.txUnconfirmedText);

            final TextView feeScale = (TextView) rootView.findViewById(R.id.txFeeScale);
            final TextView feeUnit = (TextView) rootView.findViewById(R.id.txFeeUnit);
            final TextView feeInfoText = (TextView) rootView.findViewById(R.id.txFeeInfoText);

            hashText.setMovementMethod(LinkMovementMethod.getInstance());

            final Transaction t = (Transaction) getActivity().getIntent().getSerializableExtra("TRANSACTION");
            hashText.setText(Html.fromHtml("<a href=\"" + Network.BLOCKEXPLORER + "" + t.txhash + "\">" + t.txhash + "</a>"));

            if (t.type.equals(Transaction.TYPE.OUT) || t.isSpent) {
                rootView.findViewById(R.id.txUnconfirmed).setVisibility(View.GONE);
            } else {
                // unspent output
                if (t.getConfirmations() > 0) {
                    if (t.spvVerified) {
                        rootView.findViewById(R.id.txUnconfirmed).setVisibility(View.GONE);
                    } else {
                        if(getGAService().spv.getSpvBlocksLeft() != Integer.MAX_VALUE) {
                            unconfirmedText.setText(String.format("%s %s", getResources().getString(R.string.txUnverifiedTx),
                                    getGAService().spv.getSpvBlocksLeft()));
                        }
                        else{
                            unconfirmedText.setText(String.format("%s %s", getResources().getString(R.string.txUnverifiedTx),
                                    "Not yet connected to SPV!"));
                        }
                    }
                }
            }

            final String btcUnit = (String) getGAService().getAppearanceValue("unit");
            final Coin coin = Coin.valueOf(t.amount);
            final MonetaryFormat bitcoinFormat = CurrencyMapper.mapBtcUnitToFormat(btcUnit);
            bitcoinScale.setText(Html.fromHtml(CurrencyMapper.mapBtcUnitToPrefix(btcUnit)));
            feeScale.setText(Html.fromHtml(CurrencyMapper.mapBtcUnitToPrefix(btcUnit)));
            if (btcUnit == null || btcUnit.equals("bits")) {
                bitcoinUnit.setText("bits ");
                feeUnit.setText("bits ");
            } else {
                bitcoinUnit.setText(Html.fromHtml("&#xf15a; "));
                feeUnit.setText(Html.fromHtml("&#xf15a; "));
            }
            final String btcBalance = bitcoinFormat.noCode().format(coin).toString();
            final DecimalFormat formatter = new DecimalFormat("#,###.########");

            try {
                amount.setText(formatter.format(formatter.parse(btcBalance)));
            } catch (@NonNull final ParseException e) {
                amount.setText(btcBalance);
            }

            final Coin fee = Coin.valueOf(t.fee);
            final Coin feePerKb;
            if (t.size > 0) {
                feePerKb = Coin.valueOf(1000 * t.fee / t.size);
            } else {
                // shouldn't happen, but just in case let's avoid division by zero
                feePerKb = Coin.valueOf(0);
            }
            final String btcFee = bitcoinFormat.noCode().format(fee).toString();
            final String btcFeePerKb = bitcoinFormat.noCode().format(feePerKb).toString();
            String feeInfoTextStr = "";
            try {
                feeInfoTextStr += formatter.format(formatter.parse(btcFee));
            } catch (@NonNull final ParseException e) {
                feeInfoTextStr += btcFee;
            }
            feeInfoTextStr += " / " + String.valueOf(t.size) + " / ";
            try {
                feeInfoTextStr += formatter.format(formatter.parse(btcFeePerKb));
            } catch (@NonNull final ParseException e) {
                feeInfoTextStr += btcFeePerKb;
            }
            feeInfoText.setText(feeInfoTextStr);

            dateText.setText(SimpleDateFormat.getInstance().format(t.date));
            if (t.memo != null && t.memo.length() > 0) {
                memoText.setText(t.memo);
            } else {
                memoText.setVisibility(View.GONE);
                memoTitle.setVisibility(View.GONE);
                rootView.findViewById(R.id.txMemoMargin).setVisibility(View.GONE);
            }

            if (t.counterparty != null && t.counterparty.length() > 0) {
                recipientText.setText(t.counterparty);
            } else {
                recipientText.setVisibility(View.GONE);
                recipientTitle.setVisibility(View.GONE);
                rootView.findViewById(R.id.txRecipientMargin).setVisibility(View.GONE);
            }

            if (t.receivedOn != null && t.receivedOn.length() > 0) {
                receivedOnText.setText(t.receivedOn);
            } else {
                receivedOnText.setVisibility(View.GONE);
                receivedOnTitle.setVisibility(View.GONE);
                rootView.findViewById(R.id.txReceivedOnMargin).setVisibility(View.GONE);
            }

            return rootView;
        }
    }
}
