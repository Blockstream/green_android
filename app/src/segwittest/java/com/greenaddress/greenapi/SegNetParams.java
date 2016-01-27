package com.greenaddress.greenapi;

import org.bitcoinj.params.TestNet3Params;

public class SegNetParams extends TestNet3Params {
    public SegNetParams() {
        super();
        addressHeader = 30;
        p2shHeader = 50;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
    }
}
