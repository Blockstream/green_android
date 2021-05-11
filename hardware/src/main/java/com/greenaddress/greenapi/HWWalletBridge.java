package com.greenaddress.greenapi;

public interface HWWalletBridge {
    void interactionRequest(final HWWallet hw);
    String pinMatrixRequest(final HWWallet hw);
    String passphraseRequest(final HWWallet hw);
}
