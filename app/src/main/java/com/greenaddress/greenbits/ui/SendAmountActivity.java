package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.greenapi.data.AssetInfoData;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.greenaddress.gdk.GDKSession.getSession;
import static com.greenaddress.greenbits.ui.ScanActivity.INTENT_STRING_TX;
import static com.greenaddress.greenbits.ui.TabbedMainActivity.REQUEST_BITCOIN_URL_SEND;
import static com.greenaddress.greenbits.ui.TabbedMainActivity.REQUEST_SELECT_ASSET;

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

    private FontFitEditText mAmountText;
    private Button mUnitButton;

    private TextView mSelectAsset;

    private boolean mIsFiat = false;
    private ObjectNode mCurrentAmount; // output from GA_convert_amount

    private long[] mFeeEstimates = new long[4];
    private int mSelectedFee;
    private long mMinFeeRate;
    private Long mVsize;
    private String mSelectedAsset = "";
    private Map<String, BalanceData> mAssetsBalances;

    private static final int mButtonIds[] =
    { R.id.fastButton, R.id.mediumButton, R.id.slowButton, R.id.customButton };
    private static final int mFeeButtonsText[] =
    { R.string.id_fast, R.string.id_medium, R.string.id_slow, R.string.id_custom };
    private FeeButtonView[] mFeeButtons = new FeeButtonView[4];

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        if (mService == null || mService.getModel() == null) {
            toFirst();
            return;
        }

        Log.d(TAG, "onCreateView -> " + TAG);
        final int[] mBlockTargets = getBlockTargets();
        final GaService service = mService;

        // Create UI views
        setContentView(R.layout.activity_send_amount);
        UI.preventScreenshots(this);
        setTitleBackTransparent();
        mRecipientText = UI.find(this, R.id.addressText);
        mAccountBalance = UI.find(this, R.id.accountBalanceText);

        mAmountText = UI.find(this, R.id.amountText);
        mUnitButton = UI.find(this, R.id.unitButton);
        mSelectAsset = UI.find(this, R.id.selectAsset);

        // Retrieve assets list
        try {
            mAssetsBalances = getModel().getCurrentAccountBalanceData();
            updateAssetSelected();
        } catch (final Exception e) {
            e.printStackTrace();
        }

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
        mSelectedFee = service.getModel().getSettings().getFeeBuckets(mBlockTargets);

        final List<Long> estimates = service.getFeeEstimates();
        mMinFeeRate = estimates.get(0);

        for (int i = 0; i < mButtonIds.length; ++i) {
            mFeeEstimates[i] = estimates.get(mBlockTargets[i]);
            mFeeButtons[i] = this.findViewById(mButtonIds[i]);
            final String summary = String.format("(%s)", UI.getFeeRateString(estimates.get(mBlockTargets[i])));
            final String expectedConfirmationTime = getExpectedConfirmationTime(this,
                                                                                mService.isLiquid() ? 60 : 6,
                                                                                mBlockTargets[i]);
            final String buttonText = getString(mFeeButtonsText[i]) + (i == 3 ? "" : expectedConfirmationTime);
            mFeeButtons[i].init(buttonText, summary, i == 3);
            mFeeButtons[i].setOnClickListener(this);
        }

        // Create the initial transaction
        try {
            if (mTx == null) {
                final String tx = getIntent().getStringExtra(INTENT_STRING_TX);
                final ObjectNode txJson = new ObjectMapper().readValue(tx, ObjectNode.class);
                // Fee
                // FIXME: If default fee is custom then fetch it here
                final LongNode fee_rate = new LongNode(mFeeEstimates[mSelectedFee]);
                txJson.set("fee_rate", fee_rate);

                // FIXME: If we didn't pass in the full transaction (with utxos)
                // then this call will go to the server. So, we should do it in
                // the background and display a wait icon until it returns
                mTx = getSession().createTransactionRaw(txJson);
            }

            final JsonNode node = mTx.get("satoshi");
            if (node != null && node.asLong() != 0L) {
                final long newSatoshi = node.asLong();
                try {
                    final String unit = mSelectedAsset.isEmpty() ||
                                        mSelectedAsset == "btc" ? getBitcoinUnitClean() : mSelectedAsset;
                    mAmountText.setText(convert(newSatoshi).get(isFiat() ? "fiat" : unit).asText());
                } catch (final RuntimeException | IOException e) {
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
                for (int i = 0; i < mButtonIds.length -1; ++i) {
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

            final String defaultFeerate = service.cfg().getString(PrefKeys.DEFAULT_FEERATE_SATBYTE, null);
            final boolean isBump = mTx.get("previous_transaction") != null;
            if (isBump) {
                mFeeEstimates[3] = getOldFeeRate(mTx) + mMinFeeRate;
            } else if (defaultFeerate != null) {
                final Double mPrefDefaultFeeRate = Double.valueOf(defaultFeerate);
                mFeeEstimates[3] = Double.valueOf(mPrefDefaultFeeRate *1000.0).longValue();
                updateFeeSummaries();
            }

            updateTransaction(mRecipientText);
        } catch (final Exception e) {
            // FIXME: Toast and go back to main activity since we must be disconnected
            throw new RuntimeException(e);
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

        if (mService.isLiquid()) {
            mSelectAsset.setOnClickListener(v -> startActivityForResult(
                                                new Intent(this, AssetsSelectActivity.class),
                                                REQUEST_SELECT_ASSET));
        } else {
            mSelectAsset.setVisibility(View.GONE);
        }
    }

    private void updateAssetSelected() {
        if (mSelectedAsset.isEmpty()) {
            mSelectAsset.setText(getString(R.string.id_select_asset));
            return;
        }
        final AssetInfoData assetInfo = mAssetsBalances.get(mSelectedAsset).getAssetInfo();
        final String label = assetInfo != null ? assetInfo.getName() : mSelectedAsset;
        mSelectAsset.setText("btc".equals(mSelectedAsset) ? getBitcoinOrLiquidUnit() : label);
        UI.showIf(!mService.isLiquid() || "btc".equals(mSelectedAsset), mUnitButton);
        updateAccountBalance();
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
    public void onResumeWithService() {
        super.onResumeWithService();
        if (mService == null || mService.getModel() == null) {
            toFirst();
            return;
        }
        if (mService.isDisconnected()) {
            return;
        }

        final boolean isLiquid = mService.getNetwork().getLiquid();
        updateAccountBalance();
        mSendAllButton.setPressed(mSendAll);
        mSendAllButton.setSelected(mSendAll);
        mSendAllButton.setOnClickListener(this);
        for (int i = 0; i < mButtonIds.length; ++i) {
            mFeeButtons[i].setSelected(i == mSelectedFee, isLiquid);
            mFeeButtons[i].setOnClickListener(this);
        }

        // FIXME: Update fee estimates (also update them if notified)
    }

    void updateAccountBalance() {
        // Setup balance
        final Integer subAccount = mService.getModel().getCurrentSubaccount();
        final String asset = mSelectedAsset != null && !mSelectedAsset.isEmpty() ? mSelectedAsset : "btc";
        final BalanceData balanceData = mAssetsBalances.get(asset);
        final Long satoshi = balanceData.getSatoshi();
        if ("btc".equals(asset) )
            mAccountBalance.setText(mService.getValueString(satoshi, false, true));
        else
            mAccountBalance.setText(mService.getValueString(satoshi, asset, balanceData.getAssetInfo(), true));
    }

    @Override
    public void onPauseWithService() {
        super.onPauseWithService();
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
            // Toggle unit display and selected state
            mIsFiat = !mIsFiat;
            mUnitButton.setText(isFiat() ? getFiatCurrency() : getBitcoinOrLiquidUnit());
            mUnitButton.setPressed(!isFiat());
            mUnitButton.setSelected(!isFiat());
            updateFeeSummaries();

            if (mCurrentAmount != null) {
                mAmountText.setText(isFiat() ? mCurrentAmount.get("fiat").asText() : mCurrentAmount.get(
                                        getBitcoinUnitClean()).asText());
            }
        } else {
            final boolean isLiquid = mService.getNetwork().getLiquid();

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
        final String hint = UI.getFeeRateString(customValue);
        final String initValue = String.format(Locale.US, "%.2f", customValue/1000.0);

        mCustomFeeDialog = new MaterialDialog.Builder(this)
                           .title(R.string.id_set_custom_fee_rate)
                           .positiveText(android.R.string.ok)
                           .negativeText(android.R.string.cancel)
                           .backgroundColor(getResources().getColor(R.color.buttonJungleGreen))
                           .inputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL)
                           .input(hint,
                                  initValue,
                                  false,
                                  (dialog, input) -> {
            try {
                final String rateText = input.toString().trim();
                if (rateText.isEmpty())
                    throw new Exception();
                final double feePerByte = Double.valueOf(rateText);
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
                onClick(mFeeButtons[1]);                                    // FIXME: Get from user config
            }
        }).show();
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

        if (caller == mSelectAsset) {
            final JsonNode assetTag = addressee.get("asset_tag");
            changed |= assetTag == null || !assetTag.asText().equals(mSelectedAsset);
            addressee.put("asset_tag", mSelectedAsset);
            updateAssetSelected();
        }

        final LongNode fee_rate = new LongNode(mFeeEstimates[mSelectedFee]);
        final JsonNode replacedValue = mTx.replace("fee_rate", fee_rate);
        changed |= !fee_rate.toString().equals(replacedValue == null ? "" : replacedValue.toString());

        // If the caller is mRecipientText, this is the initial creation so re-populate everything
        if (changed || caller == mRecipientText) {
            // Our tx has changed, so recreate it
            try {
                mTx = getSession().createTransactionRaw(mTx);
            } catch (final Exception e) {
                // FIXME: Toast and go back to main activity since we must be disconnected
                throw new RuntimeException(e);
            }
            addressee = (ObjectNode) mTx.get("addressees").get(0);
            mRecipientText.setText(addressee.get("address").asText());

            // TODO this should be removed when handled in gdk
            if (mService.isLiquid() && mSelectedAsset.isEmpty()) {
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
                        final String unit = mSelectedAsset.isEmpty() ||
                                            mSelectedAsset == "btc" ? getBitcoinUnitClean() : mSelectedAsset;
                        mAmountText.setText(convert(newSatoshi).get(isFiat() ? "fiat" : unit).asText());
                    }
                } catch (final RuntimeException | IOException e) {
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

    public ObjectNode convert(final long satoshi) throws RuntimeException, IOException {
        final String asset = mSelectedAsset.isEmpty() ? "btc" : mSelectedAsset;
        AssetInfoData assetInfo = new AssetInfoData(asset, "", 0, "", "");
        if (mAssetsBalances.get(asset) != null && mAssetsBalances.get(asset).getAssetInfo() != null)
            assetInfo = mAssetsBalances.get(asset).getAssetInfo();
        final ObjectNode details = new ObjectMapper().createObjectNode();
        details.put("satoshi", satoshi);
        details.set("asset_info", assetInfo.toObjectNode());
        mCurrentAmount = getSession().convert(details);
        return mCurrentAmount;
    }

    private void updateFeeSummaries() {
        for (int i = 0; i < mButtonIds.length; ++i) {
            long currentEstimate = mFeeEstimates[i];
            final String feeRateString = UI.getFeeRateString(currentEstimate);
            mFeeButtons[i].setSummary(mVsize == null ?
                                      String.format("(%s)", feeRateString) :
                                      String.format("%s (%s)", mService.getValueString(
                                                        (currentEstimate * mVsize)/1000L,
                                                        isFiat(), true),
                                                    feeRateString));
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
        } else if (requestCode == REQUEST_SELECT_ASSET && resultCode == RESULT_OK) {
            mSelectedAsset = data.getStringExtra(PrefKeys.ASSET_SELECTED);

            // onTextChanged updates mCurrentAmount otherwise updateTransaction doesn't work
            onTextChanged(null,0,0,0);

            updateTransaction(mSelectAsset);
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

    public void onFinish(final JsonNode transactionData) {
        // Open next fragment
        final Intent intent = new Intent(this, SendConfirmActivity.class);
        intent.putExtra("transaction", transactionData.toString());
        intent.putExtra("asset_info", mAssetsBalances.get(
                            mSelectedAsset.isEmpty() ? "btc" : mSelectedAsset).getAssetInfo());
        if (mService.getConnectionManager().isHW())
            intent.putExtra("hww", mService.getConnectionManager().getHWDeviceData().toString());
        startActivityForResult(intent, REQUEST_BITCOIN_URL_SEND);
    }

    private boolean isFiat() {
        return mIsFiat;
    }

    private String getFiatCurrency() {
        return mService.getFiatCurrency();
    }

    private String getBitcoinOrLiquidUnit() {
        return mService.getBitcoinOrLiquidUnit();
    }

    private String getBitcoinUnitClean() {
        return mService.getUnitKey();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        final String key = isAsset() ? mSelectedAsset : isFiat() ? "fiat" : getBitcoinUnitClean();
        if (key.isEmpty())
            return;
        final String value = mAmountText.getText().toString();
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode amount = mapper.createObjectNode();
        amount.put(key, value.isEmpty() ? "0" : value);
        if (isAsset()) {
            final AssetInfoData assetInfoDefault = new AssetInfoData(mSelectedAsset, mSelectedAsset, 0, "", "");
            final AssetInfoData assetInfo = mAssetsBalances.get(key).getAssetInfo();
            amount.set("asset_info", (assetInfo == null ? assetInfoDefault : assetInfo).toObjectNode());
        }
        try {
            // avoid updating the view if changing from fiat to btc or vice versa
            if (!mSendAll &&
                (mCurrentAmount == null || mCurrentAmount.get(key) == null ||
                 !mCurrentAmount.get(key).asText().equals(value))) {
                mCurrentAmount = getSession().convert(amount);
                updateTransaction(null);
            }
        } catch (final RuntimeException | IOException e) {
            Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
        }
    }

    private boolean isAsset() {
        return mService.isLiquid() && !"btc".equals(mSelectedAsset);
    }

    @Override
    public void afterTextChanged(Editable s) {}
}
