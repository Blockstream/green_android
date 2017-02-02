package com.greenaddress.greenbits.ui;

import android.text.Html;

import org.bitcoinj.utils.MonetaryFormat;

class CurrencyMapper {
    private static final MonetaryFormat MBTC = new MonetaryFormat().shift(3).minDecimals(2).repeatOptionalDecimals(1, 3);

    public static String mapBtcUnitToPrefix(final String btcUnit) {
        if (btcUnit != null) {
            if (btcUnit.equals("mBTC"))
                return "m";
            if (btcUnit.equals(Html.fromHtml("&micro;").toString() + "BTC"))
                return "&micro;"; // bits or uBTC or default
        }
        return ""; // Everything else
    }

    public static MonetaryFormat mapBtcUnitToFormat(final String btcUnit) {
        if (btcUnit != null) {
            if (btcUnit.equals("BTC"))
                return MonetaryFormat.BTC;
            if (btcUnit.equals("mBTC"))
                return MBTC;
            if (btcUnit.equals(Html.fromHtml("&micro;").toString() + "BTC"))
                return MonetaryFormat.UBTC;
        }
        return MonetaryFormat.UBTC.code(6, "bits");
    }

    public static String mapBtcFormatToPrefix(final MonetaryFormat bitcoinFormat) {
        if (bitcoinFormat.code().equals(MonetaryFormat.CODE_BTC))
            return "";
        if (bitcoinFormat.code().equals(MonetaryFormat.CODE_MBTC))
            return "m";
        if (bitcoinFormat.code().equals(MonetaryFormat.CODE_UBTC))
            return "&micro;";
        if (bitcoinFormat.code().equals("bits"))
            return "";
        return null;
    }
}
