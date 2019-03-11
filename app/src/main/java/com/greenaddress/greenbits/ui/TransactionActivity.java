package com.greenaddress.greenbits.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.JSONMap;
import com.greenaddress.greenapi.data.BumpTxData;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import static com.greenaddress.greenbits.ui.ScanActivity.INTENT_STRING_TX;


public class TransactionActivity extends LoggedActivity implements View.OnClickListener {

    private static final String TAG = TransactionActivity.class.getSimpleName();
    private static final int FEE_BLOCK_NUMBERS[] = {1, 3, 6};

    private Menu mMenu;
    private TextView mEstimatedBlocks;
    private TextView mMemoTitle;
    private TextView mMemoSave;
    private TextView mMemoText;
    private TextView mUnconfirmedText;
    private TextView mStatusIncreaseFee;
    private Button mExplorerButton;
    private Dialog mSummary;
    private Dialog mTwoFactor;
    private ImageView mStatusIcon;

    private TransactionItem mTxItem;

    @Override
    protected int getMainViewId() { return R.layout.activity_transaction; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        setResult(RESULT_OK);
        UI.preventScreenshots(this);

        setTitleBackTransparent();

        mMemoTitle = UI.find(this, R.id.txMemoTitle);
        mMemoSave = UI.find(this, R.id.txMemoSave);
        mMemoText = UI.find(this, R.id.txMemoText);
        mEstimatedBlocks = UI.find(this, R.id.txUnconfirmedEstimatedBlocks);
        mExplorerButton = UI.find(this, R.id.txExplorer);
        mUnconfirmedText = UI.find(this, R.id.txUnconfirmedText);
        mStatusIncreaseFee = UI.find(this, R.id.status_increase_fee);
        mStatusIcon = UI.find(this, R.id.status_icon);

        final TextView doubleSpentByText = UI.find(this, R.id.txDoubleSpentByText);
        final TextView doubleSpentByTitle = UI.find(this, R.id.txDoubleSpentByTitle);

        mTxItem = (TransactionItem) getIntent().getSerializableExtra("TRANSACTION");
        final boolean isWatchOnly = mService.isWatchOnly();

        // Set txid
        final TextView hashText = UI.find(this, R.id.txHashText);
        hashText.setText(mTxItem.txHash.toString());

        // Set explorer button
        final String blockExplorerTx = mService.getNetwork().getTxExplorerUrl();
        openInBrowser(mExplorerButton, mTxItem.txHash.toString(), blockExplorerTx, null);

        // Set title: incoming, outgoing, redeposited
        final String title;
        if (mService.isElements())
            title = "";
        else if (mTxItem.type == TransactionItem.TYPE.OUT)
            title = getString(R.string.id_outgoing);
        else if (mTxItem.type == TransactionItem.TYPE.REDEPOSIT)
            title = getString(R.string.id_redeposited);
        else
            title = getString(R.string.id_received_on);
        setTitle(title);


        final String confirmations;
        final int confirmationsColor;
        if (mTxItem.getConfirmations() == 0) {
            confirmations = getString(R.string.id_unconfirmed);
            confirmationsColor = R.color.red;
            mStatusIcon.setVisibility(View.GONE);
        } else if (!mTxItem.hasEnoughConfirmations()) {
            confirmations = getString(R.string.id_d6_confirmations, mTxItem.getConfirmations());
            confirmationsColor = R.color.grey_light;
            mStatusIcon.setVisibility(View.GONE);
        } else {
            confirmations = getString(R.string.id_completed);
            confirmationsColor = R.color.green;
            mStatusIcon.setVisibility(View.VISIBLE);
        }
        mUnconfirmedText.setText(confirmations);
        mUnconfirmedText.setTextColor(getResources().getColor(confirmationsColor));

        // Set amount
        boolean negative = mTxItem.amount < 0;
        final ObjectNode amount = mService.getSession().convertSatoshi(negative ? -mTxItem.amount : mTxItem.amount);
        final String btc = mService.getValueString(amount, false, true);
        final String fiat = mService.getValueString(amount, true, true);
        final String neg = negative ? "-" : "";
        final TextView amountText = UI.find(this, R.id.txAmountText);
        amountText.setText(String.format("%s%s / %s%s", neg, btc, neg, fiat));

        // Set date/time
        final TextView dateText = UI.find(this, R.id.txDateText);
        final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm, MMM dd, yyyy", Locale.US);
        dateText.setText(timeFormat.format(mTxItem.date));

        // Set fees
        showFeeInfo(mTxItem.fee, mTxItem.vSize, mTxItem.feeRate);
        UI.hide(mEstimatedBlocks);
        UI.hide(mStatusIncreaseFee);
        if (mTxItem.type == TransactionItem.TYPE.OUT || mTxItem.type == TransactionItem.TYPE.REDEPOSIT ||
            mTxItem.isSpent) {
            if (mTxItem.getConfirmations() == 0)
                showUnconfirmed();
        }
        // FIXME: use a list instead of reusing a TextView to show all double spends to allow
        // for a warning to be shown before the browser is open
        // this is to prevent to accidentally leak to block explorers your addresses
        if (mTxItem.doubleSpentBy != null || !mTxItem.replacedHashes.isEmpty()) {
            CharSequence res = "";
            if (mTxItem.doubleSpentBy != null) {
                if (mTxItem.doubleSpentBy.equals("malleability") || mTxItem.doubleSpentBy.equals("update"))
                    res = mTxItem.doubleSpentBy;
                else
                    res = Html.fromHtml(
                        "<a href=\"" + blockExplorerTx + mTxItem.doubleSpentBy + "\">" + mTxItem.doubleSpentBy +
                        "</a>");
                if (!mTxItem.replacedHashes.isEmpty())
                    res = TextUtils.concat(res, "; ");
            }
            if (!mTxItem.replacedHashes.isEmpty()) {
                res = TextUtils.concat(res, Html.fromHtml("replaces transactions:<br/>"));
                for (int i = 0; i < mTxItem.replacedHashes.size(); ++i) {
                    if (i > 0)
                        res = TextUtils.concat(res, Html.fromHtml("<br/>"));
                    final String txHashHex = mTxItem.replacedHashes.get(i).toString();
                    final String link = "<a href=\"" + blockExplorerTx + txHashHex + "\">" + txHashHex + "</a>";
                    res = TextUtils.concat(res, Html.fromHtml(link));
                }
            }
            doubleSpentByText.setText(res);
        }
        final boolean showDoubleSpent = mTxItem.doubleSpentBy != null || !mTxItem.replacedHashes.isEmpty();
        UI.showIf(showDoubleSpent, doubleSpentByText);
        UI.showIf(showDoubleSpent, doubleSpentByTitle);

        // Set recipient / received on
        final TextView receivedOnText = UI.find(this, R.id.txReceivedOnText);
        final TextView receivedOnTitle = UI.find(this, R.id.txReceivedOnTitle);
        final TextView recipientText = UI.find(this, R.id.txRecipientText);
        final TextView recipientTitle = UI.find(this, R.id.txRecipientTitle);
        if (!TextUtils.isEmpty(mTxItem.counterparty)) {
            recipientText.setText(mTxItem.counterparty);
            UI.hide(receivedOnText);
            UI.hide(receivedOnTitle);
        }

        final String name = mService.getModel().getSubaccountDataObservable().
                getSubaccountDataWithPointer(mTxItem.subaccount).getNameWithDefault(getString(R.string.id_main_account));
        receivedOnText.setText(name);

        UI.hideIf(mTxItem.type == TransactionItem.TYPE.REDEPOSIT, UI.find(this, R.id.txRecipientReceiverView));
        UI.hideIf(mTxItem.type == TransactionItem.TYPE.IN, recipientText);
        UI.hideIf(mTxItem.type == TransactionItem.TYPE.IN, recipientTitle);

        // Memo
        if (!TextUtils.isEmpty(mTxItem.memo)) {
            mMemoText.setText(mTxItem.memo);
        }

        if (!isWatchOnly) {
            mMemoSave.setOnClickListener(this);
            mMemoText.addTextChangedListener(new TextWatcher() {

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                        mMemoSave.setVisibility(View.VISIBLE);
                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                public void afterTextChanged(Editable s) { }
            });
        } else {
            mMemoText.setTextIsSelectable(true);
            mMemoText.setKeyListener(null);
        }

        // The following are needed to effectively loose focus and cursor from mMemoText
        mMemoTitle.setFocusable(true);
        mMemoTitle.setFocusableInTouchMode(true);

        // SPV
        final boolean spvVerified = mTxItem.spvVerified || mTxItem.isSpent ||
                mTxItem.type == TransactionItem.TYPE.OUT ||
                !mService.isSPVEnabled();

        if (!spvVerified) {
            mStatusIncreaseFee.setVisibility(View.VISIBLE);
            mStatusIncreaseFee.setText(String.format("⚠️ %s", getString(R.string.id_spv_unverified)));
            mStatusIncreaseFee.setTextColor(getResources().getColor(R.color.red));
            mStatusIcon.setVisibility(View.GONE);
        }

    }

    private void showFeeInfo(final long fee, final long vSize, final long feeRate) {

        final TextView feeText = UI.find(this, R.id.txFeeInfoText);
        final String btcFee = mService.getValueString(mService.getSession().convertSatoshi(fee), false, true);
        feeText.setText(String.format("%s, %s vbytes, %s", btcFee,
                                      String.valueOf(vSize), UI.getFeeRateString(feeRate)));
    }

    private void showUnconfirmed() {
        final List<Long> estimates = mService.getFeeEstimates();
        int block = 1;
        while (block < estimates.size()) {
            if (mTxItem.feeRate >= estimates.get(block)) {
                break;
            }
            ++block;
        }

        UI.show(mEstimatedBlocks);
        mEstimatedBlocks.setText(getString(R.string.id_estimated_blocks_until, block));

        if (mService.isWatchOnly() || mService.isElements() || !mTxItem.replaceable)
            return; // FIXME: Implement RBF for elements

        // Allow RBF if it might decrease the number of blocks until confirmation
        final boolean allowRbf = block > 1 || mService.getNetwork().alwaysAllowRBF();

        UI.show(mStatusIncreaseFee);
        mStatusIncreaseFee.setOnClickListener(this);
        mStatusIcon.setVisibility(View.GONE);
    }

    @Override
    public void onPauseWithService() {
        mSummary = UI.dismiss(this, mSummary);
        mTwoFactor = UI.dismiss(this, mTwoFactor);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UI.unmapClick(mMemoText);
        UI.unmapClick(mMemoSave);
        UI.unmapClick(mStatusIncreaseFee);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_transaction, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public void onClick(final View v) {
        if (v == mMemoSave)
            onMemoSaveClicked();
        else if (v == mStatusIncreaseFee)
            onBumpFeeButtonClicked();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_share:
            final Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                                mService.getNetwork().getTxExplorerUrl() + mTxItem.txHash.toString());
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
            return true;
        case android.R.id.home:
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void onFinishedSavingMemo() {
        runOnUiThread(() -> {
            mMemoSave.setVisibility(View.GONE);
            hideKeyboardFrom(mMemoText);
            mMemoTitle.requestFocus();
        });
        // Force reload tx
        mService.getModel().getTransactionDataObservable(mTxItem.subaccount).refresh();
    }

    private void onMemoSaveClicked() {
        final String newMemo = UI.getText(mMemoText);
        if (newMemo.equals(mTxItem.memo)) {
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

    private void openInBrowser(final Button button, final String identifier, final String url,
                               final JSONMap confidentialData) {
        button.setOnClickListener(v -> {
            if (TextUtils.isEmpty(url))
                return;

            String domain = url;
            try {
                domain = new URI(url).getHost();
            } catch (final URISyntaxException e) {
                e.printStackTrace();
            }

            if (TextUtils.isEmpty(domain))
                return;

            final String stripped = domain.startsWith("www.") ? domain.substring(4) : domain;
            final Uri uri = Uri.parse(TextUtils.concat(url, identifier).toString());
            final boolean dontAskAgain = mService.cfg().getBoolean(PrefKeys.DONT_ASK_AGAIN_TO_OPEN_URL, false);
            if (dontAskAgain) {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } else {
                new MaterialDialog.Builder(this)
                        .checkBoxPromptRes(R.string.id_dont_ask_me_again, false,
                                (buttonView, isChecked) -> mService.cfgEdit().putBoolean(PrefKeys.DONT_ASK_AGAIN_TO_OPEN_URL, isChecked).apply())
                        .content(getString(R.string.id_are_you_sure_you_want_to_view, stripped))
                        .backgroundColor(getResources().getColor(R.color.buttonJungleGreen))
                        .positiveText(android.R.string.ok)
                        .negativeText(android.R.string.cancel)
                        .cancelable(false)
                        .onNegative((dialog, which) -> mService.cfgEdit().putBoolean(PrefKeys.DONT_ASK_AGAIN_TO_OPEN_URL, false).apply())
                        .onPositive((dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, uri)))
                        .build().show();
            }
        });
    }

    private void onBumpFeeButtonClicked() {
        Log.d(TAG,"onBumpFeeButtonClicked");
        try {
            startLoading();
            final GDKSession session = mService.getSession();
            final Model model = mService.getModel();
            final String txhash = mTxItem.txHash.toString();
            final int subaccount = mTxItem.subaccount == null ? model.getCurrentSubaccount() : mTxItem.subaccount;
            final JsonNode txToBump = session.getTransactionRaw(subaccount, txhash);
            final JsonNode feeRate = txToBump.get("fee_rate");
            BumpTxData bumpTxData = new BumpTxData();
            bumpTxData.setPreviousTransaction(txToBump);
            bumpTxData.setFeeRate(feeRate.asLong());
            bumpTxData.setSubaccount(subaccount);
            Log.d(TAG,"createTransactionRaw(" + bumpTxData.toString() + ")");
            final ObjectNode tx = session.createTransactionRaw(bumpTxData);
            final Intent intent = new Intent(this, SendActivity.class);
            intent.putExtra(INTENT_STRING_TX, tx.toString());
            startActivity(intent);
            finish();
        } catch (Exception e) {
            UI.toast(this,e.getMessage(), Toast.LENGTH_LONG);
            Log.e(TAG,e.getMessage());
            e.printStackTrace();
        }
    }

}
