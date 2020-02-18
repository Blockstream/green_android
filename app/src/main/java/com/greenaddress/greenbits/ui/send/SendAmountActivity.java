package com.greenaddress.greenbits.ui.send;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

import com.afollestad.materialdialogs.MaterialDialog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.gdk.GDKTwoFactorCall;
import com.greenaddress.greenapi.data.AssetInfoData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.assets.AssetsAdapter;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.greenaddress.gdk.GDKSession.getSession;
import static com.greenaddress.greenbits.ui.TabbedMainActivity.REQUEST_BITCOIN_URL_SEND;

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
    private String mSelectedAsset = "btc";
    private Long mAssetBalances;
    private boolean isSweep;

    private static final int[] mButtonIds =
    {R.id.fastButton, R.id.mediumButton, R.id.slowButton, R.id.customButton};
    private static final int[] mFeeButtonsText =
    {R.string.id_fast, R.string.id_medium, R.string.id_slow, R.string.id_custom};
    private FeeButtonView[] mFeeButtons = new FeeButtonView[4];
    private NetworkData networkData;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (modelIsNullOrDisconnected())
            return;

        Log.d(TAG, "onCreateView -> " + TAG);
        final int[] mBlockTargets = getBlockTargets();
        networkData = getGAApp().getCurrentNetworkData();

        isSweep = getIntent().getBooleanExtra(PrefKeys.SWEEP, false);

        if (isSweep) {
            final int account = getModel().getActiveAccountObservable().getActiveAccount();
            final String accountName = getModel().getSubaccountsData(account).getName();
            setTitle(String.format(getString(R.string.id_sweep_into_s),
                                   accountName.equals("") ? getString(R.string.id_main_account) : accountName));
        }

        // Create UI views
        setContentView(R.layout.activity_send_amount);
        UI.preventScreenshots(this);
        setTitleBackTransparent();
        mRecipientText = UI.find(this, R.id.addressText);
        mAccountBalance = UI.find(this, R.id.accountBalanceText);

        mAmountText = UI.find(this, R.id.amountText);
        UI.localeDecimalInput(mAmountText);
        mUnitButton = UI.find(this, R.id.unitButton);

        mAmountText.addTextChangedListener(this);
        mUnitButton.setOnClickListener(this);

        mUnitButton.setText(isFiat() ? getFiatCurrency() : getBitcoinOrLiquidUnit());
        mUnitButton.setPressed(!isFiat());
        mUnitButton.setSelected(!isFiat());

        mSendAllButton = UI.find(this, R.id.sendallButton);

        mNextButton = UI.find(this, R.id.nextButton);
        mNextButton.setOnClickListener(this);
        UI.disable(mNextButton);

        // Setup fee buttons
        mSelectedFee = getModel().getSettings().getFeeBuckets(mBlockTargets);

        final List<Long> estimates = getGAApp().getModel().getFeeObservable().getFees();
        mMinFeeRate = estimates.get(0);

        for (int i = 0; i < mButtonIds.length; ++i) {
            mFeeEstimates[i] = estimates.get(mBlockTargets[i]);
            mFeeButtons[i] = this.findViewById(mButtonIds[i]);
            final String summary = String.format("(%s)", UI.getFeeRateString(estimates.get(mBlockTargets[i])));
            final String expectedConfirmationTime = getExpectedConfirmationTime(this,
                                                                                networkData.getLiquid() ? 60 : 6,
                                                                                mBlockTargets[i]);
            final String buttonText = getString(mFeeButtonsText[i]) + (i == 3 ? "" : expectedConfirmationTime);
            mFeeButtons[i].init(buttonText, summary, i == 3);
            mFeeButtons[i].setOnClickListener(this);
        }

        // Create the initial transaction
        if (mTx == null) {
            try {
                final String tx = getIntent().getStringExtra(PrefKeys.INTENT_STRING_TX);
                final ObjectNode txJson = new ObjectMapper().readValue(tx, ObjectNode.class);
                // Fee
                // FIXME: If default fee is custom then fetch it here
                final LongNode fee_rate = new LongNode(mFeeEstimates[mSelectedFee]);
                txJson.set("fee_rate", fee_rate);

                // FIXME: If we didn't pass in the full transaction (with utxos)
                // then this call will go to the server. So, we should do it in
                // the background and display a wait icon until it returns
                final GDKTwoFactorCall call = getSession().createTransactionRaw(null, txJson);
                mTx = call.resolve(null, getConnectionManager().getHWResolver());
            } catch (final Exception e) {
                UI.toast(this, R.string.id_operation_failure, Toast.LENGTH_LONG);
                finishOnUiThread();
                return;
            }
        }
        try {
            // Retrieve assets list
            updateAssetSelected();
        } catch (final Exception e) {
            UI.toast(this, R.string.id_operation_failure, Toast.LENGTH_LONG);
            finishOnUiThread();
            return;
        }
        JsonNode node = mTx.get("satoshi");
        try {
            final ObjectNode addressee = (ObjectNode) mTx.get("addressees").get(0);
            node = addressee.get("satoshi");
        } catch (final Exception e) {
            // Asset not passed, default "btc"
        }
        if (node != null && node.asLong() != 0L) {
            final long newSatoshi = node.asLong();
            try {
                setAmountText(mAmountText, isFiat(), convert(newSatoshi), mSelectedAsset);
            } catch (final Exception e) {
                Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
            }
        }

        final JsonNode readOnlyNode = mTx.get("addressees_read_only");
        if (readOnlyNode != null && readOnlyNode.asBoolean()) {
            mAmountText.setEnabled(false);
            mSendAllButton.setVisibility(View.GONE);
            mAccountBalance.setVisibility(View.GONE);
        } else {
            mAmountText.requestFocus();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        // Select the fee button that is the next highest rate from the old tx
        final Long oldRate = getOldFeeRate(mTx);
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
        final boolean isBump = mTx.get("previous_transaction") != null;
        if (isBump) {
            mFeeEstimates[3] = getOldFeeRate(mTx) + mMinFeeRate;
        } else if (defaultFeerate != null) {
            final Double mPrefDefaultFeeRate = Double.valueOf(defaultFeerate);
            mFeeEstimates[3] = Double.valueOf(mPrefDefaultFeeRate * 1000.0).longValue();
            updateFeeSummaries();
        }

        try {
            updateTransaction(mRecipientText);
        } catch (final Exception e) {
            UI.toast(this, R.string.id_operation_failure, Toast.LENGTH_LONG);
            finishOnUiThread();
            return;
        }

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

    private void updateAssetSelected() {
        try {
            final ObjectNode addressee = (ObjectNode) mTx.get("addressees").get(0);
            mSelectedAsset = addressee.get("asset_tag").asText("btc");
        } catch (final Exception e) {
            // Asset not passed, default "btc"
        }

        final long satoshi = getModel().getCurrentAccountBalanceData().get(mSelectedAsset);
        final AssetInfoData info = getModel().getAssetsObservable().getAssetsInfos().get(mSelectedAsset);

        final Map<String, Long> balances = new HashMap<>();
        balances.put(mSelectedAsset, satoshi);

        final RecyclerView assetsList = findViewById(R.id.assetsList);
        final AssetsAdapter adapter = new AssetsAdapter(balances, getNetwork(), null, getModel());
        assetsList.setLayoutManager(new LinearLayoutManager(this));
        assetsList.setAdapter(adapter);
        UI.showIf(!isAsset(), mUnitButton);
        UI.showIf(!isAsset(), mAccountBalance);
        UI.showIf(isAsset(), assetsList);
        try {
            if (!isAsset())
                mAccountBalance.setText(getModel().getBtc(satoshi, true));
            else
                mAccountBalance.setText(getModel().getAsset(satoshi, mSelectedAsset, info, true));
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

        final boolean isLiquid = networkData.getLiquid();
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
    public void onClick(final View view) {
        if (view == mNextButton) {
            if (isKeyboardOpen) {
                InputMethodManager inputManager = (InputMethodManager)
                                                  this.getSystemService(Context.INPUT_METHOD_SERVICE);

                final View currentFocus = getCurrentFocus();
                inputManager.hideSoftInputFromWindow(currentFocus == null ? null : currentFocus.getWindowToken(),
                                                     InputMethodManager.HIDE_NOT_ALWAYS);
            } else {
                onFinish(mTx);
            }
        } else if (view == mSendAllButton) {
            mSendAll = !mSendAll;
            updateTransaction(null);
            mAmountText.setEnabled(!mSendAll);
            mSendAllButton.setPressed(mSendAll);
            mSendAllButton.setSelected(mSendAll);
        } else if (view == mUnitButton) {

            if (mCurrentAmount != null) {
                try {
                    final boolean isFiat = !mIsFiat;
                    setAmountText(mAmountText, isFiat, mCurrentAmount);

                    // Toggle unit display and selected state
                    mIsFiat = isFiat;
                    mUnitButton.setText(isFiat() ? getFiatCurrency() : getBitcoinOrLiquidUnit());
                    mUnitButton.setPressed(!isFiat());
                    mUnitButton.setSelected(!isFiat());
                    updateFeeSummaries();
                } catch (final ParseException e) {
                    UI.popup(this, R.string.id_your_favourite_exchange_rate_is).show();
                }
            }
        } else {
            final boolean isLiquid = networkData.getLiquid();

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
                updateTransaction(view);
        }
    }

    private void onCustomFeeClicked() {
        long customValue = mFeeEstimates[mButtonIds.length - 1];

        final String initValue = Model.getNumberFormat(2).format(customValue/1000.0);

        final View v = UI.inflateDialog(this, R.layout.dialog_set_custom_feerate);
        final EditText rateEdit = UI.find(v, R.id.set_custom_feerate_amount);
        UI.localeDecimalInput(rateEdit);
        rateEdit.setText(initValue);

        final MaterialDialog dialog;
        dialog = UI.popup(this, R.string.id_set_custom_fee_rate)
                 .customView(v, true)
                 .backgroundColor(getResources().getColor(R.color.buttonJungleGreen))
                 .onPositive((dlg, which) -> {
            try {
                final String rateText = rateEdit.getText().toString().trim();
                if (rateText.isEmpty())
                    throw new Exception();
                final double feePerByte = Model.getNumberFormat(2).parse(rateText).doubleValue();
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
                updateTransaction(mFeeButtons[mSelectedFee]);
            } catch (final Exception e) {
                e.printStackTrace();
                onClick(mFeeButtons[1]);          // FIXME: Get from user config
            }
        }).build();
        UI.showDialog(dialog);
    }

    private void updateTransaction(final View caller) {
        if (isFinishing())
            return;

        ObjectNode addressee = (ObjectNode) mTx.get("addressees").get(0);
        boolean changed;

        final BooleanNode send_all = mSendAll ? BooleanNode.TRUE : BooleanNode.FALSE;
        changed = !send_all.equals(mTx.replace("send_all", send_all));

        if (mSendAll) {
            // Send all was clicked and enabled. Mark changed to update amounts
            changed |= mSendAllButton == caller;
        } else if (mCurrentAmount != null) {
            // We are only changed if the amount entered has changed
            final LongNode satoshi = new LongNode(mCurrentAmount.get("satoshi").asLong());
            final JsonNode replacedValue = addressee.replace("satoshi", satoshi);
            changed |= !satoshi.toString().equals(replacedValue == null ? "" : replacedValue.toString());
        }

        final LongNode fee_rate = new LongNode(mFeeEstimates[mSelectedFee]);
        final JsonNode replacedValue = mTx.replace("fee_rate", fee_rate);
        changed |= !fee_rate.toString().equals(replacedValue == null ? "" : replacedValue.toString());

        // If the caller is mRecipientText, this is the initial creation so re-populate everything
        if (changed || caller == mRecipientText) {
            // Our tx has changed, so recreate it
            try {
                final GDKTwoFactorCall call = getSession().createTransactionRaw(null, mTx);
                mTx = call.resolve(null, getConnectionManager().getHWResolver());
            } catch (final Exception e) {
                // FIXME: Toast and go back to main activity since we must be disconnected
                throw new RuntimeException(e);
            }
            addressee = (ObjectNode) mTx.get("addressees").get(0);
            mRecipientText.setText(addressee.get("address").asText());

            // TODO this should be removed when handled in gdk
            if (networkData.getLiquid() && mSelectedAsset.isEmpty()) {
                mNextButton.setText(R.string.id_select_asset);
                return;
            }

            final String error = mTx.get("error").asText();
            if (error.isEmpty()) {
                // The tx is valid so show the updated amount
                try {
                    final long newSatoshi = addressee.get("satoshi").asLong();
                    // avoid updating view if value hasn't changed
                    if (mSendAll || (mCurrentAmount != null && mCurrentAmount.get("satoshi").asLong() != newSatoshi)) {
                        setAmountText(mAmountText, isFiat(), convert(newSatoshi), mSelectedAsset);
                    }
                } catch (final Exception e) {
                    Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
                }
                if (mTx.get("transaction_vsize") != null)
                    mVsize = mTx.get("transaction_vsize").asLong();
                updateFeeSummaries();
                mNextButton.setText(R.string.id_review);
            } else {
                mNextButton.setText(UI.i18n(getResources(), error));
            }
            UI.enableIf(error.isEmpty(), mNextButton);
        }
    }

    public ObjectNode convert(final long satoshi) throws Exception {
        final ObjectNode details = new ObjectMapper().createObjectNode();
        details.put("satoshi", satoshi);
        if (!"btc".equals(mSelectedAsset)) {
            final AssetInfoData info = getModel().getAssetsObservable().getAssetsInfos().get(mSelectedAsset);
            details.set("asset_info", info.toObjectNode());
        }
        mCurrentAmount = getSession().convert(details);
        return mCurrentAmount;
    }

    private void updateFeeSummaries() {
        if (mVsize == null)
            return;

        for (int i = 0; i < mButtonIds.length; ++i) {
            try {
                long currentEstimate = mFeeEstimates[i];
                final String feeRateString = UI.getFeeRateString(currentEstimate);
                final long amount = (currentEstimate * mVsize)/1000L;
                final String formatted = isFiat() ? getModel().getFiat(amount, true) : getModel().getBtc(amount, true);
                mFeeButtons[i].setSummary(mVsize == null ?
                                          String.format("(%s)", feeRateString) :
                                          String.format("%s (%s)", formatted, feeRateString));
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
        final AssetInfoData info = getModel().getAssetsObservable().getAssetsInfos().get(mSelectedAsset);
        removeUtxosIfTooBig(transactionData);
        intent.putExtra(PrefKeys.INTENT_STRING_TX, transactionData.toString());
        intent.putExtra("asset_info", info);
        intent.putExtra(PrefKeys.SWEEP, isSweep);
        if (getConnectionManager().isHW())
            intent.putExtra("hww", getConnectionManager().getHWDeviceData().toString());
        startActivityForResult(intent, REQUEST_BITCOIN_URL_SEND);
    }

    private boolean isFiat() {
        return mIsFiat;
    }

    private String getFiatCurrency() {
        return getModel().getFiatCurrency();
    }

    private String getBitcoinOrLiquidUnit() {
        return getModel().getBitcoinOrLiquidUnit();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        final String key = isAsset() ? mSelectedAsset : isFiat() ? "fiat" : getBitcoinUnitClean();
        if (key.isEmpty())
            return;
        final String localizedValue = mAmountText.getText().toString();
        String value = "0";
        try {
            value = Model.getNumberFormat(8).parse(localizedValue).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode amount = mapper.createObjectNode();

        amount.put(key, value);
        if (isAsset()) {
            final AssetInfoData assetInfoDefault = new AssetInfoData(mSelectedAsset);
            final AssetInfoData info = getModel().getAssetsObservable().getAssetsInfos().get(mSelectedAsset);
            amount.set("asset_info", (info == null ? assetInfoDefault : info).toObjectNode());
        }
        try {
            // avoid updating the view if changing from fiat to btc or vice versa
            if (!mSendAll &&
                (mCurrentAmount == null || mCurrentAmount.get(key) == null ||
                 !mCurrentAmount.get(key).asText().equals(value))) {
                mCurrentAmount = getSession().convert(amount);
                updateTransaction(null);
            }
        } catch (final Exception e) {
            Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
        }
    }

    private boolean isAsset() {
        return networkData.getLiquid() && !"btc".equals(mSelectedAsset);
    }

    @Override
    public void afterTextChanged(Editable s) {}
}
