package com.greenaddress.greenbits.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.GAException;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.Output;
import com.greenaddress.greenapi.PreparedTransaction;
import com.greenaddress.greenbits.ConnectivityObservable;
import com.greenaddress.greenbits.wallets.TrezorHWWallet;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;
import org.bitcoinj.utils.MonetaryFormat;
import org.spongycastle.util.encoders.Hex;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TransactionActivity extends GaActivity {

    @Override
    protected int getMainViewId() { return R.layout.activity_transaction; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState,
                                       final ConnectivityObservable.ConnectionState cs) {
        if (savedInstanceState == null)
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        setResult(RESULT_OK);
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
            sendIntent.putExtra(Intent.EXTRA_TEXT, Network.BLOCKEXPLORER_TX + t.txhash);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends GAFragment {

        @NonNull private static final String TAG = PlaceholderFragment.class.getSimpleName();
        private Dialog mSummary;
        private Dialog mTwoFactor;

        private void openInBrowser(final TextView textView, final String identifier, final String url) {
            textView.setText(identifier);
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    String domain = url;
                    try {
                        domain = new URI(url).getHost();
                    } catch (final URISyntaxException e) {
                        e.printStackTrace();
                    }

                    final String stripped = domain.startsWith("www.") ? domain.substring(4) : domain;

                    Popup(getActivity(), getString(R.string.warning), R.string.continueText, R.string.cancel)
                        .content(getString(R.string.view_block_explorer, stripped))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(final MaterialDialog dlg, final DialogAction which) {
                                final String fullUrl = TextUtils.concat(url, identifier).toString();
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)));
                            }
                        }).build().show();
                }
            });
        }

        @Override
        public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                                 final Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_transaction, container, false);

            final TextView hashText = (TextView) rootView.findViewById(R.id.txHashText);

            final TextView amount = (TextView) rootView.findViewById(R.id.txAmountText);
            final TextView bitcoinScale = (TextView) rootView.findViewById(R.id.txBitcoinScale);
            final TextView bitcoinUnit = (TextView) rootView.findViewById(R.id.txBitcoinUnit);

            final TextView dateText = (TextView) rootView.findViewById(R.id.txDateText);
            final TextView memoText = (TextView) rootView.findViewById(R.id.txMemoText);

            final TextView memoEdit = (TextView) rootView.findViewById(R.id.sendToNoteIcon);
            final EditText memoEditText = (EditText) rootView.findViewById(R.id.sendToNoteText);

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
            final Button saveMemo = (Button) rootView.findViewById(R.id.saveMemo);

            final TextView feeScale = (TextView) rootView.findViewById(R.id.txFeeScale);
            final TextView feeUnit = (TextView) rootView.findViewById(R.id.txFeeUnit);
            final TextView feeInfoText = (TextView) rootView.findViewById(R.id.txFeeInfoText);

            final Transaction t = (Transaction) getActivity().getIntent().getSerializableExtra("TRANSACTION");
            final GaActivity gaActivity = getGaActivity();

            openInBrowser(hashText, t.txhash, Network.BLOCKEXPLORER_TX);

            memoEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                        final boolean editVisible = memoEditText.getVisibility() == View.VISIBLE;
                        memoEditText.setText(memoText.getText().toString());
                        memoEditText.setVisibility(editVisible ? View.GONE : View.VISIBLE);
                        saveMemo.setVisibility(editVisible ? View.GONE : View.VISIBLE);
                        memoText.setVisibility(editVisible ? View.VISIBLE: View.GONE);
                }
            });

            saveMemo.setOnClickListener(new View.OnClickListener() {

                private void onDisableEdit() {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            memoText.setText(memoEditText.getText().toString());
                            saveMemo.setVisibility(View.GONE);
                            memoEditText.setVisibility(View.GONE);
                            if (memoText.getText().toString().isEmpty()) {
                                memoText.setVisibility(View.GONE);
                                rootView.findViewById(R.id.txMemoMargin).setVisibility(View.GONE);
                            } else {
                                rootView.findViewById(R.id.txMemoMargin).setVisibility(View.VISIBLE);
                                memoText.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                }

                @Override
                public void onClick(final View view) {
                    final String edited = memoEditText.getText().toString();
                    if (!edited.equals(memoText.getText().toString())) {
                        CB.after(getGAService().getClient().changeMemo(t.txhash, edited),
                                 new CB.Toast<Boolean>(gaActivity) {
                            @Override
                            public void onSuccess(final Boolean result) {
                                onDisableEdit();
                            }
                        });
                    } else {
                        onDisableEdit();
                    }
                }
            });
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
                    final String checkValues[] = {"1", "3", "6"};
                    for (final String value : checkValues) {
                        final double feerate = Double.parseDouble(((Map)feeEstimates.get(value)).get("feerate").toString());
                        if (feePerKb.compareTo(Coin.valueOf((long)(feerate*1000*1000*100))) >= 0) {
                            currentEstimate = (Integer)((Map)feeEstimates.get(value)).get("blocks");
                            break;
                        }
                    }
                    bestEstimate = (Integer)((Map)feeEstimates.get("1")).get("blocks");
                    unconfirmedEstimatedBlocks.setText(String.format(getResources().getString(R.string.willConfirmAfter), currentEstimate));
                    if (bestEstimate < currentEstimate && t.replaceable) {
                        if (bestEstimate == 1) {
                            unconfirmedRecommendation.setText(R.string.recommendationSingleBlock);
                        } else {
                            unconfirmedRecommendation.setText(String.format(getResources().getString(R.string.recommendationBlocks), bestEstimate));
                        }
                        unconfirmedIncreaseFee.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(final View v) {
                                final double feerate = Double.parseDouble(((Map) feeEstimates.get("1")).get("feerate").toString());
                                final Coin feerateCoin = Coin.valueOf((long) (feerate * 1000 * 1000 * 100));
                                replaceByFee(t, feerateCoin, null, 0);
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

            final String btcUnit = (String) getGAService().getUserConfig("unit");
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
                amount.setText(formatter.format(Double.valueOf(btcBalance)));
            } catch (@NonNull final NumberFormatException e) {
                amount.setText(btcBalance);
            }


            final String btcFee = bitcoinFormat.noCode().format(fee).toString();
            final String btcFeePerKb = bitcoinFormat.noCode().format(feePerKb).toString();
            String feeInfoTextStr = "";
            try {
                feeInfoTextStr += formatter.format(Double.valueOf(btcFee));
            } catch (@NonNull final NumberFormatException e) {
                feeInfoTextStr += btcFee;
            }
            feeInfoTextStr += " / " + String.valueOf(t.size) + " / ";
            try {
                feeInfoTextStr += formatter.format(Double.valueOf(btcFeePerKb));
            } catch (@NonNull final NumberFormatException e) {
                feeInfoTextStr += btcFeePerKb;
            }
            feeInfoText.setText(feeInfoTextStr);

            dateText.setText(SimpleDateFormat.getInstance().format(t.date));
            if (t.memo != null && t.memo.length() > 0) {
                memoText.setText(t.memo);
            } else {
                memoText.setVisibility(View.GONE);
                rootView.findViewById(R.id.txMemoMargin).setVisibility(View.GONE);
            }
            // FIXME: use a list instead of reusing a TextView to show all double spends to allow
            // for a warning to be shown before the browser is open
            // this is to prevent to accidentally leak to block explorers your addresses
            if (t.doubleSpentBy != null || t.replaced_hashes.size() > 0) {
                CharSequence res = "";
                if (t.doubleSpentBy != null) {
                    if (t.doubleSpentBy.equals("malleability") || t.doubleSpentBy.equals("update")) {
                        res = t.doubleSpentBy;
                    } else {
                        res = Html.fromHtml("<a href=\"" + Network.BLOCKEXPLORER_TX + "" + t.doubleSpentBy + "\">" + t.doubleSpentBy + "</a>");
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
                        res = TextUtils.concat(res, Html.fromHtml("<a href=\"" + Network.BLOCKEXPLORER_TX + "" + txhash + "\">" + txhash + "</a>"));
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
                openInBrowser(receivedOnText, t.receivedOn, Network.BLOCKEXPLORER_ADDRESS);
            } else {
                receivedOnText.setVisibility(View.GONE);
                receivedOnTitle.setVisibility(View.GONE);
                rootView.findViewById(R.id.txReceivedOnMargin).setVisibility(View.GONE);
            }

            return rootView;
        }

        private void replaceByFee(final Transaction txData, final Coin feerate, Integer txSize, final int level) {
            if (level > 10) {
                throw new RuntimeException("Recursion limit exceeded");
            }
            final GaActivity gaActivity = getGaActivity();

            final org.bitcoinj.core.Transaction tx = new org.bitcoinj.core.Transaction(Network.NETWORK, Hex.decode(txData.data));
            Integer change_pointer = null;
            final Integer subaccount_pointer = getGAService().cfg("main").getInt("curSubaccount", 0);
            // requiredFeeDelta assumes mintxfee = 1000, and inputs increasing
            // by at most 4 bytes per input (signatures have variable lengths)
            if (txSize == null) {
                txSize = tx.getMessageSize();
            }
            long requiredFeeDelta = txSize + tx.getInputs().size() * 4;
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
            final Coin newFeeWithRate = feerate.multiply(txSize).divide(1000);
            Coin feeDelta = Coin.valueOf(Math.max(
                    newFeeWithRate.subtract(tx.getFee()).longValue(),
                    requiredFeeDelta
            ));
            final Coin oldFee = tx.getFee();
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

            if (remainingFeeDelta.compareTo(Coin.ZERO) <= 0)
                doReplaceByFee(txData, feerate, tx, change_pointer, subaccount_pointer, oldFee, null, null, level);
            else {
                final Coin finalRemaining = remainingFeeDelta;
                CB.after(getGAService().getClient().getAllUnspentOutputs(1, subaccount_pointer),
                         new CB.Toast<ArrayList>(gaActivity) {
                    @Override
                    public void onSuccess(@javax.annotation.Nullable ArrayList result) {
                        Coin remaining = finalRemaining;
                        final List<ListenableFuture<byte[]>> scripts = new ArrayList<>();
                        final List<Map<String, Object>> moreInputs = new ArrayList<>();
                        for (Object utxo_ : result) {
                            Map<String, Object> utxo = (Map<String, Object>) utxo_;
                            remaining = remaining.subtract(Coin.valueOf(Long.valueOf((String)utxo.get("value"))));
                            scripts.add(getGAService().createOutScript((Integer)utxo.get("subaccount"), (Integer)utxo.get("pointer")));
                            moreInputs.add(utxo);
                            if (remaining.compareTo(Coin.ZERO) <= 0) {
                                break;
                            }
                        }
                        if (remaining.compareTo(Coin.ZERO) > 0) {
                            gaActivity.toast(R.string.insufficientFundsText);
                        } else {
                            if (remaining.compareTo(Coin.ZERO) < 0) {
                                final Coin changeValue = remaining.multiply(-1);
                                // we need to add a new change output
                                CB.after(getGAService().getClient().getNewAddress(subaccount_pointer),
                                         new CB.Toast<Map>(gaActivity) {
                                    @Override
                                    public void onSuccess(final @javax.annotation.Nullable Map result) {
                                        tx.addOutput(
                                                changeValue,
                                                Address.fromP2SHHash(
                                                        Network.NETWORK,
                                                        Utils.sha256hash160(Hex.decode((String)result.get("script")))
                                                )
                                        );
                                        CB.after(Futures.allAsList(scripts), new CB.Toast<List<byte[]>>(gaActivity) {
                                            @Override
                                            public void onSuccess(@javax.annotation.Nullable List<byte[]> morePrevouts) {
                                                doReplaceByFee(txData, feerate, tx, (Integer) result.get("pointer"),
                                                        subaccount_pointer, oldFee, moreInputs, morePrevouts, level);
                                            }
                                        });
                                    }
                                });
                            } else {
                                // we were lucky enough to match the required value
                                CB.after(Futures.allAsList(scripts), new CB.Toast<List<byte[]>>(gaActivity) {
                                    @Override
                                    public void onSuccess(@javax.annotation.Nullable List<byte[]> morePrevouts) {
                                        doReplaceByFee(txData, feerate, tx, null, subaccount_pointer,
                                                       oldFee, moreInputs, morePrevouts, level);
                                    }
                                });
                            }
                        }
                    }
                });
            }
        }

        private void doReplaceByFee(final Transaction txData, final Coin feerate,
                                    final org.bitcoinj.core.Transaction tx,
                                    final Integer change_pointer, final Integer subaccount_pointer,
                                    final Coin oldFee, final List<Map<String, Object>> moreInputs,
                                    final List<byte[]> morePrevouts, final int level) {
            final GaActivity gaActivity = getGaActivity();
            String twoOfThreeBackupChaincode = null, twoOfThreeBackupPubkey = null;

            for (final Object subaccount_ : getGAService().getSubaccounts()) {
                final Map<String, ?> subaccountMap = (Map) subaccount_;
                if (subaccountMap.get("type").equals("2of3") && subaccountMap.get("pointer").equals(subaccount_pointer)) {
                    twoOfThreeBackupChaincode = (String) subaccountMap.get("2of3_backup_chaincode");
                    twoOfThreeBackupPubkey = (String) subaccountMap.get("2of3_backup_pubkey");
                }
            }

            final Map<String, org.bitcoinj.core.Transaction> prevoutRawTxs = new HashMap<>();

            final PreparedTransaction prepTx = new PreparedTransaction(
                    change_pointer, subaccount_pointer, /*requires_2factor*/false,
                    tx, twoOfThreeBackupChaincode, twoOfThreeBackupPubkey,
                    prevoutRawTxs
            );

            for (final Map<String, Object> ep : (List<Map<String, Object>>)txData.eps) {
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

            int i = 0;
            if (moreInputs != null) {
                for (final Map<String, Object> ep : moreInputs) {
                    prepTx.prev_outputs.add(new Output(
                            (Integer) ep.get("subaccount"),
                            (Integer) ep.get("pointer"),
                            1,
                            10,  // == P2SH_FORTIFIED_OUT
                            new String(Hex.encode(morePrevouts.get(i))),
                            Long.valueOf((String) ep.get("value"))
                    ));
                    tx.addInput(
                            new TransactionInput(
                                    Network.NETWORK,
                                    null,
                                    new ScriptBuilder().addChunk(
                                            // OP_0
                                            new ScriptChunk(0, new byte[0])
                                    ).data(
                                            // GA sig:
                                            new byte[71]
                                    ).data(
                                            // our sig:
                                            new byte[71]
                                    ).data(
                                            // the original outscript
                                            morePrevouts.get(i)
                                    ).build().getProgram(),
                                    new TransactionOutPoint(
                                            Network.NETWORK,
                                            (Integer) ep.get("pt_idx"),
                                            Sha256Hash.wrap(Hex.decode((String) ep.get("txhash")))
                                    ),
                                    Coin.valueOf(Long.valueOf((String) ep.get("value")))
                            )
                    );
                    i++;
                }
            }

            // verify if the increased fee is enough to achieve wanted feerate
            // (can be too small in case of added inputs)
            final int estimatedSize = tx.getMessageSize() + tx.getInputs().size() * 4;
            if (feerate.multiply(estimatedSize).divide(1000).compareTo(tx.getFee()) > 0) {
                replaceByFee(txData, feerate, estimatedSize, level + 1);
                return;
            }

            // also verify if it's enough for 'bandwidth fee increment' condition
            // of RBF
            if (tx.getFee().subtract(oldFee).compareTo(Coin.valueOf(tx.getMessageSize() + tx.getInputs().size() * 4)) < 0) {
                replaceByFee(txData, feerate, estimatedSize, level + 1);
                return;
            }

            ListenableFuture<Void> prevouts = Futures.immediateFuture(null);
            if (getGAService().getClient().getHdWallet() instanceof TrezorHWWallet) {
                for (final TransactionInput inp : tx.getInputs()) {
                    prevouts = Futures.transform(prevouts, new AsyncFunction<Void, Void>() {
                        @Override
                        public ListenableFuture<Void> apply(Void input) throws Exception {
                            return Futures.transform(
                                    getGAService().getClient().getRawOutput(inp.getOutpoint().getHash()),
                                    new Function<org.bitcoinj.core.Transaction, Void>() {
                                        @Override
                                        public Void apply(org.bitcoinj.core.Transaction input) {
                                            prevoutRawTxs.put(new String(Hex.encode(inp.getOutpoint().getHash().getBytes())), input);
                                            return null;
                                        }
                                    }
                            );
                        }
                    });
                }
            }

            final ListenableFuture<List<String>> signed = Futures.transform(prevouts, new AsyncFunction<Void, List<String>>() {
                @Override
                public ListenableFuture<List<String>> apply(Void input) throws Exception {
                    return getGAService().getClient().signTransaction(prepTx, false);
                }
            });

            CB.after(signed, new CB.Toast<List<String>>(gaActivity) {
                @Override
                public void onSuccess(final @javax.annotation.Nullable List<String> signatures) {

                    int i = 0;
                    for (final String sig : signatures) {
                        final TransactionInput input = tx.getInput(i++);
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
                    final Map<String, Object> twoFacData = new HashMap<>();
                    twoFacData.put("try_under_limits_bump", tx.getFee().subtract(oldFee).longValue());
                    final ListenableFuture<Map<String,Object>> sendFuture = getGAService().getClient().sendRawTransaction(tx, twoFacData, true);
                    Futures.addCallback(sendFuture, new FutureCallback<Map<String,Object>>() {
                        @Override
                        public void onSuccess(@Nullable final Map result) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // FIXME: Add notification with "Transaction sent"?
                                    getActivity().finish();
                                }
                            });
                        }

                        @Override
                        public void onFailure(@NonNull final Throwable t) {
                            if (t instanceof GAException && t.getMessage().equals("http://greenaddressit.com/error#auth")) {
                                // 2FA required
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final List<String> enabledTwoFac =
                                                getGAService().getEnabledTwoFacNames(true);
                                        if (enabledTwoFac.size() > 1) {
                                            show2FAChoices(oldFee, tx.getFee(), tx);
                                        } else {
                                            showIncreaseSummary(enabledTwoFac.get(0), oldFee, tx.getFee(), tx);
                                        }
                                    }
                                });
                            } else {
                                gaActivity.toast(t);
                            }

                        }
                    }, getGAService().es);
                }
            });
        }

        private void show2FAChoices(final Coin oldFee, final Coin newFee, @NonNull final org.bitcoinj.core.Transaction signedTx) {
            Log.i(TAG, "params " + oldFee + " " + newFee);
            final String[] enabledTwoFacNames = new String[]{};
            final List<String> enabledTwoFacNamesSystem = getGAService().getEnabledTwoFacNames(true);
            mTwoFactor = Popup(getActivity(), getString(R.string.twoFactorChoicesTitle), R.string.choose, R.string.cancel)
                             .items(getGAService().getEnabledTwoFacNames(false).toArray(enabledTwoFacNames))
                             .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                                 @Override
                                 public boolean onSelection(MaterialDialog dlg, View view, int which, CharSequence text) {
                                     showIncreaseSummary(enabledTwoFacNamesSystem.get(which), oldFee, newFee, signedTx);
                                     return true;
                                 }
                             }).build();
            mTwoFactor.show();
        }

        private void showIncreaseSummary(@Nullable final String method, final Coin oldFee, final Coin newFee, @NonNull final org.bitcoinj.core.Transaction signedTx) {
            Log.i(TAG, "showIncreaseSummary( params " + method + " " + oldFee + " " + newFee + ")");
            final GaActivity gaActivity = getGaActivity();

            final String btcUnit = (String) getGAService().getUserConfig("unit");
            final MonetaryFormat bitcoinFormat = CurrencyMapper.mapBtcUnitToFormat(btcUnit);

            final View inflatedLayout = getActivity().getLayoutInflater().inflate(R.layout.dialog_new_transaction, null, false);

            ((TextView) inflatedLayout.findViewById(R.id.newTxAmountLabel)).setText(R.string.newFeeText);
            final TextView amountText = (TextView) inflatedLayout.findViewById(R.id.newTxAmountText);
            final TextView amountScale = (TextView) inflatedLayout.findViewById(R.id.newTxAmountScaleText);
            final TextView amountUnit = (TextView) inflatedLayout.findViewById(R.id.newTxAmountUnitText);
            ((TextView) inflatedLayout.findViewById(R.id.newTxFeeLabel)).setText(R.string.oldFeeText);
            final TextView feeText = (TextView) inflatedLayout.findViewById(R.id.newTxFeeText);
            final TextView feeScale = (TextView) inflatedLayout.findViewById(R.id.newTxFeeScale);
            final TextView feeUnit = (TextView) inflatedLayout.findViewById(R.id.newTxFeeUnit);

            inflatedLayout.findViewById(R.id.newTxRecipientLabel).setVisibility(View.GONE);
            inflatedLayout.findViewById(R.id.newTxRecipientText).setVisibility(View.GONE);
            final TextView twoFAText = (TextView) inflatedLayout.findViewById(R.id.newTx2FATypeText);
            final EditText newTx2FACodeText = (EditText) inflatedLayout.findViewById(R.id.newTx2FACodeText);
            final String prefix = CurrencyMapper.mapBtcFormatToPrefix(bitcoinFormat);

            amountScale.setText(Html.fromHtml(prefix));
            feeScale.setText(Html.fromHtml(prefix));
            if (prefix.isEmpty()) {
                amountUnit.setText("bits ");
                feeUnit.setText("bits ");
            } else {
                amountUnit.setText(Html.fromHtml("&#xf15a; "));
                feeUnit.setText(Html.fromHtml("&#xf15a; "));
            }
            amountText.setText(bitcoinFormat.noCode().format(newFee));
            feeText.setText(bitcoinFormat.noCode().format(oldFee));


            final Map<String, Object> twoFacData;

            if (method == null) {
                twoFAText.setVisibility(View.GONE);
                newTx2FACodeText.setVisibility(View.GONE);
                twoFacData = null;
            } else {
                twoFAText.setText(String.format("2FA %s code", method));
                twoFacData = new HashMap<>();
                twoFacData.put("method", method);
                twoFacData.put("bump_fee_amount", newFee.subtract(oldFee).longValue());
                if (!method.equals("gauth")) {
                    Map<String, Long> amount = new HashMap<>();
                    amount.put("amount", newFee.subtract(oldFee).longValue());
                    getGAService().requestTwoFacCode(
                            method, "bump_fee", amount
                    );
                }
            }

            mSummary = Popup(getActivity(), getString(R.string.feeIncreaseTitle), R.string.send, R.string.cancel)
                    .customView(inflatedLayout, true)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(final @NonNull MaterialDialog dialog, final @NonNull DialogAction which) {
                            if (twoFacData != null) {
                                twoFacData.put("code", newTx2FACodeText.getText().toString());
                            }
                            final ListenableFuture<Map<String,Object>> sendFuture = getGAService().getClient().sendRawTransaction(signedTx, twoFacData, false);
                            Futures.addCallback(sendFuture, new CB.Toast<Map<String,Object>>(gaActivity) {
                                @Override
                                public void onSuccess(@Nullable final Map result) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // FIXME: Add notification with "Transaction sent"?
                                            getActivity().finish();
                                        }
                                    });
                                }
                            }, getGAService().es);
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(final @NonNull MaterialDialog dialog, final @NonNull DialogAction which) {
                            Log.i(TAG, "SHOWN ON CLOSE!");
                        }
                    })
                    .build();

            mSummary.show();
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            if (mSummary != null)
                mSummary.dismiss();
            if (mTwoFactor != null)
                mTwoFactor.dismiss();
        }
    }
}
