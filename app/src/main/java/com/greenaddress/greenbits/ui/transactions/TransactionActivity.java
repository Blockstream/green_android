package com.greenaddress.greenbits.ui.transactions;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.gdk.GDKTwoFactorCall;
import com.greenaddress.greenapi.JSONMap;
import com.greenaddress.greenapi.data.AssetInfoData;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.BumpTxData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.TransactionData;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.assets.AssetActivity;
import com.greenaddress.greenbits.ui.assets.AssetsAdapter;
import com.greenaddress.greenbits.ui.components.CharInputFilter;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;
import com.greenaddress.greenbits.ui.send.SendAmountActivity;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.Map;

import static com.greenaddress.gdk.GDKSession.getSession;


public class TransactionActivity extends LoggedActivity implements View.OnClickListener,
    AssetsAdapter.OnAssetSelected  {

    private static final String TAG = TransactionActivity.class.getSimpleName();

    private TextView mMemoTitle;
    private TextView mMemoSave;
    private TextView mMemoText;
    private TextView mUnconfirmedText;
    private TextView mStatusIncreaseFee;
    private TextView mStatusSPVUnverified;
    private Button mExplorerButton;
    private Dialog mSummary;
    private Dialog mTwoFactor;
    private ImageView mStatusIcon;

    private TransactionData mTxItem;
    private Map<String, Long> mAssetsBalances;

    @Override
    protected int getMainViewId() { return R.layout.activity_transaction; }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (modelIsNullOrDisconnected())
            return;

        setResult(RESULT_OK);
        UI.preventScreenshots(this);

        setTitleBackTransparent();

        mMemoTitle = UI.find(this, R.id.txMemoTitle);
        mMemoSave = UI.find(this, R.id.txMemoSave);
        mMemoText = UI.find(this, R.id.txMemoText);
        mExplorerButton = UI.find(this, R.id.txExplorer);
        mUnconfirmedText = UI.find(this, R.id.txUnconfirmedText);
        mStatusIncreaseFee = UI.find(this, R.id.status_increase_fee);
        mStatusSPVUnverified = UI.find(this, R.id.status_spv_unverified);
        mStatusIcon = UI.find(this, R.id.status_icon);

        mTxItem = (TransactionData) getIntent().getSerializableExtra("TRANSACTION");
        final boolean isWatchOnly = getConnectionManager().isWatchOnly();

        mAssetsBalances = getModel().getCurrentAccountBalanceData();

        // Set txid
        final TextView hashText = UI.find(this, R.id.txHashText);
        hashText.setText(mTxItem.getTxhash());

        // Set explorer button
        final NetworkData networkData = getGAApp().getCurrentNetworkData();
        final String blockExplorerTx = networkData.getTxExplorerUrl();
        openInBrowser(mExplorerButton, mTxItem.getTxhash(), blockExplorerTx, null);

        // Set title: incoming, outgoing, redeposited
        final String title;
        if (mTxItem.getTxType() == TransactionData.TYPE.OUT)
            title = getString(R.string.id_sent);
        else if (mTxItem.getTxType() == TransactionData.TYPE.REDEPOSIT)
            title = getString(R.string.id_redeposited);
        else
            title = getString(R.string.id_received);
        setTitle(title);

        final String confirmations;
        final int confirmationsColor;
        final int currentBlock = getModel().getBlockchainHeightObservable().getHeight();
        mStatusIcon.setVisibility(View.GONE);
        if (mTxItem.getConfirmations(currentBlock) == 0) {
            confirmations = getString(R.string.id_unconfirmed);
            confirmationsColor = R.color.red;
        } else if (networkData.getLiquid() && mTxItem.getConfirmations(currentBlock) < 2) {
            confirmations = getString(R.string.id_12_confirmations);
            confirmationsColor = R.color.grey_light;
        } else if (!networkData.getLiquid() && !mTxItem.hasEnoughConfirmations(currentBlock)) {
            confirmations = getString(R.string.id_d6_confirmations, mTxItem.getConfirmations(currentBlock));
            confirmationsColor = R.color.grey_light;
        } else {
            confirmations = getString(R.string.id_completed);
            confirmationsColor = networkData.getLiquid() ? R.color.liquidDark : R.color.green;
            mStatusIcon.setVisibility(View.VISIBLE);
        }
        mUnconfirmedText.setText(confirmations);
        mUnconfirmedText.setTextColor(getResources().getColor(confirmationsColor));

        // Set amount
        final boolean negative = mTxItem.getTxType() != TransactionData.TYPE.IN;
        final String neg = negative ? "-" : "";
        final TextView amountText = UI.find(this, R.id.txAmountText);

        try {
            final BalanceData balance = getSession().convertBalance(mTxItem.getSatoshi().get("btc"));
            final String btc = getModel().getBtc(balance, true);
            final String fiat = getModel().getFiat(balance, true);
            amountText.setText(String.format("%s%s / %s%s", neg, btc, neg, fiat));
        } catch (final Exception e) {
            e.printStackTrace();
        }

        if (networkData.getLiquid()) {
            amountText.setVisibility(View.GONE);
            final RecyclerView assetsList = findViewById(R.id.assetsList);
            assetsList.setLayoutManager(new LinearLayoutManager(this));
            final AssetsAdapter adapter = new AssetsAdapter(mTxItem.getSatoshi(),
                                                            getNetwork(),this, getModel());
            assetsList.setAdapter(adapter);
            assetsList.setVisibility(View.VISIBLE);
        }

        // Set date/time
        final TextView dateText = UI.find(this, R.id.txDateText);
        final String date = mTxItem.getLocalizedDate(DateFormat.LONG);
        final String time = mTxItem.getLocalizedTime(DateFormat.SHORT);
        dateText.setText(date + ", " + time);

        // Set fees
        showFeeInfo(mTxItem.getFee(), mTxItem.getTransactionVsize(), mTxItem.getFeeRate());
        UI.hide(mStatusIncreaseFee);
        if (mTxItem.getTxType() == TransactionData.TYPE.OUT || mTxItem.getTxType() == TransactionData.TYPE.REDEPOSIT ||
            mTxItem.isSpent()) {
            if (mTxItem.getConfirmations(currentBlock) == 0)
                showUnconfirmed();
        }

        // Set recipient / received on
        final TextView recipientText = UI.find(this, R.id.txRecipientText);
        final TextView recipientTitle = UI.find(this, R.id.txRecipientTitle);
        if (!TextUtils.isEmpty(mTxItem.getAddressee())) {
            recipientText.setText(mTxItem.getAddressee());
        }

        UI.hideIf(mTxItem.getTxType() == TransactionData.TYPE.REDEPOSIT, UI.find(this, R.id.txRecipientReceiverView));
        UI.hideIf(mTxItem.getTxType() == TransactionData.TYPE.REDEPOSIT, UI.find(this, R.id.amountView));
        UI.hideIf(mTxItem.getTxType() == TransactionData.TYPE.IN, recipientText);
        UI.hideIf(mTxItem.getTxType() == TransactionData.TYPE.IN, recipientTitle);

        // Memo
        CharInputFilter.setIfNecessary(mMemoText);
        if (!TextUtils.isEmpty(mTxItem.getMemo())) {
            mMemoText.setText(mTxItem.getMemo());
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
        final SharedPreferences preferences = getSharedPreferences(getNetwork().getNetwork(), MODE_PRIVATE);
        final boolean isSpvEnabled = preferences.getBoolean(PrefKeys.SPV_ENABLED, false);
        final boolean isSpvVerified = mTxItem.isSpent() ||
                                      mTxItem.getTxType() == TransactionData.TYPE.OUT ||
                                      !isSpvEnabled || (isSpvEnabled && getGAApp().getSpv().isSPVVerified(
                                                            mTxItem.getTxhash()));

        if (!isSpvVerified) {
            mStatusSPVUnverified.setVisibility(View.VISIBLE);
            mStatusSPVUnverified.setText(String.format("⚠️ %s", getString(R.string.id_spv_unverified)));
        } else {
            mStatusSPVUnverified.setVisibility(View.GONE);
        }

    }

    private void showFeeInfo(final long fee, final long vSize, final long feeRate) {
        final TextView feeText = UI.find(this, R.id.txFeeInfoText);
        try {
            final String btcFee = getModel().getBtc(fee, true);
            feeText.setText(String.format("%s (%s)", btcFee, UI.getFeeRateString(feeRate)));
        } catch (final Exception e) {
            Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
        }
    }

    private void showUnconfirmed() {
        final NetworkData networkData = getGAApp().getCurrentNetworkData();

        if (getConnectionManager().isWatchOnly() || networkData.getLiquid() || getModel().isTwoFAReset())
            return; // FIXME: Implement RBF for elements

        if (!networkData.getLiquid()) {
            UI.show(mStatusIncreaseFee);
            mStatusIncreaseFee.setOnClickListener(this);
        }
        mStatusIcon.setVisibility(View.GONE);
    }

    @Override
    public void onPause() {
        super.onPause();
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
            final NetworkData networkData = getGAApp().getCurrentNetworkData();
            final Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                                networkData.getTxExplorerUrl() + mTxItem.getTxhash());
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
        getModel().getTransactionDataObservable(mTxItem.getSubaccount()).refresh();
    }

    private void onMemoSaveClicked() {
        final String newMemo = UI.getText(mMemoText);
        if (newMemo.equals(mTxItem.getMemo())) {
            onFinishedSavingMemo();
            return;
        }
        getGAApp().getExecutor().submit(() -> {
            try {
                final boolean res = getSession().changeMemo(mTxItem.getTxhash(), newMemo);
                if (res) {
                    onFinishedSavingMemo();
                } else {
                    runOnUiThread(() -> { UI.toast(this, R.string.id_operation_failure, Toast.LENGTH_LONG); });
                }
            } catch (Exception e) {
                e.printStackTrace();
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
            final boolean dontAskAgain = cfg().getBoolean(PrefKeys.DONT_ASK_AGAIN_TO_OPEN_URL, false);
            if (dontAskAgain) {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } else {
                new MaterialDialog.Builder(this)
                .checkBoxPromptRes(R.string.id_dont_ask_me_again, false,
                                   (buttonView,
                                    isChecked) -> cfg().edit().putBoolean(PrefKeys.DONT_ASK_AGAIN_TO_OPEN_URL,
                                                                          isChecked).apply())
                .content(getString(R.string.id_are_you_sure_you_want_to_view, stripped))
                .backgroundColor(getResources().getColor(R.color.buttonJungleGreen))
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .cancelable(false)
                .onNegative((dialog, which) -> cfg().edit().putBoolean(PrefKeys.DONT_ASK_AGAIN_TO_OPEN_URL,
                                                                       false).apply())
                .onPositive((dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, uri)))
                .build().show();
            }
        });
    }

    private void onBumpFeeButtonClicked() {
        Log.d(TAG,"onBumpFeeButtonClicked");
        try {
            startLoading();
            final Model model = getModel();
            final String txhash = mTxItem.getTxhash();
            final int subaccount = mTxItem.getSubaccount() ==
                                   null ? model.getCurrentSubaccount() : mTxItem.getSubaccount();
            final GDKTwoFactorCall call = getSession().getTransactionsRaw(subaccount, 0, 30);
            ObjectNode txListObject = call.resolve(null, getConnectionManager().getHWResolver());
            final JsonNode txToBump = getSession().findTransactionRaw((ArrayNode) txListObject.get(
                                                                          "transactions"), txhash);
            final JsonNode feeRate = txToBump.get("fee_rate");
            BumpTxData bumpTxData = new BumpTxData();
            bumpTxData.setPreviousTransaction(txToBump);
            bumpTxData.setFeeRate(feeRate.asLong());
            bumpTxData.setSubaccount(subaccount);
            Log.d(TAG,"createTransactionRaw(" + bumpTxData.toString() + ")");
            final GDKTwoFactorCall signCall = getSession().createTransactionRaw(null, bumpTxData);
            final ObjectNode tx = signCall.resolve(null, getConnectionManager().getHWResolver());
            final Intent intent = new Intent(this, SendAmountActivity.class);
            removeUtxosIfTooBig(tx);
            intent.putExtra(PrefKeys.INTENT_STRING_TX, tx.toString());
            stopLoading();
            startActivity(intent);
            finish();
        } catch (Exception e) {
            UI.toast(this,e.getMessage(), Toast.LENGTH_LONG);
            stopLoading();
            Log.e(TAG,e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onAssetSelected(final String assetId) {
        // Nothing for btc
        if ("btc".equals(assetId) || mAssetsBalances == null)
            return;

        // Open selected asset detail page
        final Intent intent = new Intent(this, AssetActivity.class);
        long satoshi = 0L;
        if (mAssetsBalances.containsKey(assetId)) {
            satoshi = mAssetsBalances.get(assetId);
        }
        final AssetInfoData info = getModel().getAssetsObservable().getAssetsInfos().get(assetId);
        intent.putExtra("ASSET_ID", assetId)
        .putExtra("ASSET_INFO", info)
        .putExtra("SATOSHI", satoshi);
        startActivity(intent);
    }
}
