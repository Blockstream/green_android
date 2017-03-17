package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.support.v7.widget.GridLayout;
import android.widget.TextView;

import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;

import java.math.BigDecimal;
import java.util.Locale;


class Exchanger implements AmountFields.OnConversionFinishListener {
    private final Context mContext;
    private final GaService mService;
    private final TextView mAmountFiatWithCommission;
    private final TextView mAmountBtcWithCommission;
    private final EditText mAmountFiatEdit;
    private final EditText mAmountBtcEdit;
    private final boolean mIsBuyPage;

    public static String TAG_EXCHANGER_TX_MEMO = "__exchanger_tx__";

    interface OnCalculateCommissionFinishListener {
        void calculateCommissionFinish();
    }

    private final OnCalculateCommissionFinishListener mOnCalculateCommissionFinishListener;

    Exchanger(Context context, GaService service, View mView, boolean isBuyPage, OnCalculateCommissionFinishListener listener) {
        mContext = context;
        mService = service;
        mIsBuyPage = isBuyPage;
        mOnCalculateCommissionFinishListener = listener;

        mAmountFiatWithCommission = UI.find(mView, R.id.amountFiatWithCommission);
        mAmountBtcWithCommission = UI.find(mView, R.id.amountBtcWithCommission);

        final TextView bitcoinUnitText = UI.find(mView, R.id.sendBitcoinUnitText2);
        UI.setCoinText(mService, bitcoinUnitText, null, null);

        final String currency = mService.getFiatCurrency();

        final FontAwesomeTextView fiatView = UI.find(mView, R.id.commissionFiatIcon);
        AmountFields.changeFiatIcon(fiatView, currency);

        if (GaService.IS_ELEMENTS) {
            bitcoinUnitText.setText(mService.getAssetSymbol() + " ");
            UI.hide((View) UI.find(mView, R.id.commissionFiatColumn));
        }

        mAmountFiatEdit = UI.find(mView, R.id.sendAmountFiatEditText);
        mAmountBtcEdit = UI.find(mView, R.id.sendAmountEditText);
        final String btnsValue = service.cfg().getString("exchanger_fiat_btns", "");
        if (!btnsValue.isEmpty()) {
            final String[] btnsValueArray = btnsValue.split(" ");
            final GridLayout gridLayout = UI.find(mView, R.id.gridLayout);
            for (final String value : btnsValueArray) {
                final Button btn = new Button(mContext);
                btn.setText(String.format("%s %s", value, currency));
                final GridLayout.Spec spec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                final GridLayout.LayoutParams param = new GridLayout.LayoutParams(spec, spec);
                btn.setLayoutParams(param);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mAmountFiatEdit.setText(value);
                        if (GaService.IS_ELEMENTS)
                            mAmountBtcEdit.setText(value);
                    }
                });
                gridLayout.addView(btn);
            }
        }
    }

    private String formatFiat(final float fiatAmount) {
        return String.format(Locale.US, "%.2f", fiatAmount);
    }

    void sellBtc(final float fiatAmount) {
        final String newFiatBill = formatFiat(getFiatInBill() + fiatAmount);
        mService.cfg().edit().putString("exchanger_fiat_in_bill", newFiatBill).apply();
    }

    void buyBtc(final float fiatAmount) {
        final String newFiatBill = formatFiat(getFiatInBill() - fiatAmount);
        mService.cfg().edit().putString("exchanger_fiat_in_bill", newFiatBill).apply();
    }

    float getFiatInBill() {
        float fiatBill = 0;
        final String fiatBillTxt = mService.cfg().getString("exchanger_fiat_in_bill", "");
        if (!fiatBillTxt.isEmpty())
            fiatBill = Float.valueOf(fiatBillTxt);
        return fiatBill;
    }

    private String getCommissionConfig(final String suffix, final String def) {
        final String prefix = mIsBuyPage ? "buy_commission_" : "sell_commission_";
        final String value = mService.cfg().getString(prefix + suffix, def);
        return value.isEmpty() ? def : value;
    }

    public String getAmountWithCommission() {
        return mAmountBtcWithCommission.getText().toString();
    }

    private void calculateAmountWithCommission() {
        // fixed commission
        final String commission = getCommissionConfig("fixed_coin", "0");
        final Coin fixedCommissionBtc = Coin.valueOf(Long.valueOf(commission));

        // percentage commission
        final String percentage = getCommissionConfig("percentage", "");
        final Integer commissionPerc = percentage.isEmpty() ? 0 : Integer.valueOf(percentage);

        if (GaService.IS_ELEMENTS) {
            final String amountBtcTxt = mAmountBtcEdit.getText().toString();

            final Coin coin = amountBtcTxt.isEmpty() ? Coin.ZERO : UI.parseCoinValue(mService, amountBtcTxt);

            long amountBtcWithCommission = coin.getValue() * (100 - commissionPerc) / 100 - fixedCommissionBtc.getValue();

            final Coin amountWithCommission = Coin.valueOf(amountBtcWithCommission);

            final String value = UI.formatCoinValue(mService, amountWithCommission);
            mAmountBtcWithCommission.setText(value);
            mAmountFiatWithCommission.setText(value);
        } else {
            final float fixedCommissionFiat = convertBtcToFiat(fixedCommissionBtc);

            // amount fiat
            final String amountFiatTxt = mAmountFiatEdit.getText().toString();
            if (amountFiatTxt.isEmpty()) {
                mAmountBtcWithCommission.setText("0");
                mAmountFiatWithCommission.setText("0");
                return;
            }
            final float amountFiat = Float.valueOf(amountFiatTxt);
            float amountFiatWithCommission = (amountFiat / 100) * (100 - commissionPerc) - fixedCommissionFiat;
            if (amountFiatWithCommission < 0) {
                mAmountBtcWithCommission.setText("0");
                mAmountFiatWithCommission.setText("0");
                return;
            }
            mAmountFiatWithCommission.setText(formatFiat(amountFiatWithCommission));

            // amount btc
            final String amountBtcTxt = mAmountBtcEdit.getText().toString();
            if (amountBtcTxt.isEmpty())
                return;

            final Coin coin = UI.parseCoinValue(mService, amountBtcTxt);
            long amountBtcWithCommission = (coin.getValue() / 100) * (100 - commissionPerc) - fixedCommissionBtc.getValue();
            final Coin amountWithCommission = Coin.valueOf(amountBtcWithCommission);
            mAmountBtcWithCommission.setText(UI.formatCoinValue(mService, amountWithCommission));
        }

        if (mOnCalculateCommissionFinishListener != null)
            mOnCalculateCommissionFinishListener.calculateCommissionFinish();
    }

    @Override
    public void conversionFinish() {
        calculateAmountWithCommission();
    }

    private float convertBtcToFiat(final Coin btcValue) {
        try {
            Fiat fiatValue = mService.getFiatRate().coinToFiat(btcValue);
            // strip extra decimals (over 2 places) because that's what the old JS client does
            fiatValue = fiatValue.subtract(fiatValue.divideAndRemainder((long) Math.pow(10, Fiat.SMALLEST_UNIT_EXPONENT - 2))[1]);
            return Float.valueOf(fiatValue.toPlainString());
        } catch (final ArithmeticException | IllegalArgumentException e) {
            return -1;
        }
    }
}
