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

public class SendAmountActivity extends LoggedActivity implements TextWatcher, View.OnClickListener {
    private static final String TAG = SendAmountActivity.class.getSimpleName();

    private boolean mSendAll = false;
    private MaterialDialog mCustomFeeDialog;
    private ObjectNode mTx;
    private Boolean isKeyboardOpen = false;

    private TextView mRecipientText;
    private TextView mAccountBalance;
    private Button mNextButton;
    private Button mSendAllButton;
    private EditText mAmountText;
    private Button mUnitButton;

    private boolean mIsFiat = false;
    private ObjectNode mCurrentAmount; // output from GA_convert_amount

    private long[] mFeeEstimates = new long[4];
    private int mSelectedFee;
    private long mMinFeeRate;
    private Long mVsize;
    private String mSelectedAsset;
    private SubaccountData mSubaccount;
    private boolean isSweep;

    private static final int[] mButtonIds =
    {R.id.fastButton, R.id.mediumButton, R.id.slowButton, R.id.customButton};
    private static final int[] mFeeButtonsText =
    {R.string.id_fast, R.string.id_medium, R.string.id_slow, R.string.id_custom};
    private FeeButtonView[] mFeeButtons = new FeeButtonView[4];
    private AmountTextWatcher mAmountTextWatcher;

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
        mAccountBalance = UI.find(this, R.id.accountBalanceText);

        mAmountText = UI.find(this, R.id.amountText);
        mAmountTextWatcher = new AmountTextWatcher(mAmountText);
        mAmountText.setHint(String.format("0%s00", mAmountTextWatcher.getDefaultSeparator()));
        mAmountText.addTextChangedListener(mAmountTextWatcher);
        mAmountText.addTextChangedListener(this);

        mUnitButton = UI.find(this, R.id.unitButton);
        mUnitButton.setOnClickListener(this);
        try {
            mUnitButton.setText(isFiat() ? Conversion.getFiatCurrency(getSession()) : Conversion.getBitcoinOrLiquidUnit(getSession()));
            mUnitButton.setPressed(!isFiat());
            mUnitButton.setSelected(!isFiat());
        } catch (final Exception e) {}

        mSendAllButton = UI.find(this, R.id.sendallButton);

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
        setupKeyboard();


        setupDisposable = Observable.just(getSession())
                          .observeOn(Schedulers.computation())
                          .map((session) -> {
            final SubaccountData subAccount = getSession().getSubAccount(this, getActiveAccount());
            return subAccount;
        })
                          .observeOn(AndroidSchedulers.mainThread())
                          .subscribe((subaccount) -> {
            stopLoading();
            mSubaccount = subaccount;
            setup(mTx);
            updateTransaction();
            updateAssetSelected();
        }, (e) -> {
            e.printStackTrace();
            stopLoading();
            UI.toast(this, R.string.id_operation_failure, Toast.LENGTH_LONG);
            finishOnUiThread();
        });
    }

    private void setupKeyboard() {
        final View contentView = findViewById(android.R.id.content);
        UI.attachHideKeyboardListener(this, contentView);

        contentView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            contentView.getWindowVisibleDisplayFrame(r);
            int screenHeight = contentView.getHeight();

            // r.bottom is the position above soft keypad or device button.
            // if keypad is shown, the r.bottom is smaller than that before.
            int keypadHeight = screenHeight - r.bottom;

            Log.d(TAG, "keypadHeight = " + keypadHeight);

            isKeyboardOpen = (keypadHeight > screenHeight * 0.15); // 0.15 ratio is perhaps enough to determine keypad height.
        });
    }

    private void hideKeyboard() {
        final InputMethodManager inputManager = (InputMethodManager)
                                                this.getSystemService(Context.INPUT_METHOD_SERVICE);
        final View currentFocus = getCurrentFocus();
        inputManager.hideSoftInputFromWindow(currentFocus == null ? null : currentFocus.getWindowToken(),
                                             InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void setup(final ObjectNode tx) {
        // Setup address and amount text
        try {
            JsonNode assetsMap = tx.get("satoshi");
            final ObjectNode addressee = (ObjectNode) tx.get("addressees").get(0);
            mRecipientText.setText(addressee.get("address").asText());
            // If addressee doesn't contain asset_id, we are sending btc
            final String asset = addressee.hasNonNull("asset_id") ? addressee.get("asset_id").asText() : getNetwork().getPolicyAsset();
            final long newSatoshi = assetsMap.get(asset).asLong();
            if (newSatoshi > 0) {
                mAmountText.removeTextChangedListener(mAmountTextWatcher);
                setAmountText(mAmountText, isFiat(), convert(newSatoshi), mSelectedAsset);
                mAmountText.addTextChangedListener(mAmountTextWatcher);
            }
        } catch (final Exception e) {
            Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
        }

        // Setup read-only
        final JsonNode readOnlyNode = tx.get("addressees_read_only");
        if (readOnlyNode != null && readOnlyNode.asBoolean()) {
            mAmountText.setEnabled(false);
            mSendAllButton.setVisibility(View.GONE);
            mAccountBalance.setVisibility(View.GONE);
        } else {
            mAmountText.requestFocus();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
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
            mFeeEstimates[3] = getOldFeeRate(tx) + mMinFeeRate;
        } else if (defaultFeerate != null) {
            final Double mPrefDefaultFeeRate = Double.valueOf(defaultFeerate);
            mFeeEstimates[3] = Double.valueOf(mPrefDefaultFeeRate * 1000.0).longValue();
            updateFeeSummaries();
        }
    }

    private void updateAssetSelected() {
        try {
            final ObjectNode addressee = (ObjectNode) mTx.get("addressees").get(0);
            mSelectedAsset = addressee.get("asset_id").asText(getNetwork().getPolicyAsset());
        } catch (final Exception e) {
            // Asset not passed, default policyAsset
        }

        final long satoshi = mSubaccount.getSatoshi().get(mSelectedAsset);
        final AssetInfoData info = getSession().getRegistry().getAssetInfo(mSelectedAsset);

        final Map<String, Long> balances = new HashMap<>();
        balances.put(mSelectedAsset, satoshi);

        final RecyclerView assetsList = findViewById(R.id.assetsList);
        final AssetsAdapter adapter = new AssetsAdapter(this, balances, getNetwork(), null);
        assetsList.setLayoutManager(new LinearLayoutManager(this));
        assetsList.setAdapter(adapter);
        UI.showIf(!isAsset(), mUnitButton);
        UI.showIf(!isAsset(), mAccountBalance);
        UI.showIf(isAsset(), assetsList);
        try {
            if (!isAsset())
                mAccountBalance.setText(Conversion.getBtc(getSession(), satoshi, true));
            else
                mAccountBalance.setText(Conversion.getAsset(getSession(), satoshi, mSelectedAsset, info, true));
        } catch (final Exception e) {
            Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
        }
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
        mSendAllButton.setPressed(mSendAll);
        mSendAllButton.setSelected(mSendAll);
        mSendAllButton.setOnClickListener(this);
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

        mCustomFeeDialog = UI.dismiss(this, mCustomFeeDialog);
        UI.unmapClick(mSendAllButton);
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
            if (isKeyboardOpen) {
                hideKeyboard();
            } else {
                onFinish(mTx);
            }
        } else if (view == mSendAllButton) {
            mSendAll = !mSendAll;
            mAmountText.setText(mSendAll ? R.string.id_all : R.string.empty);
            mCurrentAmount = null;
            mAmountText.setEnabled(!mSendAll);
            mSendAllButton.setPressed(mSendAll);
            mSendAllButton.setSelected(mSendAll);
            updateTransaction();
        } else if (view == mUnitButton) {
            try {
                mIsFiat = !mIsFiat;
                if (mCurrentAmount != null && mAmountTextWatcher != null) {
                    mAmountText.removeTextChangedListener(mAmountTextWatcher);
                    setAmountText(mAmountText, mIsFiat, mCurrentAmount);
                    mAmountText.addTextChangedListener(mAmountTextWatcher);
                }

                // Toggle unit display and selected state
                mUnitButton.setText(isFiat() ? Conversion.getFiatCurrency(getSession()) : Conversion.getBitcoinOrLiquidUnit(getSession()));
                mUnitButton.setPressed(!isFiat());
                mUnitButton.setSelected(!isFiat());
                updateFeeSummaries();
            } catch (final Exception e) {
                mIsFiat = !mIsFiat;
                UI.popup(this, R.string.id_your_favourite_exchange_rate_is).show();
            }
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
                hideKeyboard();
            } catch (final Exception e) {
                e.printStackTrace();
                onClick(mFeeButtons[1]);          // FIXME: Get from user config
                hideKeyboard();
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

        if (mCurrentAmount == null && !mSendAll) {
            mNextButton.setText(R.string.id_invalid_amount);
            return;
        }

        if (updateDisposable != null)
            updateDisposable.dispose();

        mTx.replace("send_all", mSendAll ? BooleanNode.TRUE : BooleanNode.FALSE);
        final ObjectNode addressee = (ObjectNode) mTx.get("addressees").get(0);
        if (mCurrentAmount != null) {
            final LongNode satoshi = new LongNode(mCurrentAmount.get("satoshi").asLong());
            addressee.replace("satoshi", satoshi);
        }
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

    public ObjectNode convert(final long satoshi) throws Exception {
        final ObjectNode details = new ObjectMapper().createObjectNode();
        details.put("satoshi", satoshi);
        if (!getNetwork().getPolicyAsset().equals(mSelectedAsset)) {
            final AssetInfoData info = getSession().getRegistry().getAssetInfo(mSelectedAsset);
            details.set("asset_info", info.toObjectNode());
        }
        mCurrentAmount = getSession().convert(details);
        return mCurrentAmount;
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
                    final String formatted = isFiat() ? Conversion.getFiat(getSession(), amount, true) : Conversion.getBtc(getSession(), amount, true);
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

    private boolean isFiat() {
        return mIsFiat;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        final String localizedValue = mAmountText.getText().toString();
        String value = "0";
        try {
            value = Conversion.getNumberFormat(8).parse(localizedValue).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode amount = mapper.createObjectNode();
        if (isAsset()) {
            final AssetInfoData assetInfoDefault = new AssetInfoData(mSelectedAsset);
            final AssetInfoData info = getSession().getRegistry().getAssetInfo(mSelectedAsset);
            amount.set("asset_info", (info == null ? assetInfoDefault : info).toObjectNode());
        }
        try {
            final String key = isAsset() ? mSelectedAsset : isFiat() ? "fiat" : getBitcoinUnitClean();
            amount.put(key, value);
            // avoid updating the view if changing from fiat to btc or vice versa
            if (!mSendAll &&
                (mCurrentAmount == null || mCurrentAmount.get(key) == null ||
                 !mCurrentAmount.get(key).asText().equals(value))) {
                mCurrentAmount = getSession().convert(amount);
                updateTransaction();
            }
        } catch (final Exception e) {
            Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
        }
    }

    private boolean isAsset() {
        return getSession().getNetworkData().getLiquid() && !getNetwork().getPolicyAsset().equals(mSelectedAsset);
    }

    @Override
    public void afterTextChanged(Editable s) {}
}
