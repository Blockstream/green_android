package com.greenaddress.greenbits.ui;

import org.bitcoinj.utils.MonetaryFormat;

class CurrencyMapper {
    private static final String MICRO = "\u00B5";
    private static final String MICRO_BTC = "\u00B5BTC";
    private static final MonetaryFormat MBTC = new MonetaryFormat().shift(3).minDecimals(2).repeatOptionalDecimals(1, 3);

    public static String mapBtcUnitToPrefix(final String btcUnit) {
        if (MonetaryFormat.CODE_MBTC.equals(btcUnit))
            return "m";
        if (MICRO_BTC.equals(btcUnit))
            return MICRO;
        return ""; // Everything else
    }

    public static MonetaryFormat mapBtcUnitToFormat(final String btcUnit) {
        if (MonetaryFormat.CODE_BTC.equals(btcUnit))
            return MonetaryFormat.BTC;
        if (MonetaryFormat.CODE_MBTC.equals(btcUnit))
            return MBTC;
        if (MICRO_BTC.equals(btcUnit))
            return MonetaryFormat.UBTC;
        return MonetaryFormat.UBTC.code(6, "bits");
    }
}
