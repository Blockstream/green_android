package com.greenaddress.greenbits.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.blockstream.libwally.Wally;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.ConfidentialAddress;
import com.greenaddress.greenapi.GAException;
import com.greenaddress.greenapi.GATx;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.JSONMap;
import com.greenaddress.greenapi.PreparedTransaction;
import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.params.RegTestParams;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TransactionActivity extends GaActivity implements View.OnClickListener {

    private static final String TAG = TransactionActivity.class.getSimpleName();
    private static final int FEE_BLOCK_NUMBERS[] = {1, 3, 6};

    // For debug regtest builds, always allow RBF (Useful for development/testing)
    private static final boolean ALWAYS_ALLOW_RBF = BuildConfig.DEBUG &&
        Network.NETWORK == RegTestParams.get();

    private Menu mMenu;
    private TextView mUnconfirmedText;
    private TextView mEstimatedBlocks;
    private TextView mUnconfirmedRecommendation;
    private Button mBumpFeeButton;
    private View mMemoView;
    private View mMemoTitle;
    private TextView mMemoIcon;
    private TextView mMemoText;
    private TextView mMemoEditText;
    private Button mMemoSaveButton;
    private Dialog mSummary;
    private Dialog mTwoFactor;

    private int mTwoFactorAttemptsRemaining;
    private TransactionItem mTxItem;
    private Coin mChosenFeeRate;
    private final boolean mSummaryInBtc[] = new boolean[1];  // State for fiat/btc toggle

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
        mEstimatedBlocks = UI.find(this, R.id.txUnconfirmedEstimatedBlocks);
        mUnconfirmedRecommendation = UI.find(this, R.id.txUnconfirmedRecommendation);
        mBumpFeeButton = UI.find(this, R.id.txUnconfirmedIncreaseFee);

        final TextView doubleSpentByText = UI.find(this, R.id.txDoubleSpentByText);
        final TextView doubleSpentByTitle = UI.find(this, R.id.txDoubleSpentByTitle);

        final TextView recipientText = UI.find(this, R.id.txRecipientText);
        final TextView recipientTitle = UI.find(this, R.id.txRecipientTitle);

        // Hide outgoing-only widgets by default
        UI.hide(mUnconfirmedRecommendation, mEstimatedBlocks, mBumpFeeButton);

        mTxItem = (TransactionItem) getIntent().getSerializableExtra("TRANSACTION");

        final TextView hashText = UI.find(this, R.id.txHashText);
        openInBrowser(hashText, mTxItem.txHash.toString(), Network.BLOCKEXPLORER_TX, null);

        showFeeInfo(mTxItem.fee, mTxItem.size, mTxItem.getFeePerKilobyte());

        final boolean isWatchOnly = mService.isWatchOnly();

        if (GaService.IS_ELEMENTS) {
            UI.hide(UI.find(this, R.id.txUnconfirmed));
        } else if (mTxItem.type == TransactionItem.TYPE.OUT || mTxItem.type == TransactionItem.TYPE.REDEPOSIT || mTxItem.isSpent) {
            if (mTxItem.getConfirmations() > 0)
                UI.hide(UI.find(this, R.id.txUnconfirmed)); // Confirmed: hide warning
            else if (mTxItem.type == TransactionItem.TYPE.OUT || mTxItem.type == TransactionItem.TYPE.REDEPOSIT)
                showUnconfirmed();
        } else {
            // unspent incoming output
            if (mTxItem.getConfirmations() > 0)
                if (isWatchOnly || mTxItem.spvVerified)
                    UI.hide(UI.find(this, R.id.txUnconfirmed));
                else {
                    final int blocksLeft = mService.getSPVBlocksRemaining();
                    final String message = getString(R.string.txUnverifiedTx);
                    if (blocksLeft != Integer.MAX_VALUE)
                        mUnconfirmedText.setText(String.format("%s %s", message, blocksLeft));
                    else
                        mUnconfirmedText.setText(String.format("%s %s", message,
                                                 "Not yet connected to SPV!"));
                }
        }

        UI.setCoinText(this, R.id.txBitcoinUnit, R.id.txAmountText,
                       Coin.valueOf(mTxItem.amount));

        final TextView dateText = UI.find(this, R.id.txDateText);
        dateText.setText(SimpleDateFormat.getInstance().format(mTxItem.date));

        // FIXME: use a list instead of reusing a TextView to show all double spends to allow
        // for a warning to be shown before the browser is open
        // this is to prevent to accidentally leak to block explorers your addresses
        if (mTxItem.doubleSpentBy != null || !mTxItem.replacedHashes.isEmpty()) {
            CharSequence res = "";
            if (mTxItem.doubleSpentBy != null) {
                if (mTxItem.doubleSpentBy.equals("malleability") || mTxItem.doubleSpentBy.equals("update"))
                    res = mTxItem.doubleSpentBy;
                else
                    res = Html.fromHtml("<a href=\"" + Network.BLOCKEXPLORER_TX + mTxItem.doubleSpentBy + "\">" + mTxItem.doubleSpentBy + "</a>");
                if (!mTxItem.replacedHashes.isEmpty())
                    res = TextUtils.concat(res, "; ");
            }
            if (!mTxItem.replacedHashes.isEmpty()) {
                res = TextUtils.concat(res, Html.fromHtml("replaces transactions:<br/>"));
                for (int i = 0; i < mTxItem.replacedHashes.size(); ++i) {
                    if (i > 0)
                        res = TextUtils.concat(res, Html.fromHtml("<br/>"));
                    final String txHashHex = mTxItem.replacedHashes.get(i).toString();
                    final String link = "<a href=\"" + Network.BLOCKEXPLORER_TX + txHashHex + "\">" + txHashHex + "</a>";
                    res = TextUtils.concat(res, Html.fromHtml(link));
                }
            }
            doubleSpentByText.setText(res);
        } else
            UI.hide(doubleSpentByText, doubleSpentByTitle,
                    UI.find(this, R.id.txDoubleSpentByMargin));

        if (!TextUtils.isEmpty(mTxItem.counterparty))
            recipientText.setText(mTxItem.counterparty);
        else
            UI.hide(recipientText, recipientTitle,
                    UI.find(this, R.id.txRecipientMargin));

        final TextView receivedOnText = UI.find(this, R.id.txReceivedOnText);
        if (!TextUtils.isEmpty(mTxItem.receivedOn))
            openInBrowser(receivedOnText, mTxItem.receivedOn, Network.BLOCKEXPLORER_ADDRESS,
                          mTxItem.receivedOnEp);
        else {
            final View receivedOnTitle = UI.find(this, R.id.txReceivedOnTitle);
            final View receivedOnMargin = UI.find(this, R.id.txReceivedOnMargin);
            UI.hide(receivedOnText, receivedOnTitle, receivedOnMargin);
        }

        // Memo
        if (!TextUtils.isEmpty(mTxItem.memo)) {
            mMemoText.setText(mTxItem.memo);
            UI.hideIf(isWatchOnly, mMemoIcon);
        } else {
            UI.hide(mMemoText, mMemoView);
            UI.hideIf(isWatchOnly, mMemoTitle, mMemoIcon);
        }

        if (!isWatchOnly) {
            mMemoIcon.setOnClickListener(this);
            mMemoSaveButton.setOnClickListener(this);
        }
    }

    private void showFeeInfo(final long fee, final long vSize, final Coin feeRate) {
        final FontAwesomeTextView feeUnit = UI.find(this, R.id.txFeeUnit);
        final TextView feeText = UI.find(this, R.id.txFeeInfoText);
        feeText.setText(UI.setCoinText(mService, feeUnit, null, Coin.valueOf(fee)) +
                        " / " + String.valueOf(vSize) + " / " +
                        UI.setCoinText(mService, feeUnit, null, feeRate));
    }

    private void showUnconfirmed() {
        UI.show(mEstimatedBlocks);

        // FIXME: The fee rate for segwit txs without txdata is not
        // correct, because the backend does not provide vSize. So,
        // we re-compute it here.
        final double satoshiPerKb;
        final int vSize;

        if (mTxItem.data != null && mService.isSegwitEnabled()) {
            // Compute the correct fee rate as we have tx data available
            final Transaction tx = GaService.buildTransaction(mTxItem.data);
            vSize = GATx.getTxVSize(tx);
            satoshiPerKb = Math.ceil(mTxItem.fee * 1000.0 / vSize);
            // Update displayed fee info: its incorrect due to the FIXME above
            showFeeInfo(mTxItem.fee, vSize, Coin.valueOf((long) satoshiPerKb));
        } else {
            vSize = mTxItem.size;
            satoshiPerKb = mTxItem.getFeePerKilobyte().value;
        }
        final double currentFeeRate = satoshiPerKb / 100000000.0; // ->BTC/KB

        // Compute the number of expected blocks before this tx confirms
        int estimatedBlocks = 25;
        for (final int blockNum : FEE_BLOCK_NUMBERS) {
            final double blockFeeRate = mService.getFeeRate(blockNum);
            if (blockFeeRate >=0 && currentFeeRate >= blockFeeRate) {
                estimatedBlocks = mService.getFeeBlocks(blockNum);
                break;
            }
        }

        mEstimatedBlocks.setText(getString(R.string.willConfirmAfter, estimatedBlocks));
        if (mService.isWatchOnly() || GaService.IS_ELEMENTS || !mTxItem.replaceable)
            return; // FIXME: Implement RBF for elements

        // If the fastest number of blocks is less than the expected number,
        // then allow the user to RBF the fee up to the fastest fee rate
        final int fastestBlocks = mService.getFeeBlocks(1);
        boolean allowRbf = fastestBlocks < estimatedBlocks;

        double fastestFeeRate = mService.getFeeRate(1);
        if (fastestFeeRate >= 0) {
            final double rounded = Math.ceil(fastestFeeRate * 100000000.0);
            mChosenFeeRate = Coin.valueOf((long) rounded);
        } else {
            // No fastest fee rate is available: allow the user to RBF to the
            // current new transaction rate for 1 block confirmation if it is
            // higher than the current rate
            final boolean isInstant = false; // FIXME: Support instant RBF
            try {
                mChosenFeeRate = GATx.getFeeEstimateForRBF(mService, isInstant);
            } catch (final Throwable e) {
                toast(e);
                return;
            }
            fastestFeeRate = mChosenFeeRate.getValue() / 100000000.0;
            allowRbf = fastestFeeRate > currentFeeRate;
        }

        if (!allowRbf) {
            if (!ALWAYS_ALLOW_RBF)
                return;
            // Core rejects a bumped fee rate that is not higher than the old
            // one. On regtest the rates are usually unavailable and so the
            // old and new rates are the same. Increment the new rate by 1 to
            // avoid core failing the bump.
            final double rounded = Math.ceil(currentFeeRate * 100000000.0);
            mChosenFeeRate = Coin.valueOf((long) rounded + vSize);
        }

        UI.show(mUnconfirmedRecommendation, mBumpFeeButton);
        if (fastestBlocks == 1)
            mUnconfirmedRecommendation.setText(R.string.recommendationSingleBlock);
        else
            mUnconfirmedRecommendation.setText(getString(R.string.recommendationBlocks, fastestBlocks));

        mBumpFeeButton.setOnClickListener(this);
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
        mSummary = UI.dismiss(this, mSummary);
        mTwoFactor = UI.dismiss(this, mTwoFactor);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UI.unmapClick(mMemoIcon);
        UI.unmapClick(mMemoSaveButton);
        UI.unmapClick(mBumpFeeButton);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_transaction, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public void onClick(final View v) {
        if (v == mMemoIcon)
            onMemoIconClicked();
        else if (v == mMemoSaveButton)
            onMemoSaveButtonClicked();
        else if (v == mBumpFeeButton)
            onBumpFeeButtonClicked();
    }

    private void onMemoIconClicked() {
        final boolean editInProgress = mMemoEditText.getVisibility() == View.VISIBLE;
        mMemoEditText.setText(UI.getText(mMemoText));
        UI.hideIf(editInProgress, mMemoEditText, mMemoSaveButton);
        UI.showIf(editInProgress, mMemoText);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_share:
                final Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, Network.BLOCKEXPLORER_TX + mTxItem.txHash.toString());
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

    private void onMemoSaveButtonClicked() {
        final String newMemo = UI.getText(mMemoEditText);
        if (newMemo.equals(UI.getText(mMemoText))) {
            onFinishedSavingMemo();
            return;
        }

        CB.after(mService.changeMemo(mTxItem.txHash.toString(), newMemo),
                new CB.Toast<Boolean>(this) {
                    @Override
                    public void onSuccess(final Boolean result) {
                        onFinishedSavingMemo();
                    }
                });
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

    private List<JSONMap> getUtxosFromEndpoints() {
        final List<JSONMap> utxos = new ArrayList<>();
        for (final JSONMap ep : mTxItem.eps)
            if (!ep.getBool("is_credit"))
                utxos.add(ep);
        return utxos;
    }

    private static Coin getUtxoSum(final List<JSONMap> utxos) {
        Coin value = Coin.ZERO;
        for (final JSONMap utxo : utxos)
            value = value.add(Coin.valueOf(utxo.getLong("value")));
        return value;
    }

    // Modify tx to pay fees at the chosen feeRate
    private void onBumpFeeButtonClicked() {
        // For RBF we must ensure that the fee is incremented by at least the
        // minimum relay fee of the original transaction (per BIP 125), i.e.
        // the new fee must be higher than the old fee plus the bandwidth fee.
        final Transaction tx = GaService.buildTransaction(mTxItem.data);
        final Coin oldFee = Coin.valueOf(mTxItem.fee);
        Coin amount = tx.getOutputSum();

        final List<JSONMap> usedUtxos = getUtxosFromEndpoints();
        final int subAccount = mService.getCurrentSubAccount();

        final GATx.ChangeOutput changeOutput = GATx.findChangeOutput(mTxItem.eps, tx, subAccount);
        if (changeOutput != null) {
            // Either this tx has change, or it is a changeless re-deposit.
            if (tx.getOutputs().size() != 1) {
                // Not a changeless re-deposit
                // Subtract the change from the output amount
                amount = amount.subtract(changeOutput.mOutput.getValue());
            }

            // See if we can shrink the change/redeposit value to increase the fee
            final Coin bandwidthFee = GATx.getTxFee(mService, tx, mService.getMinFeeRate());
            Coin fee = GATx.getTxFee(mService, tx, mChosenFeeRate);
            fee = fee.isLessThan(oldFee) ? oldFee.add(Coin.SATOSHI) : fee;

            final Coin feeIncrement = fee.subtract(oldFee).add(bandwidthFee);
            fee = fee.add(bandwidthFee);
            final Coin remainingChange = changeOutput.mOutput.getValue().subtract(feeIncrement);
            if (remainingChange.isGreaterThan(mService.getDustThreshold())) {
                // We have enough change to cover the fee increase
                changeOutput.mOutput.setValue(remainingChange);
                final Coin newFee = fee;
                CB.after(Futures.immediateFuture((Void) null),
                         new CB.Toast<Void>(this, mBumpFeeButton) {
                    @Override
                    public void onSuccess(final Void dummy) {
                        final PreparedTransaction ptx;
                        ptx = GATx.signTransaction(mService, tx, usedUtxos, subAccount, changeOutput);
                        onTransactionConstructed(tx, oldFee, newFee);
                    }
                });
                return;
            }
        }

        // We can't shrink the change output; add new inputs (and possibly
        // a new change output) in order to increase the fee.
        final int numConfs = 1; // FIXME: 6 if instant
        final boolean is2Of3 = mService.findSubaccountByType(subAccount, "2of3") != null;
        final boolean minimizeInputs = is2Of3;
        final boolean filterAsset = true;
        final boolean sendAll = false;
        final JSONMap privateData = new JSONMap(); // FIXME: Populate
        final Coin outputAmount = amount;

        CB.after(mService.getAllUnspentOutputs(numConfs, subAccount, filterAsset),
                 new CB.Toast<List<JSONMap>>(this, mBumpFeeButton) {
            @Override
            public void onSuccess(final List<JSONMap> utxos) {
                Pair<Integer, JSONMap> ret = createFailed(R.string.insufficientFundsText);
                if (!utxos.isEmpty()) {
                    GATx.sortUtxos(utxos, minimizeInputs);
                    ret = createRawTransaction(mService, tx, usedUtxos, utxos, subAccount,
                                               changeOutput, outputAmount, oldFee, mChosenFeeRate,
                                               privateData, sendAll);
                    /*
                     * FIXME: Attempt to replace smallest used utxo with the larget fee one?
                     * FIXME: Implement
                    if (ret == R.string.insufficientFundsText && !minimizeInputs && uxtos.size() > 1) {
                        // Not enough money using nlocktime outputs first:
                        // Try again using the largest values first
                        GATx.sortUtxos(utxos, true);
                        ret = createRawTransaction(utxos, recipient, amount, privateData, sendAll);
                    }
                    */
                    if (ret.first == 0) {
                        final PreparedTransaction ptx;
                        ptx = GATx.signTransaction(mService, tx, usedUtxos, subAccount, changeOutput);
                        onTransactionConstructed(tx, oldFee, ret.second.getCoin("fee"));
                    }
                }
                if (ret.first != 0)
                    toast(ret.first, mBumpFeeButton);
            }
        });
    }

    private static Pair<Integer, JSONMap> createFailed(final int ret) {
        return new Pair<>(ret, null);
    }

    private static Pair<Integer, JSONMap>
    createRawTransaction(final GaService service,
                         final Transaction tx, final List<JSONMap> usedUtxos,
                         final List<JSONMap> utxos, final int subAccount,
                         GATx.ChangeOutput changeOutput,
                         final Coin amount, final Coin oldFee,
                         final Coin feeRate,
                         final JSONMap privateData, final boolean sendAll) {

        final boolean isRBF = usedUtxos != null;
        final boolean haveExistingChange = changeOutput != null;
        Coin total =  isRBF ? getUtxoSum(usedUtxos) : Coin.ZERO;
        Coin fee;

        // First add inputs until we cover the amount to send
        while ((sendAll || total.isLessThan(amount)) && !utxos.isEmpty())
            total = total.add(GATx.addUtxo(service, tx, utxos, usedUtxos));

        // Then add inputs until we cover amount + fee/change
        while (true) {
            fee = GATx.getTxFee(service, tx, feeRate);
            if (isRBF) {
                final Coin bandwidthFee = GATx.getTxFee(service, tx, service.getMinFeeRate());
                fee = (fee.isLessThan(oldFee) ? oldFee : fee).add(bandwidthFee);
            }

            final Coin minChange = changeOutput == null ? Coin.ZERO : service.getDustThreshold();
            final int cmp = sendAll ? 0 : total.compareTo(amount.add(fee).add(minChange));
            if (cmp < 0) {
                // Need more inputs to cover amount + fee/change
                if (utxos.isEmpty())
                    return createFailed(R.string.insufficientFundsText); // None left, fail

                total = total.add(GATx.addUtxo(service, tx, utxos, usedUtxos));
                continue;
            }

            if (cmp == 0 || changeOutput != null) {
                // Inputs exactly match amount + fee/change, or are greater
                // and we have a change output for the excess
                break;
            }

            // Inputs greater than amount + fee, add a change output and try again
            changeOutput = GATx.addChangeOutput(service, tx, subAccount);
            if (changeOutput == null)
                return createFailed(R.string.unable_to_create_change);
        }

        boolean randomizedChange = false;
        if (changeOutput != null) {
            // Set the value of the change output
            if (tx.getOutputs().size() == 1)
                changeOutput.mOutput.setValue(total.subtract(fee)); // Redeposit
            else
                changeOutput.mOutput.setValue(total.subtract(amount).subtract(fee));
            if (haveExistingChange)
                randomizedChange = changeOutput.mOutput == tx.getOutput(0);
            else
                randomizedChange = GATx.randomizeChangeOutput(tx);
        }

        final Coin actualAmount;
        if (!sendAll)
            actualAmount = amount;
        else {
            actualAmount = total.subtract(fee);
            if (!actualAmount.isGreaterThan(Coin.ZERO))
                return createFailed(R.string.insufficientFundsText);
            final int amtIndex = tx.getOutputs().size() == 1 ? 0 : (randomizedChange ? 1 : 0);
            tx.getOutput(amtIndex).setValue(actualAmount);
        }

        tx.setLockTime(service.getCurrentBlock()); // Prevent fee sniping

        int changeIndex = -1;
        if (changeOutput != null && tx.getOutputs().size() != 1)
            changeIndex = randomizedChange ? 0 : 1;
        return new Pair<>(0,
                          GATx.makeLimitsData(fee.subtract(oldFee), fee, changeIndex));
    }

    private void onTransactionConstructed(final Transaction tx, final Coin oldFee, final Coin newFee) {
        final Coin feeDelta = newFee.subtract(oldFee);
        final boolean skipChoice = mService.isUnderLimit(feeDelta);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTwoFactor = UI.popupTwoFactorChoice(TransactionActivity.this, mService, skipChoice,
                                                     new CB.Runnable1T<String>() {
                    public void run(String method) {
                        if (skipChoice && mService.hasAnyTwoFactor())
                            method = "limit";
                        showIncreaseSummary(method, oldFee, newFee, tx);
                    }
                });
                if (mTwoFactor != null)
                    mTwoFactor.show();
            }
        });
    }

    private void showIncreaseSummary(final String method, final Coin oldFee, final Coin newFee,
                                     final Transaction signedTx) {
        Log.i(TAG, "showIncreaseSummary( params " + method + ' ' + oldFee + ' ' + newFee + ')');

        final long feeDeltaSatoshi = newFee.subtract(oldFee).longValue();
        final Map<String, Object> twoFacData;
        if (method == null) {
            twoFacData = null;
        } else if (method.equals("limit")) {
            twoFacData = new HashMap<>();
            twoFacData.put("try_under_limits_bump", feeDeltaSatoshi);
        } else {
            twoFacData = new HashMap<>();
            twoFacData.put("method", method);
            twoFacData.put("bump_fee_amount", feeDeltaSatoshi);
            if (!method.equals("gauth")) {
                final Map<String, Long> amount = new HashMap<>();
                amount.put("amount", feeDeltaSatoshi);
                mService.requestTwoFacCode(method, "bump_fee", amount);
            }
        }

        final View v = getLayoutInflater().inflate(R.layout.dialog_new_transaction, null, false);

        final TextView amountLabel = UI.find(v, R.id.newTxAmountLabel);
        amountLabel.setText(R.string.newFeeText);
        final TextView feeLabel = UI.find(v, R.id.newTxFeeLabel);
        feeLabel.setText(R.string.oldFeeText);

        UI.hide(UI.find(v, R.id.newTxRecipientLabel), UI.find(v, R.id.newTxRecipientText));

        final Button showFiatBtcButton = UI.find(v, R.id.newTxShowFiatBtcButton);
        final TextView recipientText = UI.find(v, R.id.newTxRecipientText);
        final EditText newTx2FACodeText = UI.find(v, R.id.newTx2FACodeText);
        final String fiatNewFee = mService.coinToFiat(newFee);
        final String fiatOldFee = mService.coinToFiat(oldFee);
        final String fiatCurrency = mService.getFiatCurrency();

        mSummaryInBtc[0] = true;
        UI.setCoinText(mService, v, R.id.newTxAmountUnitText, R.id.newTxAmountText, newFee);
        UI.setCoinText(mService, v, R.id.newTxFeeUnit, R.id.newTxFeeText, oldFee);

        if (!GaService.IS_ELEMENTS) {
            showFiatBtcButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View btn) {
                    // Toggle display between fiat and BTC
                    if (mSummaryInBtc[0]) {
                        AmountFields.changeFiatIcon((FontAwesomeTextView) UI.find(v, R.id.newTxAmountUnitText), fiatCurrency);
                        AmountFields.changeFiatIcon((FontAwesomeTextView) UI.find(v, R.id.newTxFeeUnit), fiatCurrency);
                        UI.setAmountText((TextView) UI.find(v, R.id.newTxAmountText), fiatNewFee);
                        UI.setAmountText((TextView) UI.find(v, R.id.newTxFeeText), fiatOldFee);
                    } else {
                        UI.setCoinText(mService, v, R.id.newTxAmountUnitText, R.id.newTxAmountText, newFee);
                        UI.setCoinText(mService, v, R.id.newTxFeeUnit, R.id.newTxFeeText, oldFee);
                    }
                    mSummaryInBtc[0] = !mSummaryInBtc[0];
                    showFiatBtcButton.setText(mSummaryInBtc[0] ? R.string.show_fiat : R.string.show_btc);
                }
            });
        }

        if (method != null && !method.equals("limit")) {
            final TextView twoFAText = UI.find(v, R.id.newTx2FATypeText);
            twoFAText.setText(String.format("2FA %s code", method));
            UI.show(twoFAText, newTx2FACodeText);
        }

        mTwoFactorAttemptsRemaining = 3;
        mSummary = UI.popup(this, R.string.feeIncreaseTitle, R.string.send, R.string.cancel)
                .customView(v, true)
                .autoDismiss(false)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final MaterialDialog dialog, final DialogAction which) {
                        UI.dismiss(null, TransactionActivity.this.mSummary);
                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final MaterialDialog dialog, final DialogAction which) {
                        if (twoFacData != null && twoFacData.containsKey("method"))
                            twoFacData.put("code", UI.getText(newTx2FACodeText));
                        Futures.addCallback(mService.sendRawTransaction(signedTx, twoFacData, null),
                                            new CB.Toast<String>(TransactionActivity.this) {
                            @Override
                            public void onSuccess(final String dummy) {
                                // FIXME: Add notification with "Transaction sent"?
                                UI.dismiss(TransactionActivity.this, TransactionActivity.this.mSummary);
                                finishOnUiThread();
                            }

                            @Override
                            public void onFailure(final Throwable t) {
                                final TransactionActivity activity = TransactionActivity.this;
                                if (t instanceof GAException) {
                                    final GAException e = (GAException) t;
                                    if (e.mUri.equals(GAException.AUTH)) {
                                        final int n = --activity.mTwoFactorAttemptsRemaining;
                                        if (n > 0) {
                                            final Resources r = activity.getResources();
                                            final String msg = r.getQuantityString(R.plurals.attempts_remaining, n, n);
                                            UI.toast(activity, e.mMessage + "\n(" + msg + ')', mBumpFeeButton);
                                            return; // Allow re-trying
                                        }
                                    }
                                }
                                UI.toast(activity, t, mBumpFeeButton);
                                // Out of 2FA attempts, or another exception; give up
                                UI.dismiss(activity, activity.mSummary);
                            }
                        }, mService.getExecutor());
                    }
                }).build();
        UI.mapEnterToPositive(mSummary, R.id.newTx2FACodeText);
        mSummary.show();
    }
}
