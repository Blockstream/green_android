//package com.blockstream.common.devices;
//
////import org.bitcoinj.core.Base58;
////import org.bitcoinj.core.Sha256Hash;
////import org.bouncycastle.crypto.digests.RIPEMD160Digest;
////import org.satochip.client.seedkeeper.*;
////import org.satochip.io.*;
////import org.bouncycastle.util.encoders.Hex;
//
////import static com.satochip.Constants.INS_GET_STATUS;
//
//import java.nio.ByteBuffer;
//import java.security.SecureRandom;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.logging.Logger;
//import java.util.logging.Level;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.ByteArrayInputStream;
//import java.nio.charset.StandardCharsets;
//import java.security.cert.CertPathValidator;
//import java.security.cert.CertPath;
//import java.security.cert.CertificateFactory;
//import java.security.cert.Certificate;
//import java.security.cert.PKIXParameters;
//import java.security.KeyStore;
//import java.security.PublicKey;
//
//
//
//
///**
// * This class is used to send APDU to the applet. Each method corresponds to an APDU as defined in the APPLICATION.md
// * file. Some APDUs map to multiple methods for the sake of convenience since their payload or response require some
// * pre/post processing.
// */
//public class SatochipCommandSet {
//
//    private static final Logger logger = Logger.getLogger("org.satochip.client");
//
//    public final static byte INS_GET_STATUS = (byte) 0x3C;
//
//    private final CardChannel apduChannel;
////    private SecureChannelSession secureChannel;
//    private ApplicationStatus status;
////    private SatochipParser parser = null;
//
//    private byte[] pin0 = null;
//    private List<byte[]> possibleAuthentikeys = new ArrayList<byte[]>();
//    private byte[] authentikey = null;
//    private String authentikeyHex = null;
//    private String defaultBip32path = null;
//    private byte[] extendedKey = null;
//    private byte[] extendedChaincode = null;
//    private String extendedKeyHex = null;
//    private byte[] extendedPrivKey = null;
//    private String extendedPrivKeyHex = null;
//
//    // Satodime, SeedKeeper or Satochip?
//    private String cardType = null;
//    private String certPem = null; // PEM certificate of device, if any
//
//    // satodime
//    // SatodimeStatus satodimeStatus = null;
//
//    public static final byte[] SATOCHIP_AID = hexToBytes("5361746f43686970"); //SatoChip
//    public static final byte[] SEEDKEEPER_AID = hexToBytes("536565644b6565706572"); //SeedKeeper
//    public static final byte[] SATODIME_AID = hexToBytes("5361746f44696d65"); //SatoDime
//
//
//    /**
//     * Creates a SatochipCommandSet using the given APDU Channel
//     *
//     * @param apduChannel APDU channel
//     */
//    public SatochipCommandSet(CardChannel apduChannel) {
//        this.apduChannel = apduChannel;
////        this.secureChannel = new SecureChannelSession();
////        this.parser = new SatochipParser();
////        this.satodimeStatus = new SatodimeStatus();
//        logger.setLevel(Level.WARNING);
//    }
//
//    public void setLoggerLevel(String level) {
//        switch (level) {
//            case "info":
//                logger.setLevel(Level.INFO);
//                break;
//            case "warning":
//                logger.setLevel(Level.WARNING);
//                break;
//            default:
//                logger.setLevel(Level.WARNING);
//                break;
//        }
//    }
//
//    public void setLoggerLevel(Level level) {
//        logger.setLevel(level);
//    }
//
//    /**
//     * Returns the application info as stored from the last sent SELECT command. Returns null if no succesful SELECT
//     * command has been sent using this command set.
//     *
//     * @return the application info object
//     */
//    public ApplicationStatus getApplicationStatus() {
//        return status;
//    }
//
////    public SatodimeStatus getSatodimeStatus() {
////        this.satodimeGetStatus();
////        return this.satodimeStatus;
////    }
//
////    public byte[] getSatodimeUnlockSecret() {
////        return this.satodimeStatus.getUnlockSecret();
////    }
////
////    public void setSatodimeUnlockSecret(byte[] unlockSecret) {
////        this.satodimeStatus.setUnlockSecret(unlockSecret);
////    }
//
//    /* s must be an even-length string. */
//    public static byte[] hexToBytes(String s) {
//        int len = s.length();
//        byte[] data = new byte[len / 2];
//        for (int i = 0; i < len; i += 2) {
//            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
//                    + Character.digit(s.charAt(i+1), 16));
//        }
//        return data;
//    }
//
//    /****************************************
//     *                AUTHENTIKEY                    *
//     ****************************************/
////    public byte[] getAuthentikey() {
////        if (authentikey == null) {
////            cardGetAuthentikey();
////        }
////        return authentikey;
////    }
//
////    public String getAuthentikeyHex() {
////        if (authentikeyHex == null) {
////            cardGetAuthentikey();
////        }
////        return authentikeyHex;
////    }
//
////    public byte[] getBip32Authentikey() {
////        if (authentikey == null) {
////            cardBip32GetAuthentikey();
////        }
////        return authentikey;
////    }
//
////    public String getBip32AuthentikeyHex() {
////        if (authentikeyHex == null) {
////            cardBip32GetAuthentikey();
////        }
////        return authentikeyHex;
////    }
//
//    public List<byte[]> getPossibleAuthentikeys(){
//        return this.possibleAuthentikeys;
//    }
//
////    public SatochipParser getParser() {
////        return parser;
////    }
//
//    public void setDefaultBip32path(String bip32path) {
//        defaultBip32path = bip32path;
//    }
//
//    /**
//     * Set the SecureChannel object
//     *
//     * param secureChannel secure channel
//     */
////    protected void setSecureChannel(SecureChannelSession secureChannel) {
////        this.secureChannel = secureChannel;
////    }
//
//
//
//    public ApduResponse cardTransmit(ApduCommand plainApdu) {
//
//        // we try to transmit the APDU until we receive the answer or we receive an unrecoverable error
//        boolean isApduTransmitted = false;
//        do {
//            try {
//                byte[] apduBytes = plainApdu.serialize();
//                byte ins = apduBytes[1];
//                boolean isEncrypted = false;
//
//                // check if status available
//                if (status == null) {
//                    ApduCommand statusCapdu = new ApduCommand(0xB0, INS_GET_STATUS, 0x00, 0x00, new byte[0]);
//                    ApduResponse statusRapdu = apduChannel.send(statusCapdu);
//                    status = new ApplicationStatus(statusRapdu);
//                    logger.info("SATOCHIPLIB: Status cardGetStatus:" + status.toString());
//                }
//
//                ApduCommand capdu = null;
//                if (status.needsSecureChannel() && (ins != 0xA4) && (ins != 0x81) && (ins != 0x82) && (ins != INS_GET_STATUS)) {
//
////                    if (!secureChannel.initializedSecureChannel()) {
////                        cardInitiateSecureChannel();
////                        logger.info("SATOCHIPLIB: secure Channel initiated!");
////                    }
//                    // encrypt apdu
//                    //logger.info("SATOCHIPLIB: Capdu before encryption:"+ plainApdu.toHexString());
////                    capdu = secureChannel.encrypt_secure_channel(plainApdu);
//                    isEncrypted = true;
//                    //logger.info("SATOCHIPLIB: Capdu encrypted:"+ capdu.toHexString());
//                } else {
//                    // plain adpu
//                    capdu = plainApdu;
//                }
//
//                ApduResponse rapdu = apduChannel.send(capdu);
//                int sw12 = rapdu.getSw();
//
//                // check answer
//                if (sw12 == 0x9000) { // ok!
//                    if (isEncrypted) {
//                        // decrypt
//                        //logger.info("SATOCHIPLIB: Rapdu encrypted:"+ rapdu.toHexString());
////                        rapdu = secureChannel.decrypt_secure_channel(rapdu);
//                        //logger.info("SATOCHIPLIB: Rapdu decrypted:"+ rapdu.toHexString());
//                    }
//                    isApduTransmitted = true; // leave loop
//                    return rapdu;
//                }
//                // PIN authentication is required
//                else if (sw12 == 0x9C06) {
////                    cardVerifyPIN();
//                }
//                // SecureChannel is not initialized
//                else if (sw12 == 0x9C21) {
////                    secureChannel.resetSecureChannel();
//                } else {
//                    // cannot resolve issue at this point
//                    isApduTransmitted = true; // leave loop
//                    return rapdu;
//                }
//
//            } catch (Exception e) {
//                logger.warning("SATOCHIPLIB: Exception in cardTransmit: " + e);
//                return new ApduResponse(new byte[0], (byte) 0x00, (byte) 0x00); // return empty ApduResponse
//            }
//
//        } while (!isApduTransmitted);
//
//        return new ApduResponse(new byte[0], (byte) 0x00, (byte) 0x00); // should not happen
//    }
//
//    public void cardDisconnect() {
////        secureChannel.resetSecureChannel();
//        status = null;
//        pin0 = null;
//    }
//
//    /**
//     * Selects a Satochip/Satodime/SeedKeeper instance. The applet is assumed to have been installed with its default AID.
//     *
//     * @return the raw card response
//     * @throws IOException communication error
//     */
//    public ApduResponse cardSelect() throws IOException {
//
//        ApduResponse rapdu = cardSelect("satochip");
//        if (rapdu.getSw() != 0x9000) {
//            rapdu = cardSelect("seedkeeper");
//            if (rapdu.getSw() != 0x9000) {
//                rapdu = cardSelect("satodime");
//                if (rapdu.getSw() != 0x9000) {
//                    this.cardType = "unknown";
//                    logger.warning("SATOCHIPLIB: CardSelect: could not select a known applet");
//                }
//            }
//        }
//
//        return rapdu;
//    }
//
//    public ApduResponse cardSelect(String cardType) throws IOException {
//
//        ApduCommand selectApplet;
//        if (cardType.equals("satochip")) {
//            selectApplet = new ApduCommand(0x00, 0xA4, 0x04, 0x00, SATOCHIP_AID);
//        } else if (cardType.equals("seedkeeper")) {
//            selectApplet = new ApduCommand(0x00, 0xA4, 0x04, 0x00, SEEDKEEPER_AID);
//        } else {
//            selectApplet = new ApduCommand(0x00, 0xA4, 0x04, 0x00, SATODIME_AID);
//        }
//
//        logger.info("SATOCHIPLIB: C-APDU cardSelect:" + selectApplet.toHexString());
//        ApduResponse respApdu = apduChannel.send(selectApplet);
//        logger.info("SATOCHIPLIB: R-APDU cardSelect:" + respApdu.toHexString());
//
//        if (respApdu.getSw() == 0x9000) {
//            this.cardType = cardType;
//            logger.info("SATOCHIPLIB: Satochip-java: CardSelect: found a " + this.cardType);
//        }
//        return respApdu;
//    }
//
//    public ApduResponse cardGetStatus() {
//        ApduCommand plainApdu = new ApduCommand(0xB0, INS_GET_STATUS, 0x00, 0x00, new byte[0]);
//
//        logger.info("SATOCHIPLIB: C-APDU cardGetStatus:" + plainApdu.toHexString());
//        ApduResponse respApdu = this.cardTransmit(plainApdu);
//        logger.info("SATOCHIPLIB: R-APDU cardGetStatus:" + respApdu.toHexString());
//
//        status = new ApplicationStatus(respApdu);
//        logger.info("SATOCHIPLIB: Status from cardGetStatus:" + status.toString());
//
//        return respApdu;
//    }
//
//}
