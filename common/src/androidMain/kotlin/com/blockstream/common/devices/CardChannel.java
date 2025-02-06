//package com.blockstream.common.devices;
//
//import java.io.IOException;
//
//
///**
// * A channel to transcieve ISO7816-4 APDUs.
// */
//public interface CardChannel {
//    /**
//     * Sends the given C-APDU and returns an R-APDU.
//     *
//     * @param cmd the command to send
//     * @return the card response
//     * @throws IOException communication error
//     */
//    APDUResponse send(APDUCommand cmd) throws IOException;
//
//    /**
//     * True if connected, false otherwise
//     * @return true if connected, false otherwise
//     */
//    boolean isConnected();
//
//}
