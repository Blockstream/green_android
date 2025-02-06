//package com.blockstream.common.devices;
//
//import android.nfc.tech.IsoDep;
//import android.util.Log;
////import org.satochip.io.APDUCommand;
////import org.satochip.io.APDUResponse;
////import org.satochip.io.CardChannel;
//
//import java.io.IOException;
//
///**
// * Implementation of the CardChannel interface using the Android NFC API.
// */
//public class NfcCardChannel implements CardChannel {
//  private static final String TAG = "CardChannel";
//
//  private IsoDep isoDep;
//
//  public NfcCardChannel(IsoDep isoDep) {
//    this.isoDep = isoDep;
//  }
//
//  @Override
//  public APDUResponse send(APDUCommand cmd) throws IOException {
//    byte[] apdu = cmd.serialize();
//    Log.d(TAG, String.format("COMMAND CLA: %02X INS: %02X P1: %02X P2: %02X LC: %02X", cmd.getCla(), cmd.getIns(), cmd.getP1(), cmd.getP2(), cmd.getData().length));
//    byte[] resp = this.isoDep.transceive(apdu);
//    APDUResponse response = new APDUResponse(resp);
//    Log.d(TAG, String.format("RESPONSE LEN: %02X, SW: %04X %n-----------------------", response.getData().length, response.getSw()));
//    return response;
//  }
//
//  @Override
//  public boolean isConnected() {
//    return this.isoDep.isConnected();
//  }
//}
