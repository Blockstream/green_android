package com.greenaddress.greenbits.ui;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.Fiat;

import java.io.Serializable;
import java.util.Date;

public class BitBoatTransaction implements Serializable {
    static final int PAYMETHOD_SUPERFLASH = 0, PAYMETHOD_POSTEPAY = 1, PAYMETHOD_MANDATCOMPTE = 2;

    public final Date date;
    public final String firstbits;
    public final int method;
    public final Coin valueBtc;
    public final Fiat valueFiat;
    public final String number;
    public final String cf;
    public final String key;

    public BitBoatTransaction(Date date, String firstbits, int method, Coin valueBtc, Fiat valueFiat, String number, String cf, String key) {
        this.date = date;
        this.firstbits = firstbits;
        this.method = method;
        this.valueBtc = valueBtc;
        this.valueFiat = valueFiat;
        this.number = number;
        this.cf = cf;
        this.key = key;
    }
}
