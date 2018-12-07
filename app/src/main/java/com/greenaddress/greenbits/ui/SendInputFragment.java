package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.text.DecimalFormat;
import java.util.List;

import static com.greenaddress.greenbits.ui.ScanActivity.INTENT_STRING_TX;

public class SendInputFragment extends GAFragment implements View.OnClickListener,
    CurrencyView2.BalanceConversionProvider {
    private static final String TAG = SendInputFragment.class.getSimpleName();

    private boolean mSendAll = false;
    private OnCallbackListener mCallbackListener;
    private MaterialDialog mCustomFeeDialog;
    private ObjectNode mTx;

    private View mView;
    private TextView mRecipientText;
    private CurrencyView2 mAmountView;
    private Button mNextButton;
    private Button mSendAllButton;
    private TextView mFeeRateText;
    private TextView mFeeTimeText;
    private TextView mSummaryText;

    private static final int mButtonIds[] =
    { R.id.feeLowButton, R.id.feeMediumButton, R.id.feeHighButton, R.id.feeCustomButton };
    private static final int mBlockTargets[] = { 12, 6, 3, 0 };
    private static final int mBlockTimes[] =
    { R.string.id_4_hours, R.string.id_2_hours, R.string.id_1030_minutes, R.string.id_unknown_custom };
    private Button[] mFeeButtons = new Button[4];
    private long[] mFeeEstimates = new long[4];
    private int mSelectedFee;
    private long mMinFeeRate;
    private Double mPrefDefaultFeeRate;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView -> " + TAG);

        // Get arguments from bundle
        final Bundle b = this.getArguments();
        if (b == null)
            return mView;

        final GaService service = getGAService();

        // Create UI views
        mView = inflater.inflate(R.layout.fragment_send_input, container, false);
        mRecipientText = UI.find(mView, R.id.addressText);
        mAmountView = UI.find(mView, R.id.sendAmountCurrency);

        mSendAllButton = UI.find(mView, R.id.sendallButton);

        mFeeRateText = UI.find(mView, R.id.feeText);
        mFeeTimeText = UI.find(mView, R.id.feeTimeText);
        mSummaryText = UI.find(mView, R.id.amountWithFeeText);

        mNextButton = UI.find(mView, R.id.nextButton);
        mNextButton.setOnClickListener(this);
        UI.disable(mNextButton);

        // Setup fee buttons
        mSelectedFee = 1; // FIXME: Get From user config, Set custom fee from config
        final List<Long> estimates = service.getFeeEstimates();
        mMinFeeRate = estimates.get(0);

        for (int i = 0; i < mButtonIds.length; ++i) {
            mFeeEstimates[i] = estimates.get(mBlockTargets[i]);
            mFeeButtons[i] = UI.find(mView, mButtonIds[i]);
            mFeeButtons[i].setOnClickListener(this);
        }

        // Setup vars
        mCallbackListener = (OnCallbackListener) getGaActivity();

        // Create the initial transaction
        try {
            final String tx = b.getString(INTENT_STRING_TX);
            final ObjectNode txJson = new ObjectMapper().readValue(tx, ObjectNode.class);
            // Fee
            // FIXME: If default fee is custom then fetch it here
            final LongNode fee_rate = new LongNode(mFeeEstimates[mSelectedFee]);
            txJson.set("fee_rate", fee_rate);

            // FIXME: If we didn't pass in the full transaction (with utxos)
            // then this call will go to the server. So, we should do it in
            // the background and display a wait icon until it returns
            mTx = service.getSession().createTransactionRaw(txJson);

            final JsonNode node = mTx.get("satoshi");
            if (node != null && node.asLong() != 0L) {
                final long newSatoshi = node.asLong();
                mAmountView.setAmounts(service.getSession().convertSatoshi(newSatoshi));
            }

            final JsonNode readOnlyNode = mTx.get("addressees_read_only");
            if (readOnlyNode != null && readOnlyNode.asBoolean()) {
                mAmountView.setEnabled(false);
                mSendAllButton.setEnabled(false);
            }

            // Select the fee button that is the next highest rate from the old tx
            final Long oldRate = getOldFeeRate(mTx);
            if (oldRate != null) {
                mFeeEstimates[mButtonIds.length - 1] = oldRate + 1;
                boolean found = false;
                for (int i = 0; i < mButtonIds.length -1 && !found; ++i) {
                    if (oldRate < mFeeEstimates[i]) {
                        mSelectedFee = i;
                        found = true;
                    } else
                        mFeeButtons[i].setEnabled(false);
                }
                if (!found) {
                    // Set custom rate to 1 satoshi higher than the old rate
                    mSelectedFee = mButtonIds.length - 1;
                }
                // Poke in the new rate
                txJson.set("fee_rate", new LongNode(mFeeEstimates[mSelectedFee]));
            }

            final String defaultFeerate = service.cfg().getString(PrefKeys.DEFAULT_FEERATE_SATBYTE, null);
            final boolean isNotBump = mTx.get("previous_transaction") == null;
            if (defaultFeerate != null && isNotBump) {
                mPrefDefaultFeeRate = Double.valueOf(defaultFeerate);
                mFeeEstimates[3] = Double.valueOf(mPrefDefaultFeeRate*1000.0).longValue();
            }

            updateTransaction(mRecipientText);
        } catch (Exception e) {
            // FIXME: Toast and go back to main activity since we must be disconnected
            throw new RuntimeException(e);
        }

        attachHideKeyboardListener(mView);

        return mView;
    }

    // Keyboard hiding taken from https://stackoverflow.com/a/11656129
    public void attachHideKeyboardListener(View view) {
        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText) && !(view instanceof Button)) {
            view.setOnClickListener(v -> hideSoftKeyboard(getActivity()));
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                attachHideKeyboardListener(innerView);
            }
        }
    }

    public static void hideSoftKeyboard(Activity activity) {
        if (activity == null)
            return;
        final InputMethodManager inputMethodManager =
            (InputMethodManager) activity.getSystemService(
                Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager == null || activity.getCurrentFocus() == null)
            return;
        inputMethodManager.hideSoftInputFromWindow(
            activity.getCurrentFocus().getWindowToken(), 0);
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

        // Setup balance
        final GaService service = getGAService();
        final BalanceData balanceData = service.getBalanceData(service.getSession().getCurrentSubaccount());
        final TextView accountBalance = UI.find(mView, R.id.accountBalanceText);
        accountBalance.setText(service.getValueString(balanceData.toObjectNode(), false, true));

        mSendAllButton.setPressed(mSendAll);
        mSendAllButton.setSelected(mSendAll);
        mSendAllButton.setOnClickListener(this);
        for (int i = 0; i < mButtonIds.length; ++i) {
            mFeeButtons[i].setSelected(i == mSelectedFee);
            mFeeButtons[i].setOnClickListener(this);
        }

        mAmountView.onResume(this, service.getBitcoinUnit(), service.getFiatCurrency());
        // FIXME: Update fee estimates (also update them if notified)
    }

    @Override
    public void onPause() {
        super.onPause();
        mAmountView.onPause();
        mCustomFeeDialog = UI.dismiss(getGaActivity(), mCustomFeeDialog);
        mSendAllButton.setOnClickListener(null);
        for (int i = 0; i < mButtonIds.length; ++i)
            mFeeButtons[i].setOnClickListener(null);
    }

    @Override
    public void onClick(final View view) {
        if (view == mNextButton) {
            if (mCallbackListener != null)
                mCallbackListener.onFinish(mTx);
        } else if (view == mSendAllButton) {
            mSendAll = !mSendAll;
            updateTransaction(null);
            mAmountView.setSendAll(mSendAll);
            mSendAllButton.setPressed(mSendAll);
            mSendAllButton.setSelected(mSendAll);
        } else {
            // Fee Button
            for (int i = 0; i < mButtonIds.length; ++i) {
                final boolean isCurrentItem = mFeeButtons[i].getId() == view.getId();
                mFeeButtons[i].setSelected(isCurrentItem);
                mSelectedFee = isCurrentItem ? i : mSelectedFee;
            }
            // Set the block time in case the tx didn't change, if it did change
            // or the tx is invalid this will be overridden in updateTransaction()
            mFeeTimeText.setText("Time: " + getString(mBlockTimes[mSelectedFee]));
            if (mSelectedFee == mButtonIds.length -1)
                onCustomFeeClicked();
            else
                updateTransaction(view);
        }
    }

    private void onCustomFeeClicked() {
        final String hint = getFeeRateString(mFeeEstimates[mButtonIds.length -1]);

        mCustomFeeDialog = new MaterialDialog.Builder(getActivity())
                           .title(R.string.id_set_custom_fee_rate)
                           .input(hint,
                                  mPrefDefaultFeeRate != null ? String.valueOf(mPrefDefaultFeeRate) : "",
                                  false,
                                  new MaterialDialog.InputCallback() {
            @Override
            public void onInput(@NonNull final MaterialDialog dialog, final CharSequence input) {
                try {
                    final String rateText = input.toString().trim();
                    if (rateText.isEmpty())
                        throw new Exception();
                    final double feePerByte = Double.valueOf(rateText);
                    final long feePerKB = (long) (feePerByte * 1000);
                    if (feePerKB < mMinFeeRate) {
                        UI.toast(getGaActivity(), R.string.id_fee_rate_must_be_higher_than, Toast.LENGTH_LONG);
                        throw new Exception();
                    }
                    final Long oldFeeRate = getOldFeeRate(mTx);
                    if (oldFeeRate != null && feePerKB < oldFeeRate) {
                        UI.toast(getGaActivity(), R.string.id_requested_fee_rate_too_low, Toast.LENGTH_LONG);
                        return;
                    }

                    mFeeEstimates[mButtonIds.length - 1] = feePerKB;
                    // FIXME: Probably want to do this in the background
                    updateTransaction(mFeeButtons[mSelectedFee]);
                } catch (final Exception e) {
                    e.printStackTrace();
                    onClick(mFeeButtons[1]);         // FIXME: Get from user config
                }
            }
        }).show();
    }


    private void updateTransaction(final View caller)
    {
        if (!isAdded() || isDetached() || getGaActivity() == null)
            return;

        ObjectNode addressee = (ObjectNode) mTx.get("addressees").get(0);
        boolean changed;

        final BooleanNode send_all = mSendAll ? BooleanNode.TRUE : BooleanNode.FALSE;
        changed = !send_all.equals(mTx.replace("send_all", send_all));

        if (mSendAll) {
            // Send all was clicked and enabled. Mark changed to update amounts
            changed |= mSendAllButton == caller;
        } else {
            // We are only changed if the amount entered has changed
            final LongNode satoshi = new LongNode(mAmountView.getSatoshi());

            // toString() and the null check are required because jackson is completely insane:
            // set(LongNode(0)); replace(LongNode(0)).equals(LongNode(0)) is false.
            final JsonNode replacedValue = addressee.replace("satoshi", satoshi);
            changed |= !satoshi.toString().equals(replacedValue == null ? "" : replacedValue.toString());
        }

        final GDKSession session = getGAService().getSession();
        final LongNode fee_rate = new LongNode(mFeeEstimates[mSelectedFee]);
        final JsonNode replacedValue = mTx.replace("fee_rate", fee_rate);
        changed |= !fee_rate.toString().equals(replacedValue == null ? "" : replacedValue.toString());

        // If the caller is mRecipientText, this is the initial creation so re-populate everything
        if (changed || caller == mRecipientText) {
            // Our tx has changed, so recreate it
            try {
                mTx = session.createTransactionRaw(mTx);
            } catch (final Exception e) {
                // FIXME: Toast and go back to main activity since we must be disconnected
                throw new RuntimeException(e);
            }
            addressee = (ObjectNode) mTx.get("addressees").get(0);
            mRecipientText.setText(addressee.get("address").asText());
            final String error = mTx.get("error").asText();
            if (error.isEmpty()) {
                // The tx is valid so show the updated amount
                mAmountView.setAmounts(session.convertSatoshi(addressee.get("satoshi").asLong()));

                mFeeRateText.setText(getFeeRateString(mTx.get("fee_rate").asLong()));
                mFeeTimeText.setText("Time: " + getString(mBlockTimes[mSelectedFee]));
                final ObjectNode fee = session.convertSatoshi(mTx.get("fee").asLong());
                final String fiatFee = getGAService().getValueString(fee, true, true);
                final String btcFee = getGAService().getValueString(fee, false, true);
                mSummaryText.setText(String.format("Fee: %s / %s", btcFee, fiatFee));
            } else {
                mFeeRateText.setText("");
                mFeeTimeText.setText("");
                mSummaryText.setText(UI.i18n(getResources(), error));
            }
            UI.enableIf(error.isEmpty(), mNextButton);
        }
    }

    // FIXME move to common place?
    public static String getFeeRateString(final long feePerKB) {
        final double feePerByte = feePerKB / 1000.0;
        return (new DecimalFormat(".##")).format(feePerByte) +  " satoshi / vbyte";
    }

    public ObjectNode convertAmount(final ObjectNode amount) {
        return getGAService().getSession().convert(amount);
    }

    public void amountEntered() {
        updateTransaction(null);
    }

    public interface OnCallbackListener {
        void onFinish(final JsonNode transactionData);
    }
}
