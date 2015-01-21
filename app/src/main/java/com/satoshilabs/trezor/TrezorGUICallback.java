package com.satoshilabs.trezor;

public interface TrezorGUICallback {
	public String PinMatrixRequest();
	public String PassphraseRequest();
}
