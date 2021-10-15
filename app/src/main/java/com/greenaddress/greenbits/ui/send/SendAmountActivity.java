package com.greenaddress.greenbits.ui.send;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.greenapi.data.AssetInfoData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.model.Conversion;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.assets.AssetsAdapter;
import com.greenaddress.greenbits.ui.components.AmountTextWatcher;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;
import com.greenaddress.greenbits.wallets.HardwareCodeResolver;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SendAmountActivity extends LoggedActivity implements View.OnClickListener {
    private static final String TAG = SendAmountActivity.class.getSimpleName();

    private ObjectNode mTx;

    private TextView mRecipientText;
    private Button mNextButton;


    private long[] mFeeEstimates = new long[4];
    private int mSelectedFee;
    private long mMinFeeRate;
    private Long mVsize;
    private String mSelectedAsset;
    private boolean isSweep;

    private static final int[] mButtonIds =
    {R.id.fastButton, R.id.mediumButton, R.id.slowButton, R.id.customButton};
    private static final int[] mFeeButtonsText =
    {R.string.id_fast, R.string.id_medium, R.string.id_slow, R.string.id_custom};
    private FeeButtonView[] mFeeButtons = new FeeButtonView[4];

    private Disposable setupDisposable;
    private Disposable updateDisposable;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreateView -> " + TAG);
        final int[] mBlockTargets = getBlockTargets();
        mSelectedAsset = getNetwork().getPolicyAsset();

        isSweep = getIntent().getBooleanExtra(PrefKeys.SWEEP, false);

        if (isSweep) {
            setTitle(getString(R.string.id_sweep));
        }

        // Create UI views
        setContentView(R.layout.activity_send_amount);
        setTitleBackTransparent();
        mRecipientText = UI.find(this, R.id.addressText);

        mNextButton = UI.find(this, R.id.nextButton);
        mNextButton.setOnClickListener(this);
        UI.disable(mNextButton);

        try{
            // Setup fee buttons
            mSelectedFee = getSession().getSettings().getFeeBuckets(mBlockTargets);
        }catch (Exception e){
            e.printStackTrace();
            UI.toast(this, R.string.id_operation_failure, Toast.LENGTH_SHORT);
            finishOnUiThread();
            return;
        }


        final List<Long> estimates = getSession().getFees();
        if(estimates.isEmpty()){
            UI.toast(this, R.string.id_operation_failure, Toast.LENGTH_SHORT);
            finishOnUiThread();
            return;
        }
        mMinFeeRate = estimates.get(0);

        for (int i = 0; i < mButtonIds.length; ++i) {
            mFeeEstimates[i] = estimates.get(mBlockTargets[i]);
            mFeeButtons[i] = this.findViewById(mButtonIds[i]);
            final String summary = String.format("(%s)", UI.getFeeRateString(estimates.get(mBlockTargets[i])));
            final String expectedConfirmationTime = getExpectedConfirmationTime(this,
                                                                                getSession().getNetworkData().getLiquid() ? 60 : 6,
                                                                                mBlockTargets[i]);
            final String buttonText = getString(mFeeButtonsText[i]) + (i == 3 ? "" : expectedConfirmationTime);
            mFeeButtons[i].init(buttonText, summary, i == 3);
            mFeeButtons[i].setOnClickListener(this);
        }

        // Get pending transaction
        mTx = getSession().getPendingTransaction();
        if(mTx == null){
            UI.toast(this, R.string.id_operation_failure, Toast.LENGTH_SHORT);
            setResult(Activity.RESULT_CANCELED);
            finishOnUiThread();
            return;
        }

        // FIXME: If default fee is custom then fetch it here
        final LongNode fee_rate = new LongNode(mFeeEstimates[mSelectedFee]);
        mTx.set("fee_rate", fee_rate);

        // Update transaction
        setup(mTx);
        updateTransaction();
    }

    private void setup(final ObjectNode tx) {
        // Setup address and amount text
        try {
            final ObjectNode addressee = (ObjectNode) tx.get("addressees").get(0);
            mRecipientText.setText(addressee.get("address").asText());
        } catch (final Exception e) {
            e.printStackTrace();
        }

        // Select the fee button that is the next highest rate from the old tx
        final Long oldRate = getOldFeeRate(tx);
        if (oldRate != null) {
            mFeeEstimates[mButtonIds.length - 1] = oldRate + 1;
            boolean found = false;
            for (int i = 0; i < mButtonIds.length - 1; ++i) {
                if ((oldRate + mMinFeeRate) < mFeeEstimates[i]) {
                    mSelectedFee = i;
                    found = true;
                } else
                    mFeeButtons[i].setEnabled(false);
            }
            if (!found) {
                // Set custom rate to 1 satoshi higher than the old rate
                mSelectedFee = mButtonIds.length - 1;
            }
        }

        final String defaultFeerate = cfg().getString(PrefKeys.DEFAULT_FEERATE_SATBYTE, null);
        final boolean isBump = tx.get("previous_transaction") != null;
        if (isBump) {
            mSelectedFee = 3;
            mFeeEstimates[3] = getOldFeeRate(tx) + mMinFeeRate;
        } else if (defaultFeerate != null) {
            final Double mPrefDefaultFeeRate = Double.valueOf(defaultFeerate);
            mFeeEstimates[3] = Double.valueOf(mPrefDefaultFeeRate * 1000.0).longValue();
        }

        final boolean isLiquid = getSession().getNetworkData().getLiquid();
        for (int i = 0; i < mButtonIds.length; ++i) {
            mFeeButtons[i].setSelected(i == mSelectedFee, isLiquid);
        }

        updateFeeSummaries();
    }

    private void updateAssetSelected() {
        final JsonNode addressee = mTx.withArray("addressees").get(0);
        final String asset = addressee.hasNonNull("asset_id") ? addressee.get("asset_id").asText() : getNetwork().getPolicyAsset();
        final long amount = mTx.get("satoshi").get(asset).asLong(0);

        final Map<String, Long> balances = new HashMap<>();
        balances.put(asset, amount);

        final RecyclerView assetsList = findViewById(R.id.assetsList);
        final AssetsAdapter adapter = new AssetsAdapter(this, balances, getNetwork(), null);
        assetsList.setLayoutManager(new LinearLayoutManager(this));
        assetsList.setAdapter(adapter);
    }

    private int[] getBlockTargets() {
        final String[] stringArray = getResources().getStringArray(R.array.fee_target_values);
        final int[] blockTargets = {
            Integer.parseInt(stringArray[0]),
            Integer.parseInt(stringArray[1]),
            Integer.parseInt(stringArray[2]),
            0
        };
        return blockTargets;
    }

    private Long getOldFeeRate(final ObjectNode mTx) {
        final JsonNode previousTransaction = mTx.get("previous_transaction");
        if (previousTransaction != null) {
            final JsonNode oldFeeRate = previousTransaction.get("fee_rate");
            if (oldFeeRate != null && (oldFeeRate.isLong() || oldFeeRate.isInt())) {
                return oldFeeRate.asLong();
            }
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFinishing())
            return;

        final boolean isLiquid = getSession().getNetworkData().getLiquid();
        for (int i = 0; i < mButtonIds.length; ++i) {
            mFeeButtons[i].setSelected(i == mSelectedFee, isLiquid);
            mFeeButtons[i].setOnClickListener(this);
        }

        // FIXME: Update fee estimates (also update them if notified)
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isFinishing())
            return;

        for (int i = 0; i < mButtonIds.length; ++i)
            UI.unmapClick(mFeeButtons[i]);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (setupDisposable != null)
            setupDisposable.dispose();
        if (updateDisposable != null)
            updateDisposable.dispose();
    }

    @Override
    public void onClick(final View view) {
        if (view == mNextButton) {
            onFinish(mTx);
        } else {
            final boolean isLiquid = getSession().getNetworkData().getLiquid();

            // Fee Button
            for (int i = 0; i < mButtonIds.length; ++i) {
                final boolean isCurrentItem = mFeeButtons[i].getId() == view.getId();
                mFeeButtons[i].setSelected(isCurrentItem, isLiquid);
                mSelectedFee = isCurrentItem ? i : mSelectedFee;
            }
            // Set the block time in case the tx didn't change, if it did change
            // or the tx is invalid this will be overridden in updateTransaction()
            if (mSelectedFee == mButtonIds.length -1)
                onCustomFeeClicked();
            else
                updateTransaction();
        }
    }

    private void onCustomFeeClicked() {
        long customValue = mFeeEstimates[mButtonIds.length - 1];

        final String initValue = Conversion.getNumberFormat(2).format(customValue/1000.0);

        final View v = UI.inflateDialog(this, R.layout.dialog_set_custom_feerate);
        final EditText rateEdit = UI.find(v, R.id.set_custom_feerate_amount);
        final AmountTextWatcher amountTextWatcher = new AmountTextWatcher(rateEdit);
        rateEdit.setHint(String.format("0%s00", amountTextWatcher.getDefaultSeparator()));
        rateEdit.setText(initValue);
        rateEdit.addTextChangedListener(amountTextWatcher);

        final MaterialDialog dialog;
        dialog = UI.popup(this, R.string.id_set_custom_fee_rate)
                 .customView(v, true)
                 .backgroundColor(getResources().getColor(R.color.buttonJungleGreen))
                 .onPositive((dlg, which) -> {
            try {
                final String rateText = rateEdit.getText().toString().trim();
                if (rateText.isEmpty())
                    throw new Exception();
                final double feePerByte = Conversion.getNumberFormat(2).parse(rateText).doubleValue();
                final long feePerKB = (long) (feePerByte * 1000);
                if (feePerKB < mMinFeeRate) {
                    UI.toast(this, getString(R.string.id_fee_rate_must_be_at_least_s,
                                             String.format(Locale.US, "%.2f", mMinFeeRate/1000.0)),
                             Toast.LENGTH_LONG);
                    throw new Exception();
                }
                final Long oldFeeRate = getOldFeeRate(mTx);
                if (oldFeeRate != null && feePerKB < oldFeeRate) {
                    UI.toast(this, R.string.id_requested_fee_rate_too_low, Toast.LENGTH_LONG);
                    return;
                }

                mFeeEstimates[mButtonIds.length - 1] = feePerKB;
                updateFeeSummaries();
                // FIXME: Probably want to do this in the background
                updateTransaction();
                UI.hideSoftKeyboard(this);
            } catch (final Exception e) {
                e.printStackTrace();
                onClick(mFeeButtons[1]);          // FIXME: Get from user config
                UI.hideSoftKeyboard(this);
            }
        }).build();
        UI.showDialog(dialog);
    }

    private void updateTransaction() {
        if (isFinishing() || mTx == null)
            return;

        if (getSession().getNetworkData().getLiquid() && mSelectedAsset.isEmpty()) {
            mNextButton.setText(R.string.id_select_asset);
            return;
        }

        if (updateDisposable != null)
            updateDisposable.dispose();


        final LongNode fee_rate = new LongNode(mFeeEstimates[mSelectedFee]);
        mTx.replace("fee_rate", fee_rate);

        updateDisposable = Observable.just(mTx)
                           .observeOn(Schedulers.computation())
                           .map((tx) -> {
            return getSession().createTransactionRaw(tx).resolve(new HardwareCodeResolver(this), null);
        })
                           .observeOn(AndroidSchedulers.mainThread())
                           .subscribe((tx) -> {
            mTx = tx;
            updateAssetSelected();

            // TODO this should be removed when handled in gdk
            final String error = mTx.get("error").asText();
            if (error.isEmpty()) {
                if (mTx.get("transaction_vsize") != null)
                    mVsize = mTx.get("transaction_vsize").asLong();
                updateFeeSummaries();
                mNextButton.setText(R.string.id_review);
            } else {
                mNextButton.setText(UI.i18n(getResources(), error));
            }
            UI.enableIf(error.isEmpty(), mNextButton);
        }, (e) -> {
            e.printStackTrace();
            UI.toast(this, R.string.id_operation_failure, Toast.LENGTH_LONG);
            finishOnUiThread();
        });
    }

    private void updateFeeSummaries() {
        String feeSummary;
        for (int i = 0; i < mButtonIds.length; ++i) {
            try {
                long currentEstimate = mFeeEstimates[i];
                final String feeRateString = UI.getFeeRateString(currentEstimate);
                if (mVsize == null) {
                    feeSummary = String.format("(%s)", feeRateString);
                } else {
                    final long amount = (currentEstimate * mVsize)/1000L;

                    String formatted = Conversion.getBtc(getSession(), amount, true);
                    // final String formatted = isFiat() ? Conversion.getFiat(getSession(), amount, true) :
                    feeSummary = String.format("%s (%s)", formatted, feeRateString);
                }
                mFeeButtons[i].setSummary(feeSummary);

            } catch (final Exception e) {
                Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
            }
        }
    }

    private String getExpectedConfirmationTime(Context context, final int blocksPerHour, final int blocks) {
        final int n = (blocks % blocksPerHour) == 0 ? blocks / blocksPerHour : blocks * (60 / blocksPerHour);
        final String s = context.getString((blocks % blocksPerHour) == 0 ?
                                           (blocks == blocksPerHour ? R.string.id_hour : R.string.id_hours) :
                                           R.string.id_minutes);
        return String.format(Locale.getDefault(), " ~ %d %s", n, s);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BITCOIN_URL_SEND && resultCode == RESULT_OK) {
            setResult(resultCode);
            finishOnUiThread();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void onFinish(final ObjectNode transactionData) {
        // Open next fragment
        final Intent intent = new Intent(this, SendConfirmActivity.class);
        final AssetInfoData info = getSession().getRegistry().getAssetInfo(mSelectedAsset);
        getSession().setPendingTransaction(transactionData);
        intent.putExtra("asset_info", info);
        intent.putExtra(PrefKeys.SWEEP, isSweep);
        if (getSession().getHWWallet() != null)
            intent.putExtra("hww", getSession().getHWWallet().getDevice());
        startActivityForResult(intent, REQUEST_BITCOIN_URL_SEND);
    }
}
