package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.text.Html;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;

import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.Fiat;

class AmountFields {
    private final EditText mAmountEdit;
    private final EditText mAmountFiatEdit;
    private final FontAwesomeTextView mFiatView;
    private boolean mConverting;
    private final GaService mGaService;
    private final Context mContext;
    private Boolean mIsPausing = false;

    interface OnConversionFinishListener {
        void conversionFinish();
    }

    private final OnConversionFinishListener mOnConversionFinishListener;

    AmountFields(final GaService gaService, final Context context, final View view, final OnConversionFinishListener onConversionFinishListener) {
        mGaService = gaService;
        mContext = context;
        mOnConversionFinishListener = onConversionFinishListener;

        mAmountEdit = UI.find(view, R.id.sendAmountEditText);
        mAmountFiatEdit = UI.find(view, R.id.sendAmountFiatEditText);
        mFiatView = UI.find(view, R.id.sendFiatIcon);

        final FontAwesomeTextView bitcoinUnitText = UI.find(view, R.id.sendBitcoinUnitText);
        UI.setCoinText(mGaService, bitcoinUnitText, null, null);

        mAmountFiatEdit.addTextChangedListener(new UI.TextWatcher() {
            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                convertFiatToBtc();
            }
        });

        mAmountEdit.addTextChangedListener(new UI.TextWatcher() {
            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                if (mGaService.hasFiatRate())
                    convertBtcToFiat();
            }
        });

        updateFiatFields();
    }

    private void updateFiatFields() {
        if (GaService.IS_ELEMENTS) {
            UI.hide(mAmountFiatEdit, mFiatView);
            return;
        }

        changeFiatIcon(mFiatView, mGaService.getFiatCurrency());

        if (!mGaService.hasFiatRate()) {
            // Disable fiat editing
            mAmountFiatEdit.setText("N/A");
            UI.disable(mAmountFiatEdit);
        } else {
            if (UI.getText(mAmountFiatEdit).equals("N/A"))
                convertBtcToFiat(); // Fiat setting changed, recalc it
        }
    }

    void setIsPausing(final Boolean isPausing) {
        mIsPausing = isPausing;
        if (!isPausing)
            updateFiatFields(); // Resuming: Update in case fiat changed in prefs
    }

    Boolean isPausing() {
        return mIsPausing;
    }

    public static void changeFiatIcon(final FontAwesomeTextView fiatIcon, final String currency) {
        final String symbol;
        switch (currency) {
            case "AUD": symbol = "&#xf155; "; break;
            case "BRL": symbol = "R&#xf155; "; break;
            case "CAD": symbol = "&#xf155; "; break;
            case "CNY": symbol = "&#xf157; "; break;
            case "EUR": symbol = "&#xf153; "; break;
            case "GBP": symbol = "&#xf154; "; break;
            case "ILS": symbol = "&#xf20b; "; break;
            case "NZD": symbol = "&#xf155; "; break;
            case "RUB": symbol = "&#xf158; "; break;
            case "USD": symbol = "&#xf155; "; break;
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

    void convertBtcToFiat() {
        if (mConverting || mIsPausing)
            return;

        mConverting = true;

        if (GaService.IS_ELEMENTS) {
            // limit decimal places (TODO should work for BTC, but needs testing)
            try {
                final int selectionStart = mAmountEdit.getSelectionStart();
                final String old = UI.getText(mAmountEdit);
                String adjusted = old;
                if (!old.isEmpty() && Character.isDigit(old.charAt(selectionStart-1))) {
                    // don't adjust if the added char is not a digit,
                    // to still allow inserting commas/dots
                    adjusted = UI.formatCoinValue(mGaService, UI.parseCoinValue(mGaService, old));
                }
                if (old.length() > adjusted.length() &&
                        Double.parseDouble(old) != Double.parseDouble(adjusted)) {
                    // Don't ever make the string longer, for example '1.0' -> '1.00'
                    // And adjust only if the values differ, to allow adding trailing zeroes,
                    // otherwise entering values like 0.04 is not possible.
                    mAmountEdit.setText(adjusted);
                    try {
                        mAmountEdit.setSelection(selectionStart, selectionStart);
                    } catch (final IndexOutOfBoundsException e) {
                        mAmountEdit.setSelection(adjusted.length(), adjusted.length());
                    }
                }
            } catch (final NumberFormatException | IndexOutOfBoundsException e) {
            }
        }

        if (GaService.IS_ELEMENTS) {
            // fiat == btc in elements
            mAmountFiatEdit.setText(UI.getText(mAmountEdit));
            finishConversion();
            return;
        }

        try {
            final Coin btcValue = UI.parseCoinValue(mGaService, UI.getText(mAmountEdit));
            mAmountFiatEdit.setText(mGaService.coinToFiat(btcValue));
        } catch (final ArithmeticException | IllegalArgumentException e) {
            final String maxAmount = mContext.getString(R.string.all);
            if (UI.getText(mAmountEdit).equals(maxAmount))
                mAmountFiatEdit.setText(maxAmount);
            else
                UI.clear(mAmountFiatEdit);
        }
        finishConversion();
    }

    private void convertFiatToBtc() {
        if (mConverting || mIsPausing)
            return;

        mConverting = true;

        if (GaService.IS_ELEMENTS) {
            // fiat == btc in elements
            mAmountEdit.setText(UI.getText(mAmountFiatEdit));
            finishConversion();
            return;
        }

        try {
            final Fiat fiatValue = Fiat.parseFiat("???", UI.getText(mAmountFiatEdit));
            mAmountEdit.setText(UI.formatCoinValue(mGaService, mGaService.getFiatRate().fiatToCoin(fiatValue)));
        } catch (final ArithmeticException | IllegalArgumentException e) {
            UI.clear(mAmountEdit);
        }
        finishConversion();
    }

    private void finishConversion() {
        if (mOnConversionFinishListener != null)
            mOnConversionFinishListener.conversionFinish();
        mConverting = false;
    }
}
