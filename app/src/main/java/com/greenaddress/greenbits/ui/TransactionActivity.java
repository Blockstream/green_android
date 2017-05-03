package com.greenaddress.greenbits.ui;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.blockstream.libwally.Wally;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.ConfidentialAddress;
import com.greenaddress.greenapi.GAException;
import com.greenaddress.greenapi.GATx;
import com.greenaddress.greenapi.HDKey;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.JSONMap;
import com.greenaddress.greenapi.Output;
import com.greenaddress.greenapi.PreparedTransaction;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.wallets.TrezorHWWallet;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.TransactionWitness;
import org.bitcoinj.core.Utils;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.script.Script;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TransactionActivity extends GaActivity {

    private static final String TAG = TransactionActivity.class.getSimpleName();
    private static final String FEE_BLOCK_NUMBERS[] = {"1", "3", "6"};

    // For debug regtest builds, always allow RBF (Useful for development/testing)
    private static final boolean ALWAYS_ALLOW_RBF = BuildConfig.DEBUG &&
        Network.NETWORK == RegTestParams.get();

    private Menu mMenu;
    private TextView mUnconfirmedText;
    private TextView mUnconfirmedEstimatedBlocks;
    private TextView mUnconfirmedRecommendation;
    private Button mUnconfirmedIncreaseFee;
    private View mMemoView;
    private View mMemoTitle;
    private TextView mMemoIcon;
    private TextView mMemoText;
    private TextView mMemoEditText;
    private Button mMemoSaveButton;
    private Dialog mSummary;
    private Dialog mTwoFactor;

    @Override
    protected int getMainViewId() { return R.layout.activity_transaction; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        setResult(RESULT_OK);

        mMemoView = UI.find(this, R.id.txMemoMargin);
        mMemoTitle = UI.find(this, R.id.txMemoTitle);
        mMemoIcon = UI.find(this, R.id.sendToNoteIcon);
        mMemoText = UI.find(this, R.id.txMemoText);
        mMemoEditText = UI.find(this, R.id.sendToNoteText);
        mMemoSaveButton = UI.find(this, R.id.saveMemo);
        mUnconfirmedText = UI.find(this, R.id.txUnconfirmedText);
        mUnconfirmedEstimatedBlocks = UI.find(this, R.id.txUnconfirmedEstimatedBlocks);
        mUnconfirmedRecommendation = UI.find(this, R.id.txUnconfirmedRecommendation);
        mUnconfirmedIncreaseFee = UI.find(this, R.id.txUnconfirmedIncreaseFee);

        final TextView doubleSpentByText = UI.find(this, R.id.txDoubleSpentByText);
        final TextView doubleSpentByTitle = UI.find(this, R.id.txDoubleSpentByTitle);

        final TextView recipientText = UI.find(this, R.id.txRecipientText);
        final TextView recipientTitle = UI.find(this, R.id.txRecipientTitle);

        // Hide outgoing-only widgets by default
        UI.hide(mUnconfirmedRecommendation, mUnconfirmedEstimatedBlocks, mUnconfirmedIncreaseFee);

        final TransactionItem txItem = (TransactionItem) getIntent().getSerializableExtra("TRANSACTION");

        final TextView hashText = UI.find(this, R.id.txHashText);
        openInBrowser(hashText, txItem.txHash.toString(), Network.BLOCKEXPLORER_TX, null);

        final Coin feePerKb = txItem.getFeePerKilobyte();

        final boolean isWatchOnly = mService.isWatchOnly();

        if (GaService.IS_ELEMENTS) {
            UI.hide(UI.find(this, R.id.txUnconfirmed));
        } else if (txItem.type == TransactionItem.TYPE.OUT || txItem.type == TransactionItem.TYPE.REDEPOSIT || txItem.isSpent) {
            if (txItem.getConfirmations() > 0)
                UI.hide(UI.find(this, R.id.txUnconfirmed)); // Confirmed: hide warning
            else if (txItem.type == TransactionItem.TYPE.OUT || txItem.type == TransactionItem.TYPE.REDEPOSIT)
                showUnconfirmed(txItem, feePerKb);
        } else {
            // unspent incoming output
            if (txItem.getConfirmations() > 0)
                if (isWatchOnly || txItem.spvVerified)
                    UI.hide(UI.find(this, R.id.txUnconfirmed));
                else {
                    final int blocksLeft = mService.getSPVBlocksRemaining();
                    final String message = getResources().getString(R.string.txUnverifiedTx);
                    if (blocksLeft != Integer.MAX_VALUE)
                        mUnconfirmedText.setText(String.format("%s %s", message, blocksLeft));
                    else
                        mUnconfirmedText.setText(String.format("%s %s", message,
                                                 "Not yet connected to SPV!"));
                }
        }

        UI.setCoinText(this, R.id.txBitcoinUnit, R.id.txAmountText,
                       Coin.valueOf(txItem.amount));

        final TextView feeUnit = UI.find(this, R.id.txFeeUnit);
        final TextView feeInfoText = UI.find(this, R.id.txFeeInfoText);
        feeInfoText.setText(UI.setCoinText(mService, feeUnit, null, Coin.valueOf(txItem.fee)) +
                            " / " + String.valueOf(txItem.size) + " / " +
                            UI.setCoinText(mService, feeUnit, null, feePerKb));

        final TextView dateText = UI.find(this, R.id.txDateText);
        dateText.setText(SimpleDateFormat.getInstance().format(txItem.date));

        // FIXME: use a list instead of reusing a TextView to show all double spends to allow
        // for a warning to be shown before the browser is open
        // this is to prevent to accidentally leak to block explorers your addresses
        if (txItem.doubleSpentBy != null || !txItem.replacedHashes.isEmpty()) {
            CharSequence res = "";
            if (txItem.doubleSpentBy != null) {
                if (txItem.doubleSpentBy.equals("malleability") || txItem.doubleSpentBy.equals("update"))
                    res = txItem.doubleSpentBy;
                else
                    res = Html.fromHtml("<a href=\"" + Network.BLOCKEXPLORER_TX + txItem.doubleSpentBy + "\">" + txItem.doubleSpentBy + "</a>");
                if (!txItem.replacedHashes.isEmpty())
                    res = TextUtils.concat(res, "; ");
            }
            if (!txItem.replacedHashes.isEmpty()) {
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
                    UI.find(this, R.id.txDoubleSpentByMargin));

        if (!TextUtils.isEmpty(txItem.counterparty))
            recipientText.setText(txItem.counterparty);
        else
            UI.hide(recipientText, recipientTitle,
                    UI.find(this, R.id.txRecipientMargin));

        final TextView receivedOnText = UI.find(this, R.id.txReceivedOnText);
        if (!TextUtils.isEmpty(txItem.receivedOn))
            openInBrowser(
                    receivedOnText, txItem.receivedOn, Network.BLOCKEXPLORER_ADDRESS,
                    txItem.receivedOnEp
            );
        else {
            final View receivedOnTitle = UI.find(this, R.id.txReceivedOnTitle);
            final View receivedOnMargin = UI.find(this, R.id.txReceivedOnMargin);
            UI.hide(receivedOnText, receivedOnTitle, receivedOnMargin);
        }

        // Memo
        if (!TextUtils.isEmpty(txItem.memo)) {
            mMemoText.setText(txItem.memo);
            UI.hideIf(isWatchOnly, mMemoIcon);
        } else {
            UI.hide(mMemoText, mMemoView);
            UI.hideIf(isWatchOnly, mMemoTitle, mMemoIcon);
        }

        if (isWatchOnly)
            return;

        mMemoIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final boolean editInProgress = mMemoEditText.getVisibility() == View.VISIBLE;
                mMemoEditText.setText(UI.getText(mMemoText));
                UI.hideIf(editInProgress, mMemoEditText, mMemoSaveButton);
                UI.showIf(editInProgress, mMemoText);
            }
        });

        mMemoSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                saveMemo(txItem.txHash);
            }
        });
    }

    private void showUnconfirmed(final TransactionItem txItem, final Coin feePerKb) {
        UI.show(mUnconfirmedEstimatedBlocks);

        final Map<String, Object> feeEstimates = mService.getFeeEstimates();
        int currentEstimate = 25;
        for (final String atBlock : FEE_BLOCK_NUMBERS)
            if (!feePerKb.isLessThan(getFeeEstimate(feeEstimates, atBlock))) {
                currentEstimate = (Integer) ((Map) feeEstimates.get(atBlock)).get("blocks");
                break;
            }

        mUnconfirmedEstimatedBlocks.setText(String.format(getResources().getString(R.string.willConfirmAfter), currentEstimate));
        if (mService.isWatchOnly() || GaService.IS_ELEMENTS || !txItem.replaceable)
            return; // FIXME: Implement RBF for elements

        final int bestEstimate = (Integer) ((Map) feeEstimates.get("1")).get("blocks");
        if (ALWAYS_ALLOW_RBF || (bestEstimate < currentEstimate)) {
            UI.show(mUnconfirmedRecommendation, mUnconfirmedIncreaseFee);
            if (bestEstimate == 1)
                mUnconfirmedRecommendation.setText(R.string.recommendationSingleBlock);
            else
                mUnconfirmedRecommendation.setText(getString(R.string.recommendationBlocks, bestEstimate));
            mUnconfirmedIncreaseFee.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    replaceByFee(txItem, getFeeEstimate(feeEstimates, "1"), null, 0);
                }
            });
        }
    }

    @Override
    public void onResumeWithService() {
        if (mService.isForcedOff()) {
            // FIXME: Should pass flag to activity so it shows it was forced logged out
            startActivity(new Intent(this, FirstScreenActivity.class));
            finish();
            return;
        }

        setMenuItemVisible(mMenu, R.id.action_share, !mService.isLoggedIn());
    }

    @Override
    public void onPauseWithService() {
        if (mSummary != null)
            mSummary.dismiss();
        if (mTwoFactor != null)
            mTwoFactor.dismiss();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_transaction, menu);
        mMenu = menu;
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

    private void onFinishedSavingMemo() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMemoText.setText(UI.getText(mMemoEditText));
                UI.hide(mMemoEditText, mMemoSaveButton);
                UI.hideIf(UI.getText(mMemoText).isEmpty(),
                          mMemoText, mMemoView);
                hideKeyboardFrom(mMemoEditText);
            }
        });
    }

    private void saveMemo(final Sha256Hash txHash) {
        final String newMemo = UI.getText(mMemoEditText);
        if (newMemo.equals(UI.getText(mMemoText))) {
            onFinishedSavingMemo();
            return;
        }

        CB.after(mService.changeMemo(txHash, newMemo),
                new CB.Toast<Boolean>(this) {
                    @Override
                    public void onSuccess(final Boolean result) {
                        onFinishedSavingMemo();
                    }
                });
    }

    private static Coin getFeeEstimate(final Map<String, Object> feeEstimates, final String atBlock) {
        final double rate = Double.parseDouble(((Map)feeEstimates.get(atBlock)).get("feerate").toString());
        return Coin.valueOf((long) (rate * 1000 * 1000 * 100));
    }

    private void openInBrowser(final TextView textView, final String identifier, final String url, final JSONMap confidentialData) {
        if (confidentialData == null)
            textView.setText(identifier);
        else {
            final int subaccount = confidentialData.getInt("subaccount", 0);
            final int pointer = confidentialData.getInt("pubkey_pointer");
            textView.setText(ConfidentialAddress.fromP2SHHash(
                    Network.NETWORK,
                    Wally.hash160(mService.createOutScript(subaccount, pointer)),
                    mService.getBlindingPubKey(subaccount, pointer)
            ).toString());
        }

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (url == null)
                    return;

                String domain = url;
                try {
                    domain = new URI(url).getHost();
                } catch (final URISyntaxException e) {
                    e.printStackTrace();
                }

                final String stripped = domain.startsWith("www.") ? domain.substring(4) : domain;

                UI.popup(TransactionActivity.this, R.string.warning, R.string.continueText, R.string.cancel)
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

    private void replaceByFee(final TransactionItem txItem, final Coin feerate, Integer txSize, final int level) {
        if (level > 10)
            throw new RuntimeException("Recursion limit exceeded");

        final Transaction tx = GaService.buildTransaction(txItem.data);
        Integer change_pointer = null;
        final int subAccount = mService.getCurrentSubAccount();
        // requiredFeeDelta assumes mintxfee = 1000, and inputs increasing
        // by at most 4 bytes per input (signatures have variable lengths)
        if (txSize == null)
            txSize = tx.getMessageSize();
        final long requiredFeeDelta = txSize + tx.getInputs().size() * 4;
        final List<TransactionInput> oldInputs = new ArrayList<>(tx.getInputs());
        tx.clearInputs();
        final List<JSONMap> tx_eps = new ArrayList<>();
        for (final JSONMap ep : txItem.eps) {
            if (ep.getBool("is_credit"))
                continue;
            final TransactionInput oldInput = oldInputs.get(ep.getInt("pt_idx"));
            final TransactionInput newInput = new TransactionInput(
                    Network.NETWORK,
                    null,
                    oldInput.getScriptBytes(),
                    oldInput.getOutpoint(),
                    ep.getCoin("value")
            );
            newInput.setSequenceNumber(0); // This ensures nlocktime is recognized
            tx.addInput(newInput);
            tx_eps.add(ep);
        }
        final Coin oldFee = tx.getFee();
        final Coin newFeeWithRate = feerate.multiply(txSize).divide(1000);
        final Coin feeDelta = Coin.valueOf(Math.max(
                newFeeWithRate.subtract(oldFee).longValue(),
                requiredFeeDelta
        ));
        Coin remainingFeeDelta = feeDelta;
        final List<TransactionOutput> origOuts = new ArrayList<>(tx.getOutputs());
        tx.clearOutputs();

        for (final JSONMap ep : txItem.eps) {
            if (!ep.getBool("is_credit"))
                continue;

            if (!ep.getBool("is_relevant"))
                // keep non-change/non-redeposit intact
                tx.addOutput(origOuts.get(ep.getInt("pt_idx")));
            else {
                if (ep.getInt("subaccount", 0).equals(subAccount))
                    change_pointer = ep.getInt("pubkey_pointer");

                // change/redeposit
                final Coin value = ep.getCoin("value");
                if (!value.isGreaterThan(remainingFeeDelta)) {
                    // smaller than remaining fee -- get rid of this output
                    remainingFeeDelta = remainingFeeDelta.subtract(value);
                } else {
                    // larger than remaining fee -- subtract the remaining fee
                    final TransactionOutput out = origOuts.get(ep.getInt("pt_idx"));
                    out.setValue(out.getValue().subtract(remainingFeeDelta));
                    tx.addOutput(out);
                    remainingFeeDelta = Coin.ZERO;
                }
            }
        }

        if (!remainingFeeDelta.isGreaterThan(Coin.ZERO)) {
            doReplaceByFee(txItem, feerate, tx, tx_eps, change_pointer, subAccount, oldFee, null, level);
            return;
        }

        final Coin finalRemaining = remainingFeeDelta;
        final boolean filterAsset = true; // TODO: Elements doesn't support RBF yet
        CB.after(mService.getAllUnspentOutputs(1, subAccount, filterAsset),
                 new CB.Toast<List<JSONMap>>(TransactionActivity.this) {
            @Override
            public void onSuccess(final List<JSONMap> result) {
                Coin remaining = finalRemaining;
                final List<JSONMap> moreInputs = new ArrayList<>();

                for (final JSONMap utxo : result) {
                    remaining = remaining.subtract(utxo.getCoin("value"));
                    moreInputs.add(utxo);
                    if (!remaining.isGreaterThan(Coin.ZERO))
                        break;
                }

                final int remainingCmp = remaining.compareTo(Coin.ZERO);
                if (remainingCmp > 0) {
                    // Not enough funds
                    TransactionActivity.this.toast(R.string.insufficientFundsText);
                    return;
                }

                if (remainingCmp == 0) {
                    // Funds available exactly match the required value
                    doReplaceByFee(txItem, feerate, tx, tx_eps, null, subAccount,
                                   oldFee, moreInputs, level);
                    return;
                }

                // Funds left over - add a new change output
                final Coin changeValue = remaining.multiply(-1);
                final JSONMap addr = mService.getNewAddress(subAccount);
                if (addr == null) {
                    TransactionActivity.this.toast(R.string.unable_to_create_change);
                    return;
                }
                final byte[] script = addr.getBytes("script");
                tx.addOutput(changeValue,
                             Address.fromP2SHHash(Network.NETWORK, Utils.sha256hash160(script)));
                doReplaceByFee(txItem, feerate, tx, tx_eps, addr.getInt("pointer"),
                               subAccount, oldFee, moreInputs, level);
            }
        });
    }

    private void doReplaceByFee(final TransactionItem txItem, final Coin feerate,
                                final Transaction tx, final List<JSONMap> tx_eps,
                                final Integer change_pointer, final int subAccount,
                                final Coin oldFee, final List<JSONMap> moreInputs,
                                final int level) {
        final PreparedTransaction ptx;
        ptx = new PreparedTransaction(change_pointer, subAccount, tx,
                                      mService.findSubaccountByType(subAccount, "2of3"));

        for (final JSONMap ep : txItem.eps) {
            if (ep.getBool("is_credit"))
                continue;
            final Integer prevIndex = ep.get("pt_idx");
            final TransactionInput oldInput = tx.getInput(prevIndex);
            final int scriptType = ep.getInt("script_type");
            ptx.mPrevOutputs.add(new Output(
                    (Integer) ep.get("subaccount"),
                    (Integer) ep.get("pubkey_pointer"),
                    HDKey.BRANCH_REGULAR,
                    GATx.getOutScriptType(scriptType),
                    Wally.hex_from_bytes(GATx.createOutScript(mService, ep)),
                    oldInput.getValue().longValue()
            ));
        }

        if (moreInputs != null) {
            for (final JSONMap ep : moreInputs) {
                final int scriptType = ep.getInt("script_type");
                final byte[] outscript = GATx.createOutScript(mService, ep);
                ptx.mPrevOutputs.add(new Output(
                        ep.getInt("subaccount"),
                        ep.getInt("pointer"),
                        1,
                        GATx.getOutScriptType(scriptType),
                        Wally.hex_from_bytes(outscript),
                        ep.getLong("value")
                ));
                GATx.addInput(mService, tx, ep);
                tx_eps.add(ep);
            }
        }

        // verify if the increased fee is enough to achieve wanted feerate
        // (can be too small in case of added inputs)
        final int estimatedSize = tx.getMessageSize() + tx.getInputs().size() * 4;
        if (feerate.multiply(estimatedSize).divide(1000).isGreaterThan(tx.getFee())) {
            replaceByFee(txItem, feerate, estimatedSize, level + 1);
            return;
        }

        // also verify if it's enough for 'bandwidth fee increment' condition
        // of RBF
        final Coin bandwidthAdjustedFee = Coin.valueOf(tx.getMessageSize() + tx.getInputs().size() * 4);
        if (tx.getFee().subtract(oldFee).isLessThan(bandwidthAdjustedFee)) {
            replaceByFee(txItem, feerate, estimatedSize, level + 1);
            return;
        }

        final List<TransactionInput> inputs = tx.getInputs();
        final List<ListenableFuture<Void>> outpointToRaw = new ArrayList<>(inputs.size() + 1);
        if (!(mService.getSigningWallet() instanceof TrezorHWWallet))
            outpointToRaw.add(Futures.immediateFuture((Void) null));
        else
            for (final TransactionInput inp : inputs) {
                final Sha256Hash hash = inp.getOutpoint().getHash();
                outpointToRaw.add(Futures.transform(mService.getRawOutput(hash),
                        new Function<Transaction, Void>() {
                            @Override
                            public Void apply(final Transaction input) {
                                ptx.mPrevoutRawTxs.put(Wally.hex_from_bytes(hash.getReversedBytes()), input);
                                return null;
                            }
                        }));
            }

        final ListenableFuture<List<Void>> prevouts = Futures.allAsList(outpointToRaw);
        final ListenableFuture<List<byte[]>> signed = Futures.transform(prevouts, new AsyncFunction<List<Void>, List<byte[]>>() {
            @Override
            public ListenableFuture<List<byte[]>> apply(final List<Void> input) throws Exception {
                return mService.signTransaction(ptx);
            }
        });

        final boolean isSegwitEnabled = mService.isSegwitEnabled();

        CB.after(signed, new CB.Toast<List<byte[]>>(TransactionActivity.this) {
            @Override
            public void onSuccess(final List<byte[]> signatures) {
                int i = 0;
                for (final byte[] sig : signatures) {
                    final JSONMap ep = tx_eps.get(i);
                    final int scriptType = ep.getInt("script_type");
                    final byte[] outscript = GATx.createOutScript(mService, ep);
                    final List<byte[]> userSigs = ImmutableList.of(new byte[]{0}, sig);
                    final byte[] inscript = GATx.createInScript(userSigs, outscript, scriptType);

                    tx.getInput(i).setScriptSig(new Script(inscript));
                    if (isSegwitEnabled && scriptType == GATx.REDEEM_P2SH_P2WSH_FORTIFIED) {
                        final TransactionWitness witness = new TransactionWitness(1);
                        witness.setPush(0, sig);
                        tx.setWitness(i, witness);
                    }
                    i++;
                }
                final Map<String, Object> twoFacData = new HashMap<>();
                twoFacData.put("try_under_limits_bump", tx.getFee().subtract(oldFee).longValue());
                final ListenableFuture<Map<String,Object>> sendFuture;
                sendFuture = mService.sendRawTransaction(tx, twoFacData, null, true);
                Futures.addCallback(sendFuture, new FutureCallback<Map<String,Object>>() {
                    @Override
                    public void onSuccess(final Map result) {
                        // FIXME: Add notification with "Transaction sent"?
                        finishOnUiThread();
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        if (!(t instanceof GAException) || !t.getMessage().equals("http://greenaddressit.com/error#auth")) {
                            toast(t);
                            return;
                        }
                        // 2FA is required, prompt the user
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final boolean skipChoice = false;
                                mTwoFactor = UI.popupTwoFactorChoice(TransactionActivity.this, mService, skipChoice,
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
                }, mService.getExecutor());
            }
        });
    }

    private void showIncreaseSummary(final String method, final Coin oldFee, final Coin newFee, final Transaction signedTx) {
        Log.i(TAG, "showIncreaseSummary( params " + method + ' ' + oldFee + ' ' + newFee + ')');
        final View v = getLayoutInflater().inflate(R.layout.dialog_new_transaction, null, false);

        final TextView amountLabel = UI.find(v, R.id.newTxAmountLabel);
        amountLabel.setText(R.string.newFeeText);
        final TextView feeLabel = UI.find(v, R.id.newTxFeeLabel);
        feeLabel.setText(R.string.oldFeeText);

        UI.hide(UI.find(v, R.id.newTxRecipientLabel), UI.find(v, R.id.newTxRecipientText));
        final TextView twoFAText = UI.find(v, R.id.newTx2FATypeText);
        final EditText newTx2FACodeText = UI.find(v, R.id.newTx2FACodeText);

        UI.setCoinText(mService, v, R.id.newTxAmountUnitText, R.id.newTxAmountText, newFee);
        UI.setCoinText(mService, v, R.id.newTxFeeUnit, R.id.newTxFeeText, oldFee);

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
                mService.requestTwoFacCode(method, "bump_fee", amount);
            }
        }

        mSummary = UI.popup(this, R.string.feeIncreaseTitle, R.string.send, R.string.cancel)
                .customView(v, true)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final MaterialDialog dialog, final DialogAction which) {
                        if (twoFacData != null)
                            twoFacData.put("code", UI.getText(newTx2FACodeText));
                        final ListenableFuture<Map<String,Object>> sendFuture;
                        sendFuture = mService.sendRawTransaction(signedTx, twoFacData, null, false);
                        Futures.addCallback(sendFuture, new CB.Toast<Map<String,Object>>(TransactionActivity.this) {
                            @Override
                            public void onSuccess(final Map result) {
                                // FIXME: Add notification with "Transaction sent"?
                                finishOnUiThread();
                            }
                        }, mService.getExecutor());
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final MaterialDialog dialog, final DialogAction which) {
                        Log.i(TAG, "SHOWN ON CLOSE!");
                    }
                })
                .build();
        UI.mapEnterToPositive(mSummary, R.id.newTx2FACodeText);
        mSummary.show();
    }
}
