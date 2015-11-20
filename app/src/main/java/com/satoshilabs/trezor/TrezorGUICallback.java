package com.satoshilabs.trezor;

public interface TrezorGUICallback {
	String PinMatrixRequest();
	String PassphraseRequest();
}
