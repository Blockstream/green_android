package com.greenaddress.greenbits.ui;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.blockstream.libwally.Wally;
import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.GAException;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.Output;
import com.greenaddress.greenapi.PreparedTransaction;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.wallets.TrezorHWWallet;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;
import org.bitcoinj.utils.MonetaryFormat;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TransactionActivity extends GaActivity {

    @Override
    protected int getMainViewId() { return R.layout.activity_transaction; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        if (savedInstanceState == null)
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        setResult(RESULT_OK);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_transaction, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_share:
                final TransactionItem txItem = (TransactionItem) getIntent().getSerializableExtra("TRANSACTION");
                final Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, Network.BLOCKEXPLORER_TX + txItem.txHash.toString());
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class PlaceholderFragment extends GAFragment {

        private static final String TAG = PlaceholderFragment.class.getSimpleName();
        private Dialog mSummary;
        private Dialog mTwoFactor;

        private void openInBrowser(final TextView textView, final String identifier, final String url) {
            textView.setText(identifier);
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String domain = url;
                    try {
                        domain = new URI(url).getHost();
                    } catch (final URISyntaxException e) {
                        e.printStackTrace();
                    }

                    final String stripped = domain.startsWith("www.") ? domain.substring(4) : domain;

                    UI.popup(getActivity(), R.string.warning, R.string.continueText, R.string.cancel)
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
        public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                                 final Bundle savedInstanceState) {

            final GaService service = getGAService();

            final View v = inflater.inflate(R.layout.fragment_transaction, container, false);

            final TextView hashText = UI.find(v, R.id.txHashText);

            final TextView amount = UI.find(v, R.id.txAmountText);
            final TextView bitcoinScale = UI.find(v, R.id.txBitcoinScale);
            final TextView bitcoinUnit = UI.find(v, R.id.txBitcoinUnit);

            final TextView dateText = UI.find(v, R.id.txDateText);
            final TextView memoText = UI.find(v, R.id.txMemoText);

            final TextView memoEdit = UI.find(v, R.id.sendToNoteIcon);
            final EditText memoEditText = UI.find(v, R.id.sendToNoteText);

            final TextView doubleSpentByText = UI.find(v, R.id.txDoubleSpentByText);
            final TextView doubleSpentByTitle = UI.find(v, R.id.txDoubleSpentByTitle);

            final TextView recipientText = UI.find(v, R.id.txRecipientText);
            final TextView recipientTitle = UI.find(v, R.id.txRecipientTitle);

            final TextView receivedOnText = UI.find(v, R.id.txReceivedOnText);
            final TextView receivedOnTitle = UI.find(v, R.id.txReceivedOnTitle);

            final TextView unconfirmedText = UI.find(v, R.id.txUnconfirmedText);
            final TextView unconfirmedEstimatedBlocks = UI.find(v, R.id.txUnconfirmedEstimatedBlocks);
            final TextView unconfirmedRecommendation = UI.find(v, R.id.txUnconfirmedRecommendation);
            final Button unconfirmedIncreaseFee = UI.find(v, R.id.txUnconfirmedIncreaseFee);
            final Button saveMemo = UI.find(v, R.id.saveMemo);

            final TextView feeScale = UI.find(v, R.id.txFeeScale);
            final TextView feeUnit = UI.find(v, R.id.txFeeUnit);
            final TextView feeInfoText = UI.find(v, R.id.txFeeInfoText);

            final TransactionItem txItem = (TransactionItem) getActivity().getIntent().getSerializableExtra("TRANSACTION");
            final GaActivity gaActivity = getGaActivity();

            openInBrowser(hashText, txItem.txHash.toString(), Network.BLOCKEXPLORER_TX);

            final Coin fee = Coin.valueOf(txItem.fee);
            final Coin feePerKb;
            if (txItem.size > 0) {
                feePerKb = Coin.valueOf(1000 * txItem.fee / txItem.size);
            } else {
                // shouldn't happen, but just in case let's avoid division by zero
                feePerKb = Coin.valueOf(0);
            }

            final boolean isWatchOnly = service.isWatchOnly();

            if (txItem.type.equals(TransactionItem.TYPE.OUT) || txItem.type.equals(TransactionItem.TYPE.REDEPOSIT) || txItem.isSpent) {
                if (txItem.getConfirmations() > 0) {
                    // confirmed - hide unconfirmed widgets
                    UI.hide((View) UI.find(v, R.id.txUnconfirmed),
                            unconfirmedRecommendation, unconfirmedIncreaseFee,
                            unconfirmedEstimatedBlocks);
                } else if (txItem.type.equals(TransactionItem.TYPE.OUT) || txItem.type.equals(TransactionItem.TYPE.REDEPOSIT)) {
                    // unconfirmed outgoing output/redeposit - can be RBF'd
                    int currentEstimate = 25, bestEstimate;
                    final Map<String, Object> feeEstimates = service.getFeeEstimates();
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
                    if (bestEstimate < currentEstimate && txItem.replaceable && !isWatchOnly) {
                        if (bestEstimate == 1)
                            unconfirmedRecommendation.setText(R.string.recommendationSingleBlock);
                        else
                            unconfirmedRecommendation.setText(String.format(getResources().getString(R.string.recommendationBlocks), bestEstimate));
                        unconfirmedIncreaseFee.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(final View v) {
                                final double feerate = Double.parseDouble(((Map) feeEstimates.get("1")).get("feerate").toString());
                                final Coin feerateCoin = Coin.valueOf((long) (feerate * 1000 * 1000 * 100));
                                replaceByFee(txItem, feerateCoin, null, 0);
                            }
                        });
                    } else
                        UI.hide(unconfirmedIncreaseFee, unconfirmedRecommendation);
                } else
                    // incoming spent - hide outgoing-only widgets
                    UI.hide(unconfirmedRecommendation, unconfirmedIncreaseFee,
                            unconfirmedEstimatedBlocks);
            } else {
                // unspent incoming output
                // incoming - hide outgoing-only widgets
                UI.hide(unconfirmedRecommendation, unconfirmedIncreaseFee,
                        unconfirmedEstimatedBlocks);
                if (txItem.getConfirmations() > 0)
                    if (isWatchOnly || txItem.spvVerified)
                        UI.hide((View) UI.find(v, R.id.txUnconfirmed));
                    else {
                        final int blocksLeft = service.getSPVBlocksRemaining();
                        final String message = getResources().getString(R.string.txUnverifiedTx);
                        if (blocksLeft != Integer.MAX_VALUE)
                            unconfirmedText.setText(String.format("%s %s", message, blocksLeft));
                        else
                            unconfirmedText.setText(String.format("%s %s", message,
                                                    "Not yet connected to SPV!"));
                    }
            }

            final String btcUnit = (String) service.getUserConfig("unit");
            final Coin coin = Coin.valueOf(txItem.amount);
            final MonetaryFormat mf = CurrencyMapper.mapBtcUnitToFormat(btcUnit);
            bitcoinScale.setText(CurrencyMapper.mapBtcUnitToPrefix(btcUnit));
            feeScale.setText(CurrencyMapper.mapBtcUnitToPrefix(btcUnit));
            if (btcUnit == null || btcUnit.equals("bits")) {
                bitcoinUnit.setText("bits ");
                feeUnit.setText("bits ");
            } else {
                bitcoinUnit.setText(R.string.fa_btc_space);
                feeUnit.setText(R.string.fa_btc_space);
            }
            final String btcBalance = mf.noCode().format(coin).toString();
            UI.setAmountText(amount, btcBalance);

            final String btcFee = mf.noCode().format(fee).toString();
            final String btcFeePerKb = mf.noCode().format(feePerKb).toString();
            String feeInfoTextStr = UI.setAmountText(null, btcFee);
            feeInfoTextStr += " / " + String.valueOf(txItem.size) + " / ";
            feeInfoTextStr += UI.setAmountText(null, btcFeePerKb);

            feeInfoText.setText(feeInfoTextStr);

            dateText.setText(SimpleDateFormat.getInstance().format(txItem.date));
            if (txItem.memo != null && txItem.memo.length() > 0) {
                memoText.setText(txItem.memo);
                if (isWatchOnly)
                    UI.hide(memoEdit);
            } else {
                UI.hide(memoText, (View) UI.find(v, R.id.txMemoMargin));
                if (isWatchOnly)
                    UI.hide((View) UI.find(v, R.id.txMemoTitle), memoEdit);
            }
            // FIXME: use a list instead of reusing a TextView to show all double spends to allow
            // for a warning to be shown before the browser is open
            // this is to prevent to accidentally leak to block explorers your addresses
            if (txItem.doubleSpentBy != null || txItem.replacedHashes.size() > 0) {
                CharSequence res = "";
                if (txItem.doubleSpentBy != null) {
                    if (txItem.doubleSpentBy.equals("malleability") || txItem.doubleSpentBy.equals("update"))
                        res = txItem.doubleSpentBy;
                    else
                        res = Html.fromHtml("<a href=\"" + Network.BLOCKEXPLORER_TX + "" + txItem.doubleSpentBy + "\">" + txItem.doubleSpentBy + "</a>");
                    if (txItem.replacedHashes.size() > 0)
                        res = TextUtils.concat(res, "; ");
                }
                if (txItem.replacedHashes.size() > 0) {
                    res = TextUtils.concat(res, Html.fromHtml("replaces transactions:<br/>"));
                    for (int i = 0; i < txItem.replacedHashes.size(); ++i) {
                        if (i > 0)
                            res = TextUtils.concat(res, Html.fromHtml("<br/>"));
                        final String txHashHex = txItem.replacedHashes.get(i).toString();
                        final String link = "<a href=\"" + Network.BLOCKEXPLORER_TX + txHashHex + "\">" + txHashHex + "</a>";
                        res = TextUtils.concat(res, Html.fromHtml(link));
                    }
                }
                doubleSpentByText.setText(res);
            } else
                UI.hide(doubleSpentByText, doubleSpentByTitle,
                        (View) UI.find(v, R.id.txDoubleSpentByMargin));

            if (txItem.counterparty != null && txItem.counterparty.length() > 0)
                recipientText.setText(txItem.counterparty);
            else
                UI.hide(recipientText, recipientTitle,
                       (View) UI.find(v, R.id.txRecipientMargin));

            if (txItem.receivedOn != null && txItem.receivedOn.length() > 0)
                openInBrowser(receivedOnText, txItem.receivedOn, Network.BLOCKEXPLORER_ADDRESS);
            else
                UI.hide(receivedOnText, receivedOnTitle,
                        (View) UI.find(v, R.id.txReceivedOnMargin));

            if (isWatchOnly)
                return v;

            memoEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final boolean editVisible = memoEditText.getVisibility() == View.VISIBLE;
                    memoEditText.setText(UI.getText(memoText));
                    UI.hideIf(editVisible, memoEditText, saveMemo);
                    UI.showIf(editVisible, memoText);
                }
            });

            saveMemo.setOnClickListener(new View.OnClickListener() {

                private void onDisableEdit() {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            memoText.setText(UI.getText(memoEditText));
                            UI.hide(saveMemo, memoEditText);
                            if (UI.getText(memoText).isEmpty())
                                UI.hide(memoText, (View) UI.find(v, R.id.txMemoMargin));
                            else
                                UI.show((View) UI.find(v, R.id.txMemoMargin), memoText);
                        }
                    });
                }

                @Override
                public void onClick(final View v) {
                    final String edited = UI.getText(memoEditText);
                    if (!edited.equals(UI.getText(memoText))) {
                        CB.after(service.changeMemo(txItem.txHash, edited),
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

            return v;
        }

        private void replaceByFee(final TransactionItem txItem, final Coin feerate, Integer txSize, final int level) {
            if (level > 10)
                throw new RuntimeException("Recursion limit exceeded");

            final GaActivity gaActivity = getGaActivity();
            final GaService service = getGAService();

            final Transaction tx = GaService.buildTransaction(txItem.data);
            Integer change_pointer = null;
            final int subAccount = service.getCurrentSubAccount();
            // requiredFeeDelta assumes mintxfee = 1000, and inputs increasing
            // by at most 4 bytes per input (signatures have variable lengths)
            if (txSize == null)
                txSize = tx.getMessageSize();
            long requiredFeeDelta = txSize + tx.getInputs().size() * 4;
            final List<TransactionInput> oldInputs = new ArrayList<>(tx.getInputs());
            tx.clearInputs();
            for (int i = 0; i < txItem.eps.size(); ++i) {
                final Map<String, Object> ep = (Map) txItem.eps.get(i);
                if (((Boolean) ep.get("is_credit"))) continue;
                final TransactionInput oldInput = oldInputs.get((Integer) ep.get("pt_idx"));
                final TransactionInput newInput = new TransactionInput(
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
            final Coin feeDelta = Coin.valueOf(Math.max(
                    newFeeWithRate.subtract(tx.getFee()).longValue(),
                    requiredFeeDelta
            ));
            final Coin oldFee = tx.getFee();
            Coin remainingFeeDelta = feeDelta;
            final List<TransactionOutput> origOuts = new ArrayList<>(tx.getOutputs());
            tx.clearOutputs();
            for (int i = 0; i < txItem.eps.size(); ++i) {
                final Map<String, Object> ep = (Map) txItem.eps.get(i);
                if (!((Boolean) ep.get("is_credit"))) continue;

                if (!((Boolean) ep.get("is_relevant")))
                    // keep non-change/non-redeposit intact
                    tx.addOutput(origOuts.get((Integer)ep.get("pt_idx")));
                else {
                    if ((ep.get("subaccount") == null && subAccount == 0) ||
                            ep.get("subaccount").equals(subAccount))
                        change_pointer = (Integer) ep.get("pubkey_pointer");
                    // change/redeposit
                    long value = Long.valueOf((String) ep.get("value"));
                    if (Coin.valueOf(value).compareTo(remainingFeeDelta) <= 0) {
                        // smaller than remaining fee -- get rid of this output
                        remainingFeeDelta = remainingFeeDelta.subtract(
                                Coin.valueOf(value)
                        );
                    } else {
                        // larger than remaining fee -- subtract the remaining fee
                        final TransactionOutput out = origOuts.get((Integer)ep.get("pt_idx"));
                        out.setValue(out.getValue().subtract(remainingFeeDelta));
                        tx.addOutput(out);
                        remainingFeeDelta = Coin.ZERO;
                    }
                }
            }

            if (remainingFeeDelta.compareTo(Coin.ZERO) <= 0) {
                doReplaceByFee(txItem, feerate, tx, change_pointer, subAccount, oldFee, null, null, level);
                return;
            }

            final Coin finalRemaining = remainingFeeDelta;
            CB.after(service.getAllUnspentOutputs(1, subAccount),
                     new CB.Toast<ArrayList>(gaActivity) {
                @Override
                public void onSuccess(final ArrayList result) {
                    Coin remaining = finalRemaining;
                    final List<ListenableFuture<byte[]>> scripts = new ArrayList<>();
                    final List<Map<String, Object>> moreInputs = new ArrayList<>();
                    for (final Object utxo_ : result) {
                        final Map<String, Object> utxo = (Map) utxo_;
                        remaining = remaining.subtract(Coin.valueOf(Long.valueOf((String)utxo.get("value"))));
                        scripts.add(service.createOutScript((Integer)utxo.get("subaccount"), (Integer)utxo.get("pointer")));
                        moreInputs.add(utxo);
                        if (remaining.compareTo(Coin.ZERO) <= 0)
                            break;
                    }

                    final int remainingCmp = remaining.compareTo(Coin.ZERO);
                    if (remainingCmp == 0) {
                        // Funds available exactly match the required value
                        CB.after(Futures.allAsList(scripts), new CB.Toast<List<byte[]>>(gaActivity) {
                            @Override
                            public void onSuccess(List<byte[]> morePrevouts) {
                                doReplaceByFee(txItem, feerate, tx, null, subAccount,
                                               oldFee, moreInputs, morePrevouts, level);
                            }
                        });
                        return;
                    }

                    if (remainingCmp > 0) {
                        // Not enough funds
                        gaActivity.toast(R.string.insufficientFundsText);
                        return;
                    }

                    // Funds left over - add a new change output
                    final Coin changeValue = remaining.multiply(-1);
                    CB.after(service.getNewAddress(subAccount),
                             new CB.Toast<Map>(gaActivity) {
                        @Override
                        public void onSuccess(final Map result) {
                            final byte[] script = Wally.hex_to_bytes((String) result.get("script"));
                            tx.addOutput(changeValue,
                                         Address.fromP2SHHash(Network.NETWORK, Utils.sha256hash160(script)));
                            CB.after(Futures.allAsList(scripts), new CB.Toast<List<byte[]>>(gaActivity) {
                                @Override
                                public void onSuccess(List<byte[]> morePrevouts) {
                                    doReplaceByFee(txItem, feerate, tx, (Integer) result.get("pointer"),
                                                   subAccount, oldFee, moreInputs, morePrevouts, level);
                                }
                            });
                        }
                    });
                }
            });
        }

        private void doReplaceByFee(final TransactionItem txItem, final Coin feerate,
                                    final Transaction tx,
                                    final Integer change_pointer, final int subAccount,
                                    final Coin oldFee, final List<Map<String, Object>> moreInputs,
                                    final List<byte[]> morePrevouts, final int level) {
            final GaActivity gaActivity = getGaActivity();
            final GaService service = getGAService();

            final PreparedTransaction ptx;
            ptx = new PreparedTransaction(change_pointer, subAccount, tx,
                                          service.findSubaccountByType(subAccount, "2of3"));

            for (final Map<String, Object> ep : (List<Map>)txItem.eps) {
                if (((Boolean) ep.get("is_credit")))
                    continue;
                final Integer prevIndex = ((Integer) ep.get("pt_idx"));
                final TransactionInput oldInput = tx.getInput(prevIndex);
                ptx.mPrevOutputs.add(new Output(
                        (Integer) ep.get("subaccount"),
                        (Integer) ep.get("pubkey_pointer"),
                        1,
                        (Integer) ep.get("script_type"),
                        Wally.hex_from_bytes(oldInput.getScriptSig().getChunks().get(3).data),
                        oldInput.getValue().longValue()
                ));
            }

            int i = 0;
            if (moreInputs != null) {
                for (final Map<String, Object> ep : moreInputs) {
                    ptx.mPrevOutputs.add(new Output(
                            (Integer) ep.get("subaccount"),
                            (Integer) ep.get("pointer"),
                            1,
                            TransactionItem.P2SH_FORTIFIED_OUT,
                            Wally.hex_from_bytes(morePrevouts.get(i)),
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
                                            Sha256Hash.wrap((String) ep.get("txhash"))
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
                replaceByFee(txItem, feerate, estimatedSize, level + 1);
                return;
            }

            // also verify if it's enough for 'bandwidth fee increment' condition
            // of RBF
            if (tx.getFee().subtract(oldFee).compareTo(Coin.valueOf(tx.getMessageSize() + tx.getInputs().size() * 4)) < 0) {
                replaceByFee(txItem, feerate, estimatedSize, level + 1);
                return;
            }

            final List<TransactionInput> inputs = tx.getInputs();
            final List<ListenableFuture<Void>> outpointToRaw = new ArrayList<>(inputs.size() + 1);
            if (!(service.getSigningWallet() instanceof TrezorHWWallet))
                outpointToRaw.add(Futures.immediateFuture((Void) null));
            else
                for (final TransactionInput inp : inputs) {
                    final Sha256Hash hash = inp.getOutpoint().getHash();
                    outpointToRaw.add(Futures.transform(service.getRawOutput(hash),
                            new Function<Transaction, Void>() {
                                @Override
                                public Void apply(final Transaction input) {
                                    ptx.mPrevoutRawTxs.put(Wally.hex_from_bytes(hash.getBytes()), input);
                                    return null;
                                }
                            }));
                }

            final ListenableFuture<List<Void>> prevouts = Futures.allAsList(outpointToRaw);
            final ListenableFuture<List<byte[]>> signed = Futures.transform(prevouts, new AsyncFunction<List<Void>, List<byte[]>>() {
                @Override
                public ListenableFuture<List<byte[]>> apply(final List<Void> input) throws Exception {
                    return service.signTransaction(ptx);
                }
            });

            CB.after(signed, new CB.Toast<List<byte[]>>(gaActivity) {
                @Override
                public void onSuccess(final List<byte[]> signatures) {
                    int i = 0;
                    for (final byte[] sig : signatures) {
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
                                        sig
                                ).addChunk(
                                        // the original outscript
                                        input.getScriptSig().getChunks().get(3)
                                ).build()
                        );
                    }
                    final Map<String, Object> twoFacData = new HashMap<>();
                    twoFacData.put("try_under_limits_bump", tx.getFee().subtract(oldFee).longValue());
                    final ListenableFuture<Map<String,Object>> sendFuture = service.sendRawTransaction(tx, twoFacData, true);
                    Futures.addCallback(sendFuture, new FutureCallback<Map<String,Object>>() {
                        @Override
                        public void onSuccess(final Map result) {
                            // FIXME: Add notification with "Transaction sent"?
                            gaActivity.finishOnUiThread();
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            if (!(t instanceof GAException) || !t.getMessage().equals("http://greenaddressit.com/error#auth")) {
                                gaActivity.toast(t);
                                return;
                            }
                            // 2FA is required, prompt the user
                            gaActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    final boolean skipChoice = false;
                                    mTwoFactor = UI.popupTwoFactorChoice(gaActivity, service, skipChoice,
                                                                                 new CB.Runnable1T<String>() {
                                        @Override
                                        public void run(final String method) {
                                            showIncreaseSummary(method, oldFee, tx.getFee(), tx);
                                        }
                                    });
                                    if (mTwoFactor != null)
                                        mTwoFactor.show();
                                }
                            });
                        }
                    }, service.getExecutor());
                }
            });
        }

        private void showIncreaseSummary(final String method, final Coin oldFee, final Coin newFee, final Transaction signedTx) {
            Log.i(TAG, "showIncreaseSummary( params " + method + " " + oldFee + " " + newFee + ")");
            final GaActivity gaActivity = getGaActivity();
            final GaService service = getGAService();

            final View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_new_transaction, null, false);

            final TextView amountLabel = UI.find(v, R.id.newTxAmountLabel);
            amountLabel.setText(R.string.newFeeText);
            final TextView amountText = UI.find(v, R.id.newTxAmountText);
            final TextView amountScale = UI.find(v, R.id.newTxAmountScaleText);
            final TextView amountUnit = UI.find(v, R.id.newTxAmountUnitText);
            final TextView feeLabel = UI.find(v, R.id.newTxFeeLabel);
            feeLabel.setText(R.string.oldFeeText);
            final TextView feeText = UI.find(v, R.id.newTxFeeText);
            final TextView feeScale = UI.find(v, R.id.newTxFeeScale);
            final TextView feeUnit = UI.find(v, R.id.newTxFeeUnit);

            UI.hide((View) UI.find(v, R.id.newTxRecipientLabel),
                    (View) UI.find(v, R.id.newTxRecipientText));
            final TextView twoFAText = UI.find(v, R.id.newTx2FATypeText);
            final EditText newTx2FACodeText = UI.find(v, R.id.newTx2FACodeText);

            final String btcUnit = (String) service.getUserConfig("unit");
            final String prefix = CurrencyMapper.mapBtcUnitToPrefix(btcUnit);

            amountScale.setText(prefix);
            feeScale.setText(prefix);
            if (btcUnit == null || btcUnit.equals("bits")) {
                amountUnit.setText("bits ");
                feeUnit.setText("bits ");
            } else {
                amountUnit.setText(R.string.fa_btc_space);
                feeUnit.setText(R.string.fa_btc_space);
            }
            final MonetaryFormat mf = CurrencyMapper.mapBtcUnitToFormat(btcUnit);
            amountText.setText(mf.noCode().format(newFee));
            feeText.setText(mf.noCode().format(oldFee));

            final Map<String, Object> twoFacData;
            if (method == null) {
                UI.hide(twoFAText, newTx2FACodeText);
                twoFacData = null;
            } else {
                twoFAText.setText(String.format("2FA %s code", method));
                twoFacData = new HashMap<>();
                twoFacData.put("method", method);
                twoFacData.put("bump_fee_amount", newFee.subtract(oldFee).longValue());
                if (!method.equals("gauth")) {
                    final Map<String, Long> amount = new HashMap<>();
                    amount.put("amount", newFee.subtract(oldFee).longValue());
                    service.requestTwoFacCode(method, "bump_fee", amount);
                }
            }

            mSummary = UI.popup(getActivity(), R.string.feeIncreaseTitle, R.string.send, R.string.cancel)
                    .customView(v, true)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(final MaterialDialog dialog, final DialogAction which) {
                            if (twoFacData != null)
                                twoFacData.put("code", UI.getText(newTx2FACodeText));
                            final ListenableFuture<Map<String,Object>> sendFuture = service.sendRawTransaction(signedTx, twoFacData, false);
                            Futures.addCallback(sendFuture, new CB.Toast<Map<String,Object>>(gaActivity) {
                                @Override
                                public void onSuccess(final Map result) {
                                    // FIXME: Add notification with "Transaction sent"?
                                    gaActivity.finishOnUiThread();
                                }
                            }, service.getExecutor());
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(final MaterialDialog dialog, final DialogAction which) {
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
