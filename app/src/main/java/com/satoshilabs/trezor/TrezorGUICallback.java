package com.satoshilabs.trezor;

public interface TrezorGUICallback {
	String pinMatrixRequest();
	String passphraseRequest();
}
