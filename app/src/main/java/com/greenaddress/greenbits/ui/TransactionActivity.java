package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.Output;
import com.greenaddress.greenapi.PreparedTransaction;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;
import org.bitcoinj.script.ScriptOpCodes;
import org.bitcoinj.utils.MonetaryFormat;
import org.spongycastle.util.encoders.Hex;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

            final TextView doubleSpentByText = (TextView) rootView.findViewById(R.id.txDoubleSpentByText);
            final TextView doubleSpentByTitle = (TextView) rootView.findViewById(R.id.txDoubleSpentByTitle);

            final TextView recipientText = (TextView) rootView.findViewById(R.id.txRecipientText);
            final TextView recipientTitle = (TextView) rootView.findViewById(R.id.txRecipientTitle);

            final TextView receivedOnText = (TextView) rootView.findViewById(R.id.txReceivedOnText);
            final TextView receivedOnTitle = (TextView) rootView.findViewById(R.id.txReceivedOnTitle);

            final TextView unconfirmedText = (TextView) rootView.findViewById(R.id.txUnconfirmedText);
            final TextView unconfirmedEstimatedBlocks = (TextView) rootView.findViewById(R.id.txUnconfirmedEstimatedBlocks);
            final TextView unconfirmedRecommendation = (TextView) rootView.findViewById(R.id.txUnconfirmedRecommendation);
            final Button unconfirmedIncreaseFee = (Button) rootView.findViewById(R.id.txUnconfirmedIncreaseFee);

            final TextView feeScale = (TextView) rootView.findViewById(R.id.txFeeScale);
            final TextView feeUnit = (TextView) rootView.findViewById(R.id.txFeeUnit);
            final TextView feeInfoText = (TextView) rootView.findViewById(R.id.txFeeInfoText);

            hashText.setMovementMethod(LinkMovementMethod.getInstance());

            final Transaction t = (Transaction) getActivity().getIntent().getSerializableExtra("TRANSACTION");
            hashText.setText(Html.fromHtml("<a href=\"" + Network.BLOCKEXPLORER + "" + t.txhash + "\">" + t.txhash + "</a>"));

            final Coin fee = Coin.valueOf(t.fee);
            final Coin feePerKb;
            if (t.size > 0) {
                feePerKb = Coin.valueOf(1000 * t.fee / t.size);
            } else {
                // shouldn't happen, but just in case let's avoid division by zero
                feePerKb = Coin.valueOf(0);
            }

            if (t.type.equals(Transaction.TYPE.OUT) || t.type.equals(Transaction.TYPE.REDEPOSIT) || t.isSpent) {
                if (t.getConfirmations() > 0) {
                    // confirmed - hide unconfirmed widgets
                    rootView.findViewById(R.id.txUnconfirmed).setVisibility(View.GONE);
                    unconfirmedRecommendation.setVisibility(View.GONE);
                    unconfirmedIncreaseFee.setVisibility(View.GONE);
                    unconfirmedEstimatedBlocks.setVisibility(View.GONE);
                } else if (t.type.equals(Transaction.TYPE.OUT) || t.type.equals(Transaction.TYPE.REDEPOSIT)) {
                    // unconfirmed outgoing output/redeposit - can be RBF'd
                    int currentEstimate = 25, bestEstimate;
                    final Map<String, Object> feeEstimates = getGAService().getClient().getLoginData().feeEstimates;
                    String checkValues[] = {"1", "3", "6"};
                    for (String value : checkValues) {
                        Number feerate = (Number)((Map)feeEstimates.get(value)).get("feerate");
                        if (feePerKb.compareTo(Coin.valueOf((long)(feerate.doubleValue()*1000*1000*100))) >= 0) {
                            currentEstimate = (Integer)((Map)feeEstimates.get(value)).get("blocks");
                            break;
                        }
                    }
                    bestEstimate = (Integer)((Map)feeEstimates.get("1")).get("blocks");
                    unconfirmedEstimatedBlocks.setText(String.format(getResources().getString(R.string.willConfirmAfter), currentEstimate));
                    if (bestEstimate < currentEstimate && t.replaceable) {
                        if (bestEstimate == 1) {
                            unconfirmedRecommendation.setText(getResources().getString(R.string.recommendationSingleBlock));
                        } else {
                            unconfirmedRecommendation.setText(String.format(getResources().getString(R.string.recommendationBlocks), bestEstimate));
                        }
                        unconfirmedIncreaseFee.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                double feerate = (Double)((Map)feeEstimates.get("1")).get("feerate");
                                Coin feerateCoin = Coin.valueOf((long)(feerate*1000*1000*100));
                                replaceByFee(t, feerateCoin);
                            }
                        });
                    } else {
                        unconfirmedIncreaseFee.setVisibility(View.GONE);
                        unconfirmedRecommendation.setVisibility(View.GONE);
                    }
                } else {
                    // incoming spent - hide outgoing-only widgets
                    unconfirmedRecommendation.setVisibility(View.GONE);
                    unconfirmedIncreaseFee.setVisibility(View.GONE);
                    unconfirmedEstimatedBlocks.setVisibility(View.GONE);
                }
            } else {
                // unspent incoming output
                // incoming - hide outgoing-only widgets
                unconfirmedRecommendation.setVisibility(View.GONE);
                unconfirmedIncreaseFee.setVisibility(View.GONE);
                unconfirmedEstimatedBlocks.setVisibility(View.GONE);
                if (t.getConfirmations() > 0) {
                    if (t.spvVerified) {
                        rootView.findViewById(R.id.txUnconfirmed).setVisibility(View.GONE);
                    } else {
                        if (getGAService().spv.getSpvBlocksLeft() != Integer.MAX_VALUE) {
                            unconfirmedText.setText(String.format("%s %s", getResources().getString(R.string.txUnverifiedTx),
                                    getGAService().spv.getSpvBlocksLeft()));
                        } else {
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

            if (t.doubleSpentBy != null || t.replaced_hashes.size() > 0) {
                CharSequence res = "";
                if (t.doubleSpentBy != null) {
                    if (t.doubleSpentBy.equals("malleability") || t.doubleSpentBy.equals("update")) {
                        res = t.doubleSpentBy;
                    } else {
                        res = Html.fromHtml("<a href=\"" + Network.BLOCKEXPLORER + "" + t.doubleSpentBy + "\">" + t.doubleSpentBy + "</a>");
                    }
                    if (t.replaced_hashes.size() > 0) {
                        res = TextUtils.concat(res, "; ");
                    }
                }
                if (t.replaced_hashes.size() > 0) {
                    res = TextUtils.concat(res, Html.fromHtml("replaces transactions:<br/>"));
                    for (int i = 0; i < t.replaced_hashes.size(); ++i) {
                        if (i > 0) {
                            res = TextUtils.concat(res, Html.fromHtml("<br/>"));
                        }
                        String txhash = t.replaced_hashes.get(i);
                        res = TextUtils.concat(res, Html.fromHtml("<a href=\"" + Network.BLOCKEXPLORER + "" + txhash + "\">" + txhash + "</a>"));
                    }
                }
                doubleSpentByText.setText(res);
            } else {
                doubleSpentByText.setVisibility(View.GONE);
                doubleSpentByTitle.setVisibility(View.GONE);
                rootView.findViewById(R.id.txDoubleSpentByMargin).setVisibility(View.GONE);
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

        private void replaceByFee(Transaction txData, Coin feerate) {
            final org.bitcoinj.core.Transaction tx = new org.bitcoinj.core.Transaction(Network.NETWORK, Hex.decode(txData.data));
            Integer change_pointer = null, subaccount_pointer = getGAApp().getSharedPreferences("send", Context.MODE_PRIVATE).getInt("curSubaccount", 0);
            long requiredFeeDelta = tx.getMessageSize(); // assumes mintxfee = 1000
            List<TransactionInput> oldInputs = new ArrayList<>(tx.getInputs());
            tx.clearInputs();
            for (int i = 0; i < txData.eps.size(); ++i) {
                final Map<String, Object> ep = (Map<String, Object>) txData.eps.get(i);
                if (((Boolean) ep.get("is_credit"))) continue;
                TransactionInput oldInput = oldInputs.get((Integer) ep.get("pt_idx"));
                TransactionInput newInput = new TransactionInput(
                        Network.NETWORK,
                        null,
                        oldInput.getScriptBytes(),
                        oldInput.getOutpoint(),
                        Coin.valueOf(Long.valueOf((String) ep.get("value")))
                );
                newInput.setSequenceNumber(0);
                tx.addInput(newInput);
            }
            Coin feeDelta = Coin.valueOf(Math.max(
                    feerate.subtract(tx.getFee()).longValue(),
                    requiredFeeDelta
            ));
            Coin remainingFeeDelta = feeDelta;
            List<TransactionOutput> origOuts = new ArrayList<>(tx.getOutputs());
            tx.clearOutputs();
            for (int i = 0; i < txData.eps.size(); ++i) {
                final Map<String, Object> ep = (Map<String, Object>) txData.eps.get(i);
                if (!((Boolean) ep.get("is_credit"))) continue;

                if (!((Boolean) ep.get("is_relevant"))) {
                    // keep non-change/non-redeposit intact
                    tx.addOutput(origOuts.get((Integer)ep.get("pt_idx")));
                } else {
                    if ((ep.get("subaccount") == null && subaccount_pointer.equals(0)) ||
                            ep.get("subaccount").equals(subaccount_pointer)) {
                        change_pointer = (Integer) ep.get("pubkey_pointer");
                    }
                    // change/redeposit
                    long value = Long.valueOf((String) ep.get("value"));
                    if (Coin.valueOf(value).compareTo(remainingFeeDelta) <= 0) {
                        // smaller than remaining fee -- get rid of this output
                        remainingFeeDelta = remainingFeeDelta.subtract(
                                Coin.valueOf(value)
                        );
                    }  else {
                        // larger than remaining fee -- subtract the remaining fee
                        TransactionOutput out = origOuts.get((Integer)ep.get("pt_idx"));
                        out.setValue(out.getValue().subtract(remainingFeeDelta));
                        tx.addOutput(out);
                        remainingFeeDelta = Coin.ZERO;
                    }
                }
            }

            if (remainingFeeDelta.compareTo(Coin.ZERO) > 0) {
                throw new RuntimeException("adding outputs not implemented");
            }

            Boolean requires_2factor = false;
            String twoOfThreeBackupChaincode = null, twoOfThreeBackupPubkey = null;

            PreparedTransaction prepTx = new PreparedTransaction(
                    change_pointer, subaccount_pointer, requires_2factor,
                    tx, twoOfThreeBackupChaincode, twoOfThreeBackupPubkey
            );


            for (int i = 0; i < txData.eps.size(); ++i) {
                final Map<String, Object> ep = (Map<String, Object>) txData.eps.get(i);
                if (((Boolean) ep.get("is_credit"))) continue;

                prepTx.prev_outputs.add(new Output(
                        (Integer) ep.get("subaccount"),
                        (Integer) ep.get("pubkey_pointer"),
                        1,
                        (Integer) ep.get("script_type"),
                        new String(Hex.encode(tx.getInput((Integer) ep.get("pt_idx")).getScriptSig().getChunks().get(3).data)),
                        tx.getInput((Integer) ep.get("pt_idx")).getValue().longValue()
                ));
            }

            Futures.addCallback(getGAService().getClient().signTransaction(prepTx, false), new FutureCallback<List<String>>() {
                @Override
                public void onSuccess(@javax.annotation.Nullable List<String> signatures) {

                    int i = 0;
                    for (String sig : signatures) {
                        TransactionInput input = tx.getInput(i++);
                        input.setScriptSig(
                                new ScriptBuilder().addChunk(
                                        // OP_0
                                        input.getScriptSig().getChunks().get(0)
                                ).data(
                                        // GA sig:
                                        new byte[] {0}
                                ).data(
                                        // our sig:
                                        Hex.decode(sig)
                                ).addChunk(
                                        // the original outscript
                                        input.getScriptSig().getChunks().get(3)
                                ).build()
                        );
                    }

                    getGAService().getClient().sendRawTransaction(tx);
                }

                @Override
                public void onFailure(Throwable t) {
                    t.printStackTrace();
                }
            });

        }
    }
}
