//package com.blockstream.common.devices;
//
//import android.nfc.NfcAdapter;
//import android.nfc.Tag;
//import android.nfc.tech.IsoDep;
//import android.os.SystemClock;
//import android.util.Log;
//
//import java.io.IOException;
//
///**
// * Manages connection of NFC-based cards. Extends Thread and must be started using the start() method. The thread has
// * a runloop which monitors the connection and from which CardListener callbacks are called.
// */
//public class NfcCardManager extends Thread implements NfcAdapter.ReaderCallback {
//    private static final String TAG = "NFCCardManager";
//    private static final int DEFAULT_LOOP_SLEEP_MS = 50;
//
//    private IsoDep isoDep;
//    private boolean isRunning;
//    private CardListener cardListener;
//    private int loopSleepMS;
//
////  static {
////    Crypto.addBouncyCastleProvider();
////  }
//
//    /**
//     * Constructs an NFC Card Manager with default delay between loop iterations.
//     */
//    public NfcCardManager() {
//        this(DEFAULT_LOOP_SLEEP_MS);
//    }
//
//    /**
//     * Constructs an NFC Card Manager with the given delay between loop iterations.
//     *
//     * @param loopSleepMS time to sleep between loops
//     */
//    public NfcCardManager(int loopSleepMS) {
//        this.loopSleepMS = loopSleepMS;
//    }
//
//    /**
//     * True if connected, false otherwise.
//     * @return if connected, false otherwise
//     */
//    public boolean isConnected() {
//        try {
//            return isoDep != null && isoDep.isConnected();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    @Override
//    public void onTagDiscovered(Tag tag) {
//        isoDep = IsoDep.get(tag);
//        try {
//            isoDep = IsoDep.get(tag);
//            isoDep.connect();
//            isoDep.setTimeout(120000);
//        } catch (IOException e) {
//            Log.e(TAG, "error connecting to tag");
//        }
//    }
//
//    /**
//     * Runloop. Do NOT invoke directly. Use start() instead.
//     */
//    public void run() {
//        boolean connected = isConnected();
//
//        while (true) {
//            boolean newConnected = isConnected();
//            if (newConnected != connected) {
//                connected = newConnected;
//                Log.i(TAG, "tag " + (connected ? "connected" : "disconnected"));
//
//                if (connected && !isRunning) {
//                    onCardConnected();
//                } else {
//                    onCardDisconnected();
//                }
//            }
//
//            SystemClock.sleep(loopSleepMS);
//        }
//    }
//
//    /**
//     * Reacts on card connected by calling the callback of the registered listener.
//     */
//    private void onCardConnected() {
//        isRunning = true;
//
//        if (cardListener != null) {
//            cardListener.onConnected(new NfcCardChannel(isoDep));
//        }
//
//        isRunning = false;
//    }
//
//    /**
//     * Reacts on card disconnected by calling the callback of the registered listener.
//     */
//    private void onCardDisconnected() {
//        isRunning = false;
//        isoDep = null;
//        if (cardListener != null) {
//            cardListener.onDisconnected();
//        }
//    }
//
//    /**
//     * Sets the card listener.
//     *
//     * @param listener the new listener
//     */
//    public void setCardListener(CardListener listener) {
//        cardListener = listener;
//    }
//}
