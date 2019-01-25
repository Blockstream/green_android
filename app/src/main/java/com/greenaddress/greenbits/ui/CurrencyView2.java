package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Locale;


public class CurrencyView2 extends RelativeLayout implements View.OnClickListener, TextWatcher {

    private EditText mAmountEdit;
    private TextView mDisplayText;
    private Button mUnitButton;
    private ObjectNode mData;
    private boolean mSendAll = false;
    private boolean mEnabled = true;
    private String mUnit = "BTC";
    private String mCurrency = "USD";
    private boolean mConverting = false;
    private boolean mIsFiat = false;
    private BalanceConversionProvider mProvider;

    public CurrencyView2(final Context context) {
        super(context);
        setup(context);
    }

    public CurrencyView2(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    private void setup(final Context context) {
        final LayoutInflater inflater = (LayoutInflater) context
                                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_currency, this, true);
        final View view = getRootView();
        mAmountEdit = UI.find(view, R.id.amountEditText);
        mDisplayText = UI.find(view, R.id.amountText);
        mUnitButton = UI.find(view, R.id.unitButton);
        mAmountEdit.addTextChangedListener(this);
        mUnitButton.setOnClickListener(this);
    }

    public void setAmounts(final ObjectNode data) {
        if (mData == null || mData.get("satoshi").asLong() != data.get("satoshi").asLong()) {
            mData = data;
            updateFields(true);
        }
    }

    public void setSendAll(boolean sendAll) { mSendAll = sendAll; updateFields(true); }

    public long getSatoshi() { return mData == null ? 0 : mData.get("satoshi").asLong(); }

    public void setEnabled(final boolean enabled) {
        mEnabled = enabled;
        mAmountEdit.setEnabled(mEnabled && !mSendAll);
        mAmountEdit.setTextColor(ResourcesCompat.getColor(getResources(), R.color.grey_light, null));
    }

    public void onPause() {
        mProvider = null;
        mUnitButton.setOnClickListener(null);
    }

    public void onResume(BalanceConversionProvider provider, final String unit, final String currency) {
        mProvider = provider;
        mUnit = unit;
        mCurrency = currency;
        mIsFiat = !mIsFiat; // Toggle back to original setting to update amount/button state
        mUnitButton.setOnClickListener(this);
        onClick(mUnitButton);
    }

    public boolean isFiat() { return mIsFiat; }

    private String getUnit() {
        return mUnit.equals("\u00B5BTC") ? "ubtc" : mUnit.toLowerCase(Locale.US);
    }

    private String getFiat() { return mData == null ? null : mData.get("fiat").asText(); }
    private String getBTC() { return mData == null ? null : mData.get(getUnit()).asText(); }

    private void updateFields(boolean updateEditable) {
        mConverting = true;

        if (mSendAll || mData != null) {
            final String fiat = getFiat(), btc = getBTC();

            if (updateEditable)
                mAmountEdit.setText(mSendAll ? getResources().getString(R.string.id_all) : isFiat() ? fiat : btc);
            if (mData == null)
                mDisplayText.setText("");
            else {
                if (mSendAll)
                    mDisplayText.setText(String.format("%s %s / %s %s", btc, mUnit, fiat, mCurrency));
                else
                    mDisplayText.setText((isFiat() ? btc : fiat) + " " + (isFiat() ? mUnit : mCurrency));
            }
        }
        mAmountEdit.setEnabled(mEnabled && !mSendAll);
        mUnitButton.setVisibility(mSendAll ? View.INVISIBLE : View.VISIBLE);

        mConverting = false;
    }

    @Override
    public void onClick(final View view) {
        mConverting = true;

        // Toggle unit display and selected state
        mIsFiat = !mIsFiat;
        mUnitButton.setText(mIsFiat ? mCurrency : mUnit);
        mUnitButton.setPressed(!mIsFiat);
        mUnitButton.setSelected(!mIsFiat);
        updateFields(true);

        mConverting = false;
        if (mListener != null)
            mListener.onCurrencyChange();
    }

    private Listener mListener;
    public void setListener(final Listener listener) {
        mListener = listener;
    }

    @Override
    public void beforeTextChanged(final CharSequence cs, final int i, final int i1, final int i2) { }

    @Override
    public void onTextChanged(final CharSequence cs, final int i, final int i1, final int i2) {
        if (!mConverting && mProvider != null) {
            final String key = isFiat() ? "fiat" : getUnit();
            final String value = mAmountEdit.getText().toString();
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode amount = mapper.createObjectNode();
            amount.put(key, value.isEmpty() ? "0" : value);
            final ObjectNode newData = mProvider.convertAmount(amount);
            if (mData == null || (newData != null && newData.get("satoshi").asLong() != getSatoshi())) {
                mData = newData;
                if (mData != null)
                    updateFields(false);
            }
        }
    }

    @Override
    public void afterTextChanged(final Editable editable) {
        if (!mConverting && mProvider != null)
            mProvider.amountEntered();
    }

    interface BalanceConversionProvider {
        ObjectNode convertAmount(final ObjectNode amount);
        void amountEntered();
    }

    interface Listener {
        void onCurrencyChange();
    }
}