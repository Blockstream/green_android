package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.text.Html;
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
    private boolean mConverting = false;
    private GaService mGaService;
    private Context mContext;
    private Boolean mIsPausing = false;

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

        bitcoinScale.setText(CurrencyMapper.mapBtcUnitToPrefix(btcUnit));
        if (btcUnit == null || btcUnit.equals("bits"))
            bitcoinUnitText.setText("bits ");
        else
            bitcoinUnitText.setText(R.string.fa_btc_space);

        changeFiatIcon(fiatView, mGaService.getFiatCurrency());

        mAmountFiatEdit.addTextChangedListener(new UI.TextWatcher() {
            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                convertFiatToBtc();
            }
        });

        mAmountEdit.addTextChangedListener(new UI.TextWatcher() {
            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                convertBtcToFiat();
            }
        });
    }

    private MonetaryFormat getFormat() {
        final String btcUnit = (String) mGaService.getUserConfig("unit");
        return CurrencyMapper.mapBtcUnitToFormat(btcUnit);
    }

    void setIsPausing(Boolean isPausing) {
        mIsPausing = isPausing;
    }

    Boolean isPausing() {
        return mIsPausing;
    }

    public static void changeFiatIcon(final FontAwesomeTextView fiatIcon, final String currency) {
        final String symbol;
        switch (currency) {
            case "USD": symbol = "&#xf155; "; break;
            case "AUD": symbol = "&#xf155; "; break;
            case "CAD": symbol = "&#xf155; "; break;
            case "EUR": symbol = "&#xf153; "; break;
            case "CNY": symbol = "&#xf157; "; break;
            case "GBP": symbol = "&#xf154; "; break;
            case "ILS": symbol = "&#xf20b; "; break;
            case "RUB": symbol = "&#xf158; "; break;
            case "BRL": symbol = "R&#xf155; "; break;
            default:
                fiatIcon.setText(currency);
                fiatIcon.setDefaultTypeface();
                fiatIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                return;
        }
        fiatIcon.setText(Html.fromHtml(symbol));
        fiatIcon.setAwesomeTypeface();
        fiatIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
    }

    public static String formatValue(final Coin value, final String unit) {
        final MonetaryFormat mf = CurrencyMapper.mapBtcUnitToFormat(unit);
        return mf.noCode().format(value).toString();
    }

    void convertBtcToFiat() {
        convertBtcToFiat(mGaService.getFiatRate());
    }

    void convertBtcToFiat(final float exchangeRate) {
        if (mConverting || mIsPausing)
            return;

        mConverting = true;
        final Fiat exchangeFiat = Fiat.valueOf("???", new BigDecimal(exchangeRate).movePointRight(Fiat.SMALLEST_UNIT_EXPONENT)
                .toBigInteger().longValue());

        try {
            final ExchangeRate rate = new ExchangeRate(exchangeFiat);
            final Coin btcValue = getFormat().parse(UI.getText(mAmountEdit));
            Fiat fiatValue = rate.coinToFiat(btcValue);
            // strip extra decimals (over 2 places) because that's what the old JS client does
            fiatValue = fiatValue.subtract(fiatValue.divideAndRemainder((long) Math.pow(10, Fiat.SMALLEST_UNIT_EXPONENT - 2))[1]);
            mAmountFiatEdit.setText(fiatValue.toPlainString());
        } catch (final ArithmeticException | IllegalArgumentException e) {
            final String maxAmount = mContext.getString(R.string.send_max_amount);
            if (UI.getText(mAmountEdit).equals(maxAmount))
                mAmountFiatEdit.setText(maxAmount);
            else
                mAmountFiatEdit.setText("");
        }
        finishConversion();
    }

    private void convertFiatToBtc() {
        if (mConverting || mIsPausing)
            return;

        mConverting = true;
        final float exchangeRate = mGaService.getFiatRate();
        final Fiat exchangeFiat = Fiat.valueOf("???", new BigDecimal(exchangeRate).movePointRight(Fiat.SMALLEST_UNIT_EXPONENT)
                .toBigInteger().longValue());
        final ExchangeRate rate = new ExchangeRate(exchangeFiat);
        try {
            final Fiat fiatValue = Fiat.parseFiat("???", UI.getText(mAmountFiatEdit));
            mAmountEdit.setText(getFormat().noCode().format(rate.fiatToCoin(fiatValue)));
        } catch (final ArithmeticException | IllegalArgumentException e) {
            mAmountEdit.setText("");
        }
        finishConversion();
    }

    private void finishConversion() {
        if (mOnConversionFinishListener != null)
            mOnConversionFinishListener.conversionFinish();
        mConverting = false;
    }
}
