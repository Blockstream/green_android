package com.greenaddress.greenapi.model;

import com.greenaddress.greenapi.data.AssetInfoData;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenbits.ui.UI;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

import static com.greenaddress.greenapi.Session.getSession;

public class Conversion {

    public static String getFiatCurrency() {
        return getSession().getSettings().getPricing().getCurrency();
    }

    public static String getBitcoinOrLiquidUnit() {
        final int index = Math.max(UI.UNIT_KEYS_LIST.indexOf(getUnitKey()), 0);
        if (getSession().getNetworkData().getLiquid()) {
            return UI.LIQUID_UNITS[index];
        } else {
            return UI.UNITS[index];
        }
    }

    public static String getUnitKey() {
        final String unit = getSession().getSettings().getUnit();
        return toUnitKey(unit);
    }

    public static String toUnitKey(final String unit) {
        if (!Arrays.asList(UI.UNITS).contains(unit))
            return UI.UNITS[0].toLowerCase(Locale.US);
        return unit.equals("\u00B5BTC") ? "ubtc" : unit.toLowerCase(Locale.US);
    }

    public static String getFiat(final long satoshi, final boolean withUnit) throws Exception {
        return getFiat(getSession().convertBalance(satoshi), withUnit);
    }

    public static String getBtc(final long satoshi, final boolean withUnit) throws Exception {
        return getBtc(getSession().convertBalance(satoshi), withUnit);
    }

    public static String getAsset(final long satoshi,  final String asset, final AssetInfoData assetInfo,
                                  final boolean withUnit) throws Exception {
        final AssetInfoData assetInfoData = assetInfo != null ? assetInfo : new AssetInfoData(asset);
        final BalanceData balance = new BalanceData();
        balance.setSatoshi(satoshi);
        balance.setAssetInfo(assetInfoData);
        final BalanceData converted = getSession().convertBalance(balance);
        return getAsset(converted, withUnit);
    }

    public static String getFiat(final BalanceData balanceData, final boolean withUnit) {
        try {
            final Double number = Double.parseDouble(balanceData.getFiat());
            return getNumberFormat(2).format(number) + (withUnit ? " " + getFiatCurrency() : "");
        } catch (final Exception e) {
            return "N.A." + (withUnit ? " " + getFiatCurrency() : "");
        }
    }

    public static String getBtc(final BalanceData balanceData, final boolean withUnit) {
        final String converted = balanceData.toObjectNode().get(getUnitKey()).asText();
        final Double number = Double.parseDouble(converted);
        return getNumberFormat().format(number) + (withUnit ? " " + getBitcoinOrLiquidUnit() : "");
    }

    public static NumberFormat getNumberFormat() {
        switch (getUnitKey()) {
        case "btc":
            return getNumberFormat(8);
        case "mbtc":
            return getNumberFormat(5);
        case "ubtc":
        case "bits":
            return getNumberFormat(2);
        default:
            return getNumberFormat(0);
        }
    }

    public static NumberFormat getNumberFormat(final int decimals) {
        return getNumberFormat(decimals, Locale.getDefault());
    }

    public static NumberFormat getNumberFormat(final int decimals, final Locale locale) {
        final NumberFormat instance = NumberFormat.getInstance(locale);
        instance.setMinimumFractionDigits(decimals);
        instance.setMaximumFractionDigits(decimals);
        return instance;
    }

    public static String getAsset(final BalanceData balanceData, final boolean withUnit) {
        final AssetInfoData info = balanceData.getAssetInfo();
        final Double number = Double.parseDouble(balanceData.getAssetValue());
        final String ticker = info.getTicker() != null ? info.getTicker() : "";
        return getNumberFormat(info.getPrecision()).format(number) + (withUnit ? " " + ticker : "");
    }
}
