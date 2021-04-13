package com.greenaddress.greenbits.ui.transactions;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.greenaddress.Bridge;
import com.greenaddress.greenapi.data.AssetInfoData;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.BumpTxData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.TransactionData;
import com.greenaddress.greenapi.model.Conversion;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.assets.AssetActivity;
import com.greenaddress.greenbits.ui.assets.AssetsAdapter;
import com.greenaddress.greenbits.ui.components.CharInputFilter;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;
import com.greenaddress.greenbits.ui.send.SendAmountActivity;
import com.greenaddress.greenbits.wallets.HardwareCodeResolver;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;


public class TransactionActivity extends LoggedActivity implements View.OnClickListener,
                                         AssetsAdapter.OnAssetSelected  {

    private static final String TAG = TransactionActivity.class.getSimpleName();

    private TextView mMemoTitle;
    private TextView mMemoSave;
    private TextView mMemoText;
    private TextView mUnconfirmedText;
    private TextView mStatusIncreaseFee;
    private TextView mStatusSPVUnverified;
    private Dialog mSummary;
    private Dialog mTwoFactor;
    private ImageView mStatusIcon;

    private TransactionData mTxItem;
    private NetworkData mNetworkData;
    private Map<String, Long> mAssetsBalances = new HashMap<String, Long>();

    private Disposable memoDisposable, bumpDisposable;


    @Override
    protected int getMainViewId() { return R.layout.activity_transaction; }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_OK);

        setTitleBackTransparent();

        mMemoTitle = UI.find(this, R.id.txMemoTitle);
        mMemoSave = UI.find(this, R.id.txMemoSave);
        mMemoText = UI.find(this, R.id.txMemoText);
        mUnconfirmedText = UI.find(this, R.id.txUnconfirmedText);
        mStatusIncreaseFee = UI.find(this, R.id.status_increase_fee);
        mStatusSPVUnverified = UI.find(this, R.id.status_spv_unverified);
        mStatusIcon = UI.find(this, R.id.status_icon);

        try {
            mTxItem = (TransactionData) getIntent().getSerializableExtra("TRANSACTION");
            mAssetsBalances = (Map<String, Long>) getIntent().getSerializableExtra("BALANCE");
        } catch (final Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
            finishOnUiThread();
            return;
        }
    }

    public void refresh() {

        mNetworkData = getSession().getNetworkData();
        final boolean isWatchOnly = getSession().isWatchOnly();

        // Set txid
        final TextView hashText = UI.find(this, R.id.txHashText);
        hashText.setText(mTxItem.getTxhash());

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
        final int currentBlock = getSession().getNotificationModel().getBlockHeight();
        mStatusIcon.setVisibility(View.GONE);
        if (mTxItem.getConfirmations(currentBlock) == 0) {
            confirmations = getString(R.string.id_unconfirmed);
            confirmationsColor = R.color.red;
        } else if (mNetworkData.getLiquid() && mTxItem.getConfirmations(currentBlock) < 2) {
            confirmations = getString(R.string.id_12_confirmations);
            confirmationsColor = R.color.grey_light;
        } else if (!mNetworkData.getLiquid() && !mTxItem.hasEnoughConfirmations(currentBlock)) {
            confirmations = getString(R.string.id_d6_confirmations, mTxItem.getConfirmations(currentBlock));
            confirmationsColor = R.color.grey_light;
        } else {
            confirmations = getString(R.string.id_completed);
            confirmationsColor = mNetworkData.getLiquid() ? R.color.liquidDark : R.color.green;
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
            final String btc = Conversion.getBtc(getSession(), balance, true);
            final String fiat = Conversion.getFiat(getSession(), balance, true);
            amountText.setText(String.format("%s%s / %s%s", neg, btc, neg, fiat));
        } catch (final Exception e) {
            e.printStackTrace();
        }

        if (mNetworkData.getLiquid()) {
            amountText.setVisibility(View.GONE);
            final RecyclerView assetsList = findViewById(R.id.assetsList);
            assetsList.setLayoutManager(new LinearLayoutManager(this));
            final AssetsAdapter adapter = new AssetsAdapter(this, mTxItem.getSatoshi(),
                                                            getNetwork(),this);
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
                                      !isSpvEnabled || (isSpvEnabled && Bridge.INSTANCE.getSpv().isSPVVerified(
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
            final String btcFee = Conversion.getBtc(getSession(), fee, true);
            feeText.setText(String.format("%s (%s)", btcFee, UI.getFeeRateString(feeRate)));
        } catch (final Exception e) {
            Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
        }
    }

    private void showUnconfirmed() {

        if (getSession().isWatchOnly() || mNetworkData.getLiquid() || getSession().isTwoFAReset())
            return; // FIXME: Implement RBF for elements

        if (mNetworkData.canReplaceTransactions()) {
            UI.show(mStatusIncreaseFee);
            mStatusIncreaseFee.setOnClickListener(this);
        }
        mStatusIcon.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isFinishing()) {
            refresh();
        }
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

        if (memoDisposable != null)
            memoDisposable.dispose();
        if (bumpDisposable != null)
            bumpDisposable.dispose();
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
        int itemId = item.getItemId();
        if (itemId == R.id.action_share) {
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction().addToBackStack(null);
            TransactionSharingFrament.createTransactionSharingFrament(getNetwork(), mTxItem).show(ft, "");
            return true;
        } else if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onFinishedSavingMemo() {
        runOnUiThread(() -> {
            mMemoSave.setVisibility(View.GONE);
            hideKeyboardFrom(mMemoText);
            mMemoTitle.requestFocus();
        });
    }

    private void onMemoSaveClicked() {
        final String newMemo = UI.getText(mMemoText);
        if (newMemo.equals(mTxItem.getMemo())) {
            onFinishedSavingMemo();
            return;
        }

        memoDisposable = Observable.just(getSession())
                         .observeOn(Schedulers.computation())
                         .map((session) -> {
            return session.changeMemo(mTxItem.getTxhash(), newMemo);
        })
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe((res) -> {
            if (res)
                onFinishedSavingMemo();
            else
                UI.toast(this, R.string.id_operation_failure, Toast.LENGTH_LONG);
        }, (e) -> {
            e.printStackTrace();
            UI.toast(this, R.string.id_operation_failure, Toast.LENGTH_LONG);
        });
    }

    private void onBumpFeeButtonClicked() {
        Log.d(TAG,"onBumpFeeButtonClicked");

        startLoading();
        final String txhash = mTxItem.getTxhash();
        final int subaccount = mTxItem.getSubaccount() ==
                               null ? getActiveAccount() : mTxItem.getSubaccount();

        bumpDisposable = Observable.just(getSession())
                         .observeOn(Schedulers.computation())
                         .map((session) -> {
            return session.getTransactionsRaw(subaccount, 0, 30).resolve(null, new HardwareCodeResolver(this));
        })
                         .map((txListObject) -> {
            return getSession().findTransactionRaw((ArrayNode) txListObject.get(
                                                       "transactions"), txhash);
        })
                         .map((txToBump) -> {
            final JsonNode feeRate = txToBump.get("fee_rate");
            BumpTxData bumpTxData = new BumpTxData();
            bumpTxData.setPreviousTransaction(txToBump);
            bumpTxData.setFeeRate(feeRate.asLong());
            bumpTxData.setSubaccount(subaccount);
            Log.d(TAG,"createTransactionRaw(" + bumpTxData.toString() + ")");
            return bumpTxData;
        })
                         .map((bumpTxData) -> {
            return getSession().createTransactionRaw(null, bumpTxData).resolve(null, new HardwareCodeResolver(this));
        })
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe((tx) -> {
            stopLoading();
            final Intent intent = new Intent(this, SendAmountActivity.class);
            removeUtxosIfTooBig(tx);
            intent.putExtra(PrefKeys.INTENT_STRING_TX, tx.toString());
            startActivity(intent);
            finish();
        }, (e) -> {
            e.printStackTrace();
            stopLoading();
            UI.toast(this, e.getMessage(), Toast.LENGTH_LONG);
            Log.e(TAG,e.getMessage());
        });
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
        final AssetInfoData info = getSession().getRegistry().getInfos().get(assetId);
        intent.putExtra("ASSET_ID", assetId)
        .putExtra("ASSET_INFO", info)
        .putExtra("SATOSHI", satoshi);
        startActivity(intent);
    }
}