package com.greenaddress.greenbits.ui;

import android.text.Html;

import org.bitcoinj.utils.MonetaryFormat;

import java.util.HashMap;
import java.util.Map;

public class CurrencyMapper {
    private static final Map<String, String> map = new HashMap<>();

    static {
        map.put("USD", "&#xf155;");
        map.put("AUD", "&#xf155;");
        map.put("CAD", "&#xf155;");
        map.put("EUR", "&#xf153;");
        map.put("CNY", "&#xf157;");
        map.put("GBP", "&#xf154;");
        map.put("ILS", "&#xf20b;");
        map.put("RUB", "&#xf158;");
        map.put("BRL", "R&#xf155;");
    }

    public static String map(final String currency) {
        return map.get(currency);
    }

    public static String mapBtcUnitToPrefix(final String btcUnit) {
        if (btcUnit != null) {
            if (btcUnit.equals("BTC")) {
                return "";
            } else if (btcUnit.equals("mBTC")) {
                return "m";
            } else if (btcUnit.equals(Html.fromHtml("&micro;").toString() + "BTC")) { // bits or uBTC or default
                return "&micro;";
            } else {
                return ""; // bits
            }
        } else {
            return ""; // bits
        }
    }

    public static MonetaryFormat mapBtcUnitToFormat(final String btcUnit) {
        if (btcUnit != null) {
            if (btcUnit.equals("BTC")) {
                return MonetaryFormat.BTC;
            } else if (btcUnit.equals("mBTC")) {
                return MonetaryFormat.MBTC;
            } else if (btcUnit.equals(Html.fromHtml("&micro;").toString() + "BTC")) {
                return MonetaryFormat.UBTC;
            } else {
                return MonetaryFormat.UBTC.code(6, "bits");
            }
        } else {
            return MonetaryFormat.UBTC.code(6, "bits");
        }

    }

    public static String mapBtcFormatToPrefix(final MonetaryFormat bitcoinFormat) {
        if (bitcoinFormat.code().equals(MonetaryFormat.CODE_BTC)) {
            return "";
        } else if (bitcoinFormat.code().equals(MonetaryFormat.CODE_MBTC)) {
            return "m";
        } else if (bitcoinFormat.code().equals(MonetaryFormat.CODE_UBTC)) {
            return "&micro;";
        } else if (bitcoinFormat.code().equals("bits")) {
            return "";
        }
        return null;
    }
}