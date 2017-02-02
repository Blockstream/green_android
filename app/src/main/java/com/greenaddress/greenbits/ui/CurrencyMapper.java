package com.greenaddress.greenbits.ui;

import org.bitcoinj.utils.MonetaryFormat;

class CurrencyMapper {
    private static final String MICRO = "\u00B5";
    private static final String MICRO_BTC = "\u00B5BTC";
    private static final MonetaryFormat MBTC = new MonetaryFormat().shift(3).minDecimals(2).repeatOptionalDecimals(1, 3);

    public static String mapBtcUnitToPrefix(final String btcUnit) {
        if (btcUnit != null) {
            if (btcUnit.equals("mBTC"))
                return "m";
            if (btcUnit.equals(MICRO_BTC))
                return MICRO;
        }
        return ""; // Everything else
    }

    public static MonetaryFormat mapBtcUnitToFormat(final String btcUnit) {
        if (btcUnit != null) {
            if (btcUnit.equals("BTC"))
                return MonetaryFormat.BTC;
            if (btcUnit.equals("mBTC"))
                return MBTC;
            if (btcUnit.equals(MICRO_BTC))
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
            return MICRO;
        if (bitcoinFormat.code().equals("bits"))
            return "";
        return null;
    }
}
