package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;
import org.bitcoinj.utils.MonetaryFormat;

import java.math.BigDecimal;

/**
 * Created by Antonio Parrella on 11/16/16.
 * by inbitcoin
 */
class AmountFields {
    private EditText mAmountEdit;
    private EditText mAmountFiatEdit;
    private MonetaryFormat mBitcoinFormat;
    private boolean mConverting = false;
    private GaService mGaService;
    private Context mContext;
    private Boolean mPausing = false;

    interface OnConversionFinishListener {
        void conversionFinish();
    }

    private OnConversionFinishListener mOnConversionFinishListener;

    AmountFields(GaService gaService, Context context, View view, OnConversionFinishListener onConversionFinishListener) {
        mGaService = gaService;
        mContext = context;
        mOnConversionFinishListener = onConversionFinishListener;

        mAmountEdit = UI.find(view, R.id.sendAmountEditText);
        mAmountFiatEdit = UI.find(view, R.id.sendAmountFiatEditText);

        final TextView bitcoinScale = UI.find(view, R.id.sendBitcoinScaleText);
        final TextView bitcoinUnitText = UI.find(view, R.id.sendBitcoinUnitText);
        final FontAwesomeTextView fiatView = UI.find(view, R.id.sendFiatIcon);
        final String btcUnit = (String) mGaService.getUserConfig("unit");

        mBitcoinFormat = CurrencyMapper.mapBtcUnitToFormat(btcUnit);
        bitcoinScale.setText(Html.fromHtml(CurrencyMapper.mapBtcUnitToPrefix(btcUnit)));
        if (btcUnit == null || btcUnit.equals("bits"))
            bitcoinUnitText.setText("bits ");
        else
            bitcoinUnitText.setText(R.string.fa_btc_space);

        changeFiatIcon(fiatView, mGaService.getFiatCurrency());

        mAmountFiatEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                convertFiatToBtc();
            }

            @Override
            public void afterTextChanged(final Editable s) {

            }
        });

        mAmountEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                convertBtcToFiat();
            }

            @Override
            public void afterTextChanged(final Editable s) {

            }
        });
    }

    void setPause(Boolean value) {
        mPausing = value;
    }

    Boolean getPause() {
        return mPausing;
    }

    private void changeFiatIcon(final FontAwesomeTextView fiatIcon, final String currency) {

        final String converted = CurrencyMapper.map(currency);
        if (converted != null) {
            fiatIcon.setText(Html.fromHtml(converted + " "));
            fiatIcon.setAwesomeTypeface();
            fiatIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        } else {
            fiatIcon.setText(currency);
            fiatIcon.setDefaultTypeface();
            fiatIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }
    }

    void convertBtcToFiat() {
        convertBtcToFiat(mGaService.getFiatRate());
    }

    void convertBtcToFiat(final float exchangeRate) {
        if (mConverting || mPausing)
            return;

        mConverting = true;
        final Fiat exchangeFiat = Fiat.valueOf("???", new BigDecimal(exchangeRate).movePointRight(Fiat.SMALLEST_UNIT_EXPONENT)
                .toBigInteger().longValue());

        try {
            final ExchangeRate rate = new ExchangeRate(exchangeFiat);
            final Coin btcValue = mBitcoinFormat.parse(UI.getText(mAmountEdit));
            Fiat fiatValue = rate.coinToFiat(btcValue);
            // strip extra decimals (over 2 places) because that's what the old JS client does
            fiatValue = fiatValue.subtract(fiatValue.divideAndRemainder((long) Math.pow(10, Fiat.SMALLEST_UNIT_EXPONENT - 2))[1]);
            mAmountFiatEdit.setText(fiatValue.toPlainString());
        } catch (final ArithmeticException | IllegalArgumentException e) {
            if (UI.getText(mAmountEdit).equals(mContext.getString(R.string.send_max_amount)))
                mAmountFiatEdit.setText(mContext.getString(R.string.send_max_amount));
            else
                mAmountFiatEdit.setText("");
        }
        if (mOnConversionFinishListener != null) {
            mOnConversionFinishListener.conversionFinish();
        }
        mConverting = false;
    }

    private void convertFiatToBtc() {
        if (mConverting || mPausing)
            return;

        mConverting = true;
        final float exchangeRate = mGaService.getFiatRate();
        final Fiat exchangeFiat = Fiat.valueOf("???", new BigDecimal(exchangeRate).movePointRight(Fiat.SMALLEST_UNIT_EXPONENT)
                .toBigInteger().longValue());
        final ExchangeRate rate = new ExchangeRate(exchangeFiat);
        try {
            final Fiat fiatValue = Fiat.parseFiat("???", UI.getText(mAmountFiatEdit));
            mAmountEdit.setText(mBitcoinFormat.noCode().format(rate.fiatToCoin(fiatValue)));
        } catch (final ArithmeticException | IllegalArgumentException e) {
            mAmountEdit.setText("");
        }
        if (mOnConversionFinishListener != null) {
            mOnConversionFinishListener.conversionFinish();
        }
        mConverting = false;
    }
}
