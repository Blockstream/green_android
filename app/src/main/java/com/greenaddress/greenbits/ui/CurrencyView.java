package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.Fiat;

public class CurrencyView extends RelativeLayout implements View.OnClickListener, TextWatcher {


    private EditText mAmountEdit;
    private TextView mAmountText;
    private Button mUnitButton;

    private boolean mConverting;
    private GaService mService;
    private Boolean mIsPausing = false;
    private boolean mIsToFiat = true;
    private OnConversionFinishListener mOnConversionFinishListener;
    private Coin mCoinValue;
    private Fiat mFiatValue;

    public CurrencyView(final Context context) {
        super(context);
        setup(context);
    }

    public CurrencyView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    private void setup(final Context context) {
        final LayoutInflater inflater = (LayoutInflater) context
                                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_currency, this, true);

        final View view = getRootView();
        mAmountEdit = UI.find(view, R.id.amountEditText);
        mAmountText = UI.find(view, R.id.amountText);
        mUnitButton = UI.find(view, R.id.unitButton);
        mAmountEdit.addTextChangedListener(this);
        mUnitButton.setOnClickListener(this);
    }

    public void setService(final GaService gaService) {
        mService = gaService;
    }

    public void setOnConversionFinishListener(final CurrencyView.OnConversionFinishListener onConversionFinishListener)
    {
        mOnConversionFinishListener = onConversionFinishListener;
    }

    public void setCoin(final Coin coin, boolean update) {
        mCoinValue = coin;
        if (update)
            updateFields();
    }

    public void enableAll() {
        mConverting = true;
        mAmountEdit.setText("All");
        mAmountEdit.setEnabled(false);
        mConverting = false;
        mUnitButton.setVisibility(View.INVISIBLE);
    }

    public void disableAll(boolean update) {
        mAmountEdit.setEnabled(true);
        if (update)
            updateFields();
        mUnitButton.setVisibility(View.VISIBLE);
    }

    public boolean isToFiat() {
        return mIsToFiat;
    }

    public Coin getCoin() {
        return mCoinValue;
    }

    public void setFiat(final Fiat fiat) {
        mFiatValue = fiat;
        updateFields();
    }

    public Fiat getFiat() {
        return mFiatValue;
    }

    public void clear() {
        mConverting = true;
        mCoinValue = null;
        mFiatValue = null;
        mAmountText.setText("");
        mAmountEdit.setText("");
        mConverting = false;
    }

    public void setEnabled(final boolean enabled) {
        mAmountEdit.setEnabled(enabled);
    }

    private void updateToCoin() {
        mUnitButton.setText(mService.getFiatCurrency());
        mUnitButton.setPressed(false);
        mUnitButton.setSelected(false);
        if (mFiatValue != null)
            mAmountEdit.setText(mFiatValue.toPlainString());
        if (mCoinValue != null)
            mAmountText.setText(UI.formatCoinValue(mService, mCoinValue) + " " + mService.getBitcoinUnit());
        else
            convertToCoin();
    }

    private void updateToFiat() {
        mUnitButton.setText(mService.getBitcoinUnit());
        mUnitButton.setPressed(true);
        mUnitButton.setSelected(true);
        if (mCoinValue != null)
            mAmountEdit.setText(UI.formatCoinValue(mService, mCoinValue));
        if (mFiatValue != null)
            mAmountText.setText(mFiatValue.toFriendlyString());
        else
            convertToFiat();
    }

    private void updateFields() {
        if (mService == null)
            return;

        if (mIsToFiat)
            updateToFiat();
        else
            updateToCoin();
    }

    void setIsPausing(final Boolean isPausing) {
        mIsPausing = isPausing;
        mUnitButton.setOnClickListener(isPausing ? null : this);
        if (!isPausing)
            updateFields(); // Resuming: Update in case fiat changed in prefs
    }

    Boolean isPausing() {
        return mIsPausing;
    }

    void convert() {
        if (mIsToFiat)
            convertToFiat();
        else
            convertToCoin();
    }

    void convertToFiat() {
        if (mConverting || mIsPausing)
            return;

        mConverting = true;

        if (mService.isElements()) {
            // fiat == btc in elements
            mAmountText.setText(UI.getText(mAmountEdit));
            finishConversion();
            return;
        }

        try {
            mCoinValue = UI.parseCoinValue(mService, UI.getText(mAmountEdit));
            mFiatValue = mService.coinToFiat(mCoinValue);
            mAmountText.setText(mFiatValue.toFriendlyString());
        } catch (final Exception e) {
            UI.clear(mAmountEdit);
            UI.clear(mAmountText);
        }
        finishConversion();
    }

    private void convertToCoin() {
        if (mConverting || mIsPausing)
            return;

        mConverting = true;

        if (mService.isElements()) {
            // fiat == btc in elements
            mAmountEdit.setText(UI.getText(mAmountText));
            finishConversion();
            return;
        }

        try {
            mFiatValue = Fiat.parseFiat(mService.getFiatCurrency(), UI.getText(mAmountEdit));
            mCoinValue = mService.fiatToCoin(mFiatValue);
            mAmountText.setText(UI.formatCoinValue(mService, mCoinValue) + " " + mService.getBitcoinUnit());
        } catch (final Exception e) {
            UI.clear(mAmountEdit);
            UI.clear(mAmountText);
        }
        finishConversion();
    }

    private void finishConversion() {
        mConverting = false;
        if (mOnConversionFinishListener != null)
            mOnConversionFinishListener.conversionFinish();
    }

    @Override
    public void onClick(final View view) {
        mIsToFiat = !mIsToFiat;
        mConverting = true;
        updateFields();
        mConverting = false;
    }

    @Override
    public void beforeTextChanged(final CharSequence charSequence, final int i, final int i1, final int i2) {}

    @Override
    public void onTextChanged(final CharSequence charSequence, final int i, final int i1, final int i2) {
        convert();
    }

    @Override
    public void afterTextChanged(final Editable editable) {}

    interface OnConversionFinishListener {
        void conversionFinish();
    }
}
