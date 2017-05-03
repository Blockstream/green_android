package com.greenaddress.greenbits.ui.preferences;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.text.InputType;
import android.widget.Toast;

import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import org.bitcoinj.core.Coin;


public class ExchangerPreferenceFragment extends GAPreferenceFragment implements Preference.OnPreferenceChangeListener {


    private EditTextPreference mBuyCommissionPerc;
    private EditTextPreference mBuyCommissionFixed;
    private EditTextPreference mSellCommissionPerc;
    private EditTextPreference mSellCommissionFixed;
    private EditTextPreference mFiatBtns;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_exchanger);
        setHasOptionsMenu(true);

        final String btcUnit = mService.getBitcoinUnit();

        final String buyFixed = mService.cfg().getString("buy_commission_fixed_coin", "");
        final String buyFixedCurrentFormat;
        if (!buyFixed.isEmpty()) {
            final Coin coinBuy = Coin.valueOf(Long.valueOf(buyFixed));
            buyFixedCurrentFormat = UI.formatCoinValue(mService, coinBuy);
        } else
            buyFixedCurrentFormat = "";

        final String sellFixed = mService.cfg().getString("sell_commission_fixed_coin", "");
        final String sellFixedCurrentFormat;
        if (!sellFixed.isEmpty()) {
            final Coin coinSell = Coin.valueOf(Long.valueOf(sellFixed));
            sellFixedCurrentFormat = UI.formatCoinValue(mService, coinSell);
        } else
            sellFixedCurrentFormat = "";

        mBuyCommissionPerc = find("buy_commission_percentage");
        mBuyCommissionPerc.setOnPreferenceChangeListener(this);

        mSellCommissionPerc = find("sell_commission_percentage");
        mSellCommissionPerc.setOnPreferenceChangeListener(this);

        // element used only for edit
        mBuyCommissionFixed = find("buy_commission_fixed");
        mBuyCommissionFixed.setText(buyFixedCurrentFormat);
        mBuyCommissionFixed.setOnPreferenceChangeListener(this);

        // element used only for edit
        mSellCommissionFixed = find("sell_commission_fixed");
        mSellCommissionFixed.setText(sellFixedCurrentFormat);
        mSellCommissionFixed.setOnPreferenceChangeListener(this);

        mFiatBtns = find("exchanger_fiat_btns");
        mFiatBtns.getEditText().setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        mFiatBtns.setOnPreferenceChangeListener(this);

        if (GaService.IS_ELEMENTS) {
            mBuyCommissionFixed.setTitle(R.string.buy_commission_fixed_elements);
            mSellCommissionFixed.setTitle(R.string.sell_commission_fixed_elements);
            mBuyCommissionPerc.setTitle(R.string.buy_commission_percentage_elements);
            mSellCommissionPerc.setTitle(R.string.sell_commission_percentage_elements);
        }

        mBuyCommissionFixed.setSummary(String.format("%s %s", getString(R.string.fixedCommissionDesc), btcUnit));
        mSellCommissionFixed.setSummary(String.format("%s %s", getString(R.string.fixedCommissionDesc), btcUnit));
    }

    private boolean commissionPerc(final String value) {
        if (value.isEmpty())
            return true;
        final float floatValue = Float.valueOf(value);
        if (floatValue < 0 || floatValue > 100) {
            UI.popup(getActivity(), R.string.enterValidValue, android.R.string.ok)
                    .content(R.string.enterValidPerc).build().show();
            return false;
        }
        return true;
    }

    private boolean commissionBuyFixed(final String value) {
        if (value.isEmpty()) {
            mService.cfg().edit().putString("buy_commission_fixed_coin", "").apply();
            return true;
        }
        final Coin coin = UI.parseCoinValue(mService, value);
        mService.cfg().edit().putString("buy_commission_fixed_coin", coin.toString()).apply();
        mBuyCommissionFixed.setText(value);
        return false;
    }

    private boolean commissionSellFixed(final String value) {
        if (value.isEmpty()) {
            mService.cfg().edit().putString("sell_commission_fixed_coin", "").apply();
            return true;
        }
        final Coin coin = UI.parseCoinValue(mService, value);
        mService.cfg().edit().putString("sell_commission_fixed_coin", coin.toString()).apply();
        mSellCommissionFixed.setText(value);
        return false;
    }

    @Override
    public boolean onPreferenceChange(final Preference preference, final Object newValue) {
        if (preference == mBuyCommissionPerc || preference == mSellCommissionPerc) {
            return commissionPerc((String) newValue);
        } else if (preference == mBuyCommissionFixed) {
            return commissionBuyFixed((String) newValue);
        } else if (preference == mSellCommissionFixed) {
            return commissionSellFixed((String) newValue);
        } else if (preference == mFiatBtns) {
            final String strValue = (String) newValue;
            try {
                final String[] btnsValueArray = strValue.split(" ");
                for (final String value : btnsValueArray) {
                    UI.parseCoinValue(mService, value);
                }
                return true;
            } catch (final Exception e) {
                e.printStackTrace();
                UI.toast(getActivity(), "Invalid format", Toast.LENGTH_SHORT);
            }
        }
        return false;
    }
}
