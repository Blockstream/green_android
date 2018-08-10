package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.support.v7.widget.GridLayout;
import android.widget.TextView;

import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.Fiat;

import java.util.Locale;


class Exchanger implements AmountFields.OnConversionFinishListener {
    private final Context mContext;
    private final GaService mService;
    private final TextView mAmountFiatWithCommission;
    private final TextView mAmountBtcWithCommission;
    private final EditText mAmountFiatEdit;
    private final EditText mAmountBtcEdit;
    private final boolean mIsBuyPage;

    public static final String TAG_EXCHANGER_TX_MEMO = "__exchanger_tx__";

    interface OnCalculateCommissionFinishListener {
        void calculateCommissionFinish();
    }

    private final OnCalculateCommissionFinishListener mOnCalculateCommissionFinishListener;

    Exchanger(final Context context, final GaService service, final View mView, final boolean isBuyPage, final OnCalculateCommissionFinishListener listener) {
        mContext = context;
        mService = service;
        mIsBuyPage = isBuyPage;
        mOnCalculateCommissionFinishListener = listener;

        mAmountFiatWithCommission = UI.find(mView, R.id.amountFiatWithCommission);
        mAmountBtcWithCommission = UI.find(mView, R.id.amountBtcWithCommission);

        final FontAwesomeTextView bitcoinUnitText = UI.find(mView, R.id.sendBitcoinUnitText2);
        UI.setCoinText(mService, bitcoinUnitText, null, null);

        final String currency = mService.getFiatCurrency();

        final FontAwesomeTextView fiatView = UI.find(mView, R.id.commissionFiatIcon);
        AmountFields.changeFiatIcon(fiatView, currency);

        if (mService.isElements()) {
            bitcoinUnitText.setText(String.format("%s ", mService.getAssetSymbol()));
            UI.hide(UI.find(mView, R.id.commissionFiatColumn));
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
                    public void onClick(final View view) {
                        mAmountFiatEdit.setText(value);
                        if (mService.isElements())
                            mAmountBtcEdit.setText(value);
                    }
                });
                gridLayout.addView(btn);
            }
        }
    }

    private static String formatFiat(final double fiatAmount) {
        return String.format(Locale.US, "%.2f", fiatAmount);
    }

    void sellBtc(final double fiatAmount) {
        final String newFiatBill = formatFiat(getFiatInBill() + fiatAmount);
        mService.cfg().edit().putString("exchanger_fiat_in_bill", newFiatBill).apply();
    }

    void buyBtc(final double fiatAmount) {
        final String newFiatBill = formatFiat(getFiatInBill() - fiatAmount);
        mService.cfg().edit().putString("exchanger_fiat_in_bill", newFiatBill).apply();
    }

    double getFiatInBill() {
        final String fiatBillTxt = mService.cfg().getString("exchanger_fiat_in_bill", "");
        return fiatBillTxt.isEmpty() ? 0.0 : Double.valueOf(fiatBillTxt);
    }

    private String getCommissionConfig(final String suffix, final String def) {
        final String prefix = mIsBuyPage ? "buy_commission_" : "sell_commission_";
        final String value = mService.cfg().getString(prefix + suffix, def);
        return value.isEmpty() ? def : value;
    }

    public double getAmountWithCommission() {
        return Double.valueOf(UI.getText(mAmountBtcWithCommission));
    }

    private void calculateAmountWithCommission() {
        // fixed commission
        final String commission = getCommissionConfig("fixed_coin", "0");
        final Coin fixedCommissionBtc = Coin.valueOf(Long.valueOf(commission));

        // percentage commission
        final Double percentage = 100.0 - Double.valueOf(getCommissionConfig("percentage", "0"));


        if (mService.isElements()) {
            final String amountBtcTxt = UI.getText(mAmountBtcEdit);

            final Coin coin = amountBtcTxt.isEmpty() ? Coin.ZERO : UI.parseCoinValue(mService, amountBtcTxt);

            final double amountBtcWithCommission = coin.getValue() * percentage / 100.0 - fixedCommissionBtc.getValue();

            final Coin amountWithCommission = Coin.valueOf((long) amountBtcWithCommission);

            final String value = UI.formatCoinValue(mService, amountWithCommission);
            mAmountBtcWithCommission.setText(value);
            mAmountFiatWithCommission.setText(value);
        } else {
            final double fixedCommissionFiat = convertBtcToFiat(fixedCommissionBtc);

            // amount fiat
            final String amountFiatTxt = UI.getText(mAmountFiatEdit);
            boolean isValid = !amountFiatTxt.isEmpty();
            double amountFiat = 0;
            try {
                if (isValid)
                    amountFiat = Double.valueOf(amountFiatTxt);
            } catch (final Exception e) {
                isValid = false;
            }

            if (!isValid) {
                mAmountBtcWithCommission.setText("0");
                mAmountFiatWithCommission.setText("0");
                return;
            }
            final double amountFiatWithCommission = (amountFiat / 100.0) * percentage - fixedCommissionFiat;
            if (amountFiatWithCommission < 0) {
                mAmountBtcWithCommission.setText("0");
                mAmountFiatWithCommission.setText("0");
                return;
            }
            mAmountFiatWithCommission.setText(formatFiat(amountFiatWithCommission));

            // amount btc
            final String amountBtcTxt = UI.getText(mAmountBtcEdit);
            if (amountBtcTxt.isEmpty())
                return;

            final Coin coin = UI.parseCoinValue(mService, amountBtcTxt);
            final double amountBtcWithCommission = coin.getValue() / 100.0 * percentage - fixedCommissionBtc.getValue();
            final Coin amountWithCommission = Coin.valueOf((long) amountBtcWithCommission);
            mAmountBtcWithCommission.setText(UI.formatCoinValue(mService, amountWithCommission));
        }

        if (mOnCalculateCommissionFinishListener != null)
            mOnCalculateCommissionFinishListener.calculateCommissionFinish();
    }

    @Override
    public void conversionFinish() {
        calculateAmountWithCommission();
    }

    private double convertBtcToFiat(final Coin btcValue) {
        try {
            Fiat fiatValue = mService.getFiatRate().coinToFiat(btcValue);
            // strip extra decimals (over 2 places) because that's what the old JS client does
            fiatValue = fiatValue.subtract(fiatValue.divideAndRemainder((long) Math.pow(10, Fiat.SMALLEST_UNIT_EXPONENT - 2))[1]);
            return Double.valueOf(fiatValue.toPlainString());
        } catch (final ArithmeticException | IllegalArgumentException e) {
            return -1;
        }
    }
}
