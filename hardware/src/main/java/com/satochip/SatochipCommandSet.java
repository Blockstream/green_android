package com.satochip;


import static com.satochip.Constants.*;


import com.blockstream.libwally.Wally;

//import org.bitcoinj.core.Base58;
//import org.bitcoinj.core.Sha256Hash;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.util.encoders.Hex;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;

/**
 * This class is used to send APDU to the applet. Each method corresponds to an APDU as defined in the APPLICATION.md
 * file. Some APDUs map to multiple methods for the sake of convenience since their payload or response require some
 * pre/post processing.
 */
public class SatochipCommandSet {

    private static final Logger logger = Logger.getLogger("org.satochip.client");

    private final CardChannel apduChannel;
    private SecureChannelSession secureChannel;
    private ApplicationStatus status;
    private SatochipParser parser = null;

    private byte[] pin0 = null;
    private List<byte[]> possibleAuthentikeys = new ArrayList<byte[]>();
    private byte[] authentikey = null;
    private String authentikeyHex = null;
    private String defaultBip32path = null;
    private byte[] extendedKey = null;
    private byte[] extendedChaincode = null;
    private String extendedKeyHex = null;
    private byte[] extendedPrivKey = null;
    private String extendedPrivKeyHex = null;

    // Satodime, SeedKeeper or Satochip?
    private String cardType = null;
    private String certPem = null; // PEM certificate of device, if any



    public static final byte[] SATOCHIP_AID = Hex.decode("5361746f43686970"); //SatoChip
    public static final byte[] SEEDKEEPER_AID = Hex.decode("536565644b6565706572"); //SeedKeeper
    public static final byte[] SATODIME_AID = Hex.decode("5361746f44696d65"); //SatoDime


    /**
     * Creates a SatochipCommandSet using the given APDU Channel
     *
     * @param apduChannel APDU channel
     */
    public SatochipCommandSet(CardChannel apduChannel) {
        this.apduChannel = apduChannel;
        this.secureChannel = new SecureChannelSession();
        this.parser = new SatochipParser();
        logger.setLevel(Level.WARNING);
    }

    public void setLoggerLevel(String level) {
        switch (level) {
            case "info":
                logger.setLevel(Level.INFO);
                break;
            case "warning":
                logger.setLevel(Level.WARNING);
                break;
            default:
                logger.setLevel(Level.WARNING);
                break;
        }
    }

    public void setLoggerLevel(Level level) {
        logger.setLevel(level);
    }

    /**
     * Returns the application info as stored from the last sent SELECT command. Returns null if no succesful SELECT
     * command has been sent using this command set.
     *
     * @return the application info object
     */
    public ApplicationStatus getApplicationStatus() {
        return status;
    }

    /****************************************
     *                AUTHENTIKEY                    *
     ****************************************/
    public byte[] getAuthentikey() {
        if (authentikey == null) {
            cardGetAuthentikey();
        }
        return authentikey;
    }

    public String getAuthentikeyHex() {
        if (authentikeyHex == null) {
            cardGetAuthentikey();
        }
        return authentikeyHex;
    }

    public byte[] getBip32Authentikey() {
        if (authentikey == null) {
            cardBip32GetAuthentikey();
        }
        return authentikey;
    }

    public String getBip32AuthentikeyHex() {
        if (authentikeyHex == null) {
            cardBip32GetAuthentikey();
        }
        return authentikeyHex;
    }

    public List<byte[]> getPossibleAuthentikeys(){
        return this.possibleAuthentikeys;
    }

    public SatochipParser getParser() {
        return parser;
    }

    public void setDefaultBip32path(String bip32path) {
        defaultBip32path = bip32path;
    }

    /**
     * Set the SecureChannel object
     *
     * @param secureChannel secure channel
     */
    protected void setSecureChannel(SecureChannelSession secureChannel) {
        this.secureChannel = secureChannel;
    }



    public ApduResponse cardTransmit(ApduCommand plainApdu) {

        // we try to transmit the APDU until we receive the answer or we receive an unrecoverable error
        boolean isApduTransmitted = false;
        do {
            try {
                byte[] apduBytes = plainApdu.serialize();
                byte ins = apduBytes[1];
                boolean isEncrypted = false;

                // check if status available
                if (status == null) {
                    ApduCommand statusCapdu = new ApduCommand(0xB0, INS_GET_STATUS, 0x00, 0x00, new byte[0]);
                    ApduResponse statusRapdu = apduChannel.send(statusCapdu);
                    status = new ApplicationStatus(statusRapdu);
                    logger.info("SATOCHIPLIB: Status cardGetStatus:" + status.toString());
                }

                ApduCommand capdu = null;
                if (status.needsSecureChannel() && (ins != 0xA4) && (ins != 0x81) && (ins != 0x82) && (ins != INS_GET_STATUS)) {

                    if (!secureChannel.initializedSecureChannel()) {
                        cardInitiateSecureChannel();
                        logger.info("SATOCHIPLIB: secure Channel initiated!");
                    }
                    // encrypt apdu
                    //logger.info("SATOCHIPLIB: Capdu before encryption:"+ plainApdu.toHexString());
                    capdu = secureChannel.encrypt_secure_channel(plainApdu);
                    isEncrypted = true;
                    //logger.info("SATOCHIPLIB: Capdu encrypted:"+ capdu.toHexString());
                } else {
                    // plain adpu
                    capdu = plainApdu;
                }

                ApduResponse rapdu = apduChannel.send(capdu);
                int sw12 = rapdu.getSw();

                // check answer
                if (sw12 == 0x9000) { // ok!
                    if (isEncrypted) {
                        // decrypt
                        //logger.info("SATOCHIPLIB: Rapdu encrypted:"+ rapdu.toHexString());
                        rapdu = secureChannel.decrypt_secure_channel(rapdu);
                        //logger.info("SATOCHIPLIB: Rapdu decrypted:"+ rapdu.toHexString());
                    }
                    isApduTransmitted = true; // leave loop
                    return rapdu;
                }
                // PIN authentication is required
                else if (sw12 == 0x9C06) {
                    cardVerifyPIN();
                }
                // SecureChannel is not initialized
                else if (sw12 == 0x9C21) {
                    secureChannel.resetSecureChannel();
                } else {
                    // cannot resolve issue at this point
                    isApduTransmitted = true; // leave loop
                    return rapdu;
                }

            } catch (Exception e) {
                logger.warning("SATOCHIPLIB: Exception in cardTransmit: " + e);
                return new ApduResponse(new byte[0], (byte) 0x00, (byte) 0x00); // return empty ApduResponse
            }

        } while (!isApduTransmitted);

        return new ApduResponse(new byte[0], (byte) 0x00, (byte) 0x00); // should not happen
    }

    public void cardDisconnect() {
        secureChannel.resetSecureChannel();
        status = null;
        pin0 = null;
    }

    /**
     * Selects a Satochip/Satodime/SeedKeeper instance. The applet is assumed to have been installed with its default AID.
     *
     * @return the raw card response
     * @throws IOException communication error
     */
    public ApduResponse cardSelect() throws IOException {

        ApduResponse rapdu = cardSelect("satochip");
        if (rapdu.getSw() != 0x9000) {
            rapdu = cardSelect("seedkeeper");
            if (rapdu.getSw() != 0x9000) {
                rapdu = cardSelect("satodime");
                if (rapdu.getSw() != 0x9000) {
                    this.cardType = "unknown";
                    logger.warning("SATOCHIPLIB: CardSelect: could not select a known applet");
                }
            }
        }

        return rapdu;
    }

    public ApduResponse cardSelect(String cardType) throws IOException {

        ApduCommand selectApplet;
        if (cardType.equals("satochip")) {
            selectApplet = new ApduCommand(0x00, 0xA4, 0x04, 0x00, SATOCHIP_AID);
        } else if (cardType.equals("seedkeeper")) {
            selectApplet = new ApduCommand(0x00, 0xA4, 0x04, 0x00, SEEDKEEPER_AID);
        } else {
            selectApplet = new ApduCommand(0x00, 0xA4, 0x04, 0x00, SATODIME_AID);
        }

        logger.info("SATOCHIPLIB: C-APDU cardSelect:" + selectApplet.toHexString());
        ApduResponse respApdu = apduChannel.send(selectApplet);
        logger.info("SATOCHIPLIB: R-APDU cardSelect:" + respApdu.toHexString());

        if (respApdu.getSw() == 0x9000) {
            this.cardType = cardType;
            logger.info("SATOCHIPLIB: Satochip-java: CardSelect: found a " + this.cardType);
        }
        return respApdu;
    }

    public ApduResponse cardGetStatus() {
        ApduCommand plainApdu = new ApduCommand(0xB0, INS_GET_STATUS, 0x00, 0x00, new byte[0]);

        logger.info("SATOCHIPLIB: C-APDU cardGetStatus:" + plainApdu.toHexString());
        ApduResponse respApdu = this.cardTransmit(plainApdu);
        logger.info("SATOCHIPLIB: R-APDU cardGetStatus:" + respApdu.toHexString());

        status = new ApplicationStatus(respApdu);
        logger.info("SATOCHIPLIB: Status from cardGetStatus:" + status.toString());

        return respApdu;
    }

    // do setup secure channel in this method
    public List<byte[]> cardInitiateSecureChannel() throws IOException {

        byte[] pubkey = secureChannel.getPublicKey();

        ApduCommand plainApdu = new ApduCommand(0xB0, INS_INIT_SECURE_CHANNEL, 0x00, 0x00, pubkey);

        logger.info("SATOCHIPLIB: C-APDU cardInitiateSecureChannel:" + plainApdu.toHexString());
        ApduResponse respApdu = apduChannel.send(plainApdu);
        logger.info("SATOCHIPLIB: R-APDU cardInitiateSecureChannel:" + respApdu.toHexString());

        byte[] keyData = parser.parseInitiateSecureChannel(respApdu);
        possibleAuthentikeys = parser.parseInitiateSecureChannelGetPossibleAuthentikeys(respApdu);
        // setup secure channel
        secureChannel.initiateSecureChannel(keyData);

        return possibleAuthentikeys;
    }
    // only valid for v0.12 and higher
    public byte[] cardGetAuthentikey() {

        ApduCommand plainApdu = new ApduCommand(0xB0, INS_EXPORT_AUTHENTIKEY, 0x00, 0x00, new byte[0]);
        logger.info("SATOCHIPLIB: C-APDU cardExportAuthentikey:" + plainApdu.toHexString());
        ApduResponse respApdu = this.cardTransmit(plainApdu);
        logger.info("SATOCHIPLIB: R-APDU cardExportAuthentikey:" + respApdu.toHexString());

        // parse and recover pubkey
        authentikey = parser.parseBip32GetAuthentikey(respApdu);
        authentikeyHex = parser.toHexString(authentikey);
        logger.info("SATOCHIPLIB: Authentikey from cardExportAuthentikey:" + authentikeyHex);

        return authentikey;
    }

    public ApduResponse cardBip32GetAuthentikey() {

        ApduCommand plainApdu = new ApduCommand(0xB0, INS_BIP32_GET_AUTHENTIKEY, 0x00, 0x00, new byte[0]);
        logger.info("SATOCHIPLIB: C-APDU cardBip32GetAuthentikey:" + plainApdu.toHexString());
        ApduResponse respApdu = this.cardTransmit(plainApdu);
        logger.info("SATOCHIPLIB: R-APDU cardBip32GetAuthentikey:" + respApdu.toHexString());

        // parse and recover pubkey
        authentikey = parser.parseBip32GetAuthentikey(respApdu);
        authentikeyHex = parser.toHexString(authentikey);
        logger.info("SATOCHIPLIB: Authentikey from cardBip32GetAuthentikey:" + authentikeyHex);

        return respApdu;
    }

//    public ApduResponse cardExportPkiPubkey() {
//
//        ApduCommand plainApdu = new ApduCommand(0xB0, INS_EXPORT_PKI_PUBKEY, 0x00, 0x00, new byte[0]);
//        logger.info("SATOCHIPLIB: C-APDU cardExportPkiPubkey:" + plainApdu.toHexString());
//        ApduResponse respApdu = this.cardTransmit(plainApdu);
//        logger.info("SATOCHIPLIB: R-APDU cardExportPkiPubkey:" + respApdu.toHexString());
//
//        // parse and recover pubkey
//        authentikey = parser.parseExportPkiPubkey(respApdu);
//        authentikeyHex = parser.toHexString(authentikey);
//        logger.info("SATOCHIPLIB: Authentikey from cardExportPkiPubkey:" + authentikeyHex);
//
//        return respApdu;
//    }

    /****************************************
     *                 CARD MGMT                      *
     ****************************************/

//    public ApduResponse cardSetup(byte pin_tries0, byte[] pin0) {
//
//        // use random values for pin1, ublk0, ublk1
//        SecureRandom random = new SecureRandom();
//        byte[] ublk0 = new byte[8];
//        byte[] ublk1 = new byte[8];
//        byte[] pin1 = new byte[8];
//        random.nextBytes(ublk0);
//        random.nextBytes(ublk1);
//        random.nextBytes(pin1);
//
//        byte ublk_tries0 = (byte) 0x01;
//        byte ublk_tries1 = (byte) 0x01;
//        byte pin_tries1 = (byte) 0x01;
//
//        return cardSetup(pin_tries0, ublk_tries0, pin0, ublk0, pin_tries1, ublk_tries1, pin1, ublk1);
//    }

//    public ApduResponse cardSetup(
//            byte pin_tries0, byte ublk_tries0, byte[] pin0, byte[] ublk0,
//            byte pin_tries1, byte ublk_tries1, byte[] pin1, byte[] ublk1) {
//
//        byte[] pin = {0x4D, 0x75, 0x73, 0x63, 0x6C, 0x65, 0x30, 0x30}; //default pin
//        byte cla = (byte) 0xB0;
//        byte ins = INS_SETUP;
//        byte p1 = 0;
//        byte p2 = 0;
//
//        // data=[pin_length(1) | pin |
//        //        pin_tries0(1) | ublk_tries0(1) | pin0_length(1) | pin0 | ublk0_length(1) | ublk0 |
//        //        pin_tries1(1) | ublk_tries1(1) | pin1_length(1) | pin1 | ublk1_length(1) | ublk1 |
//        //        memsize(2) | memsize2(2) | ACL(3) |
//        //        option_flags(2) | hmacsha160_key(20) | amount_limit(8)]
//        int optionsize = 0;
//        int option_flags = 0; // do not use option (mostly deprecated)
//        int offset = 0;
//        int datasize = 16 + pin.length + pin0.length + pin1.length + ublk0.length + ublk1.length + optionsize;
//        byte[] data = new byte[datasize];
//
//        data[offset++] = (byte) pin.length;
//        System.arraycopy(pin, 0, data, offset, pin.length);
//        offset += pin.length;
//        // pin0 & ublk0
//        data[offset++] = pin_tries0;
//        data[offset++] = ublk_tries0;
//        data[offset++] = (byte) pin0.length;
//        System.arraycopy(pin0, 0, data, offset, pin0.length);
//        offset += pin0.length;
//        data[offset++] = (byte) ublk0.length;
//        System.arraycopy(ublk0, 0, data, offset, ublk0.length);
//        offset += ublk0.length;
//        // pin1 & ublk1
//        data[offset++] = pin_tries1;
//        data[offset++] = ublk_tries1;
//        data[offset++] = (byte) pin1.length;
//        System.arraycopy(pin1, 0, data, offset, pin1.length);
//        offset += pin1.length;
//        data[offset++] = (byte) ublk1.length;
//        System.arraycopy(ublk1, 0, data, offset, ublk1.length);
//        offset += ublk1.length;
//
//        // memsize default (deprecated)
//        data[offset++] = (byte) 00;
//        data[offset++] = (byte) 32;
//        data[offset++] = (byte) 00;
//        data[offset++] = (byte) 32;
//
//        // ACL (deprecated)
//        data[offset++] = (byte) 0x01;
//        data[offset++] = (byte) 0x01;
//        data[offset++] = (byte) 0x01;
//
//        ApduCommand plainApdu = new ApduCommand(cla, ins, p1, p2, data);
//        //logger.info("SATOCHIPLIB: C-APDU cardSetup:" + plainApdu.toHexString());
//        logger.info("SATOCHIPLIB: C-APDU cardSetup");
//        ApduResponse respApdu = this.cardTransmit(plainApdu);
//        logger.info("SATOCHIPLIB: R-APDU cardSetup:" + respApdu.toHexString());
//
//        if (respApdu.isOK()) {
//            setPin0(pin0);
//
//            if (this.cardType.equals("satodime")) { // cache values
//                this.satodimeStatus.updateStatusFromSetup(respApdu);
//            }
//        }
//
//        return respApdu;
//    }

//    public ApduResponse cardSendResetCommand() throws Exception {
//        byte[] data = new byte[]{};
//
//        ApduCommand plainApdu = new ApduCommand(
//                0xB0,
//                INS_RESET_TO_FACTORY,
//                0x00,
//                0x00,
//                data
//        );
//
//        // reset command must be sent in clear, without other commands interferring between reset commands
//        logger.warning("SATOCHIPLIB: C-APDU cardSendResetCommand:" + plainApdu.toHexString());
//        ApduResponse respApdu = apduChannel.send(plainApdu);
//        logger.warning("SATOCHIPLIB: R-APDU cardSendResetCommand:" + respApdu.toHexString());
//
//        return respApdu;
//    }

    /****************************************
     *             PIN MGMT                  *
     ****************************************/
    public void setPin0(byte[] pin) {
        this.pin0 = new byte[pin.length];
        System.arraycopy(pin, 0, this.pin0, 0, pin.length);
    }

    public ApduResponse cardVerifyPIN(byte[] pin) throws Exception {

        byte[] mypin = pin;
        if (mypin == null){
            if (pin0 == null) {
                // TODO: specific exception
                throw new RuntimeException("PIN required!");
            }
            mypin = pin0;
        }

        try {
            ApduCommand plainApdu = new ApduCommand(0xB0, INS_VERIFY_PIN, 0x00, 0x00, mypin);
            //logger.info("SATOCHIPLIB: C-APDU cardVerifyPIN:" + plainApdu.toHexString());
            logger.info("SATOCHIPLIB: C-APDU cardVerifyPIN");
            ApduResponse rapdu = this.cardTransmit(plainApdu);
            logger.info("SATOCHIPLIB: R-APDU cardVerifyPIN:" + rapdu.toHexString());

            rapdu.checkAuthOK();
            this.pin0 = mypin; // cache new pin
            return rapdu;

        } catch (WrongPINException e) {
            this.pin0 = null;
            throw e;
        } catch (WrongPINLegacyException e) {
            this.pin0 = null;
            throw e;
        } catch (BlockedPINException e) {
            this.pin0 = null;
            throw e;
        } catch (ApduException e){
            this.pin0 = null;
            throw e;
        } catch (Exception e){
            this.pin0 = null;
            throw e;
        }
    }

    public ApduResponse cardVerifyPIN() throws Exception {
        return cardVerifyPIN(this.pin0);
    }

//    public ApduResponse cardChangePin(byte[] oldPin, byte[] newPin) throws Exception {
//        logger.info("SATOCHIPLIB: changeCardPin START");
//
//        byte[] data = new byte[1 + oldPin.length + 1 + newPin.length];
//        data[0] = (byte) oldPin.length;
//        System.arraycopy(oldPin, 0, data, 1, oldPin.length);
//        data[1 + oldPin.length] = (byte) newPin.length;
//        System.arraycopy(newPin, 0, data, 2 + oldPin.length, newPin.length);
//        setPin0(newPin);
//        try{
//            ApduCommand plainApdu = new ApduCommand(0xB0, INS_CHANGE_PIN, 0x00, 0x00, data);
//            //logger.info("SATOCHIPLIB: C-APDU changeCardPin:"+ plainApdu.toHexString());
//            logger.info("SATOCHIPLIB: C-APDU changeCardPin");
//            ApduResponse rapdu = this.cardTransmit(plainApdu);
//            logger.info("SATOCHIPLIB: R-APDU changeCardPin:"+ rapdu.toHexString());
//
//            rapdu.checkAuthOK();
//            return rapdu;
//
//        } catch (WrongPINException e) {
//            this.pin0 = null;
//            throw e;
//        } catch (WrongPINLegacyException e) {
//            this.pin0 = null;
//            throw e;
//        } catch (BlockedPINException e) {
//            this.pin0 = null;
//            throw e;
//        } catch (ApduException e){
//            this.pin0 = null;
//            throw e;
//        } catch (Exception e){
//            this.pin0 = null;
//            throw e;
//        }
//    }

//    public ApduResponse cardUnblockPin(byte[] puk) throws Exception {
//        ApduCommand plainApdu = new ApduCommand(
//                0xB0,
//                INS_UNBLOCK_PIN,
//                0x00,
//                0x00,
//                puk
//        );
//
//        try{
//            //logger.info("SATOCHIPLIB: C-APDU cardUnblockPin:" + plainApdu.toHexString());
//            logger.info("SATOCHIPLIB: C-APDU cardUnblockPin");
//            ApduResponse rapdu = this.cardTransmit(plainApdu);
//            logger.info("SATOCHIPLIB: R-APDU cardUnblockPin:" + rapdu.toHexString());
//
//            rapdu.checkAuthOK();
//            return rapdu;
//
//        } catch (WrongPINException e) {
//            this.pin0 = null;
//            throw e;
//        } catch (WrongPINLegacyException e) {
//            this.pin0 = null;
//            throw e;
//        } catch (BlockedPINException e) {
//            this.pin0 = null;
//            throw e;
//        } catch (ResetToFactoryException e) {
//            this.pin0 = null;
//            throw e;
//        } catch (ApduException e){
//            this.pin0 = null;
//            throw e;
//        } catch (Exception e){
//            this.pin0 = null;
//            throw e;
//        }
//
//    }

    /****************************************
     *                 BIP32                     *
     ****************************************/

//    public ApduResponse cardBip32ImportSeed(byte[] masterseed) {
//        //TODO: check seed (length...)
//        ApduCommand plainApdu = new ApduCommand(0xB0, INS_BIP32_IMPORT_SEED, masterseed.length, 0x00, masterseed);
//
//        //logger.info("SATOCHIPLIB: C-APDU cardBip32ImportSeed:" + plainApdu.toHexString());
//        logger.info("SATOCHIPLIB: C-APDU cardBip32ImportSeed");
//        ApduResponse respApdu = this.cardTransmit(plainApdu);
//        logger.info("SATOCHIPLIB: R-APDU cardBip32ImportSeed:" + respApdu.toHexString());
//
//        return respApdu;
//    }

//    public ApduResponse cardResetSeed(byte[] pin, byte[] chalresponse) {
//
//        byte p1 = (byte) pin.length;
//        byte[] data;
//        if (chalresponse == null) {
//            data = new byte[pin.length];
//            System.arraycopy(pin, 0, data, 0, pin.length);
//        } else if (chalresponse.length == 20) {
//            data = new byte[pin.length + 20];
//            int offset = 0;
//            System.arraycopy(pin, 0, data, offset, pin.length);
//            offset += pin.length;
//            System.arraycopy(chalresponse, 0, data, offset, chalresponse.length);
//        } else {
//            throw new RuntimeException("Wrong challenge-response length (should be 20)");
//        }
//
//        ApduCommand plainApdu = new ApduCommand(0xB0, INS_BIP32_RESET_SEED, p1, 0x00, data);
//        logger.info("SATOCHIPLIB: C-APDU cardSignTransactionHash:" + plainApdu.toHexString());
//        ApduResponse respApdu = this.cardTransmit(plainApdu);
//        logger.info("SATOCHIPLIB: R-APDU cardSignTransactionHash:" + respApdu.toHexString());
//        // TODO: check SW code for particular status
//
//        return respApdu;
//    }

//    public byte[][] cardBip32GetExtendedKey() throws Exception {
//        if (defaultBip32path == null) {
//            defaultBip32path = "m/44'/60'/0'/0/0";
//        }
//        return cardBip32GetExtendedKey(defaultBip32path, null, null);
//    }

    public byte[][] cardBip32GetExtendedKey(String stringPath, Byte flags, Integer sid) throws Exception {
        Bip32Path parsedPath = parser.parseBip32PathToBytes(stringPath);
        return cardBip32GetExtendedKey(parsedPath, flags, sid);
    }

    public byte[][] cardBip32GetExtendedKey(Bip32Path parsedPath, Byte flags, Integer sid) throws Exception {
        logger.warning("SATOCHIPLIB: cardBip32GetExtendedKey");
        if (parsedPath.getDepth() > 10) {
            throw new Exception("Path length exceeds maximum depth of 10: " + parsedPath.getDepth());
        }

        byte p1 = parsedPath.getDepth().byteValue();
        byte optionFlags = (byte) 0x40;
        if (flags != null) {
            optionFlags = flags;
        }
        byte p2 = optionFlags;

        byte[] data = parsedPath.getBytes();

        if (sid != null) {
            data = Arrays.copyOf(data, data.length + 2);
            data[data.length - 2] = (byte) ((sid >> 8) & 0xFF);
            data[data.length - 1] = (byte) (sid & 0xFF);
        }

        while (true) {
            ApduCommand plainApdu = new ApduCommand(
                    0xB0,
                    INS_BIP32_GET_EXTENDED_KEY,
                    p1,
                    p2,
                    data
            );
            logger.warning("SATOCHIPLIB: C-APDU cardBip32GetExtendedKey:" + plainApdu.toHexString());
            ApduResponse respApdu = this.cardTransmit(plainApdu);
            logger.warning("SATOCHIPLIB: R-APDU cardBip32GetExtendedKey:" + respApdu.toHexString());
            if (respApdu.getSw() == 0x9C01) {
                logger.warning("SATOCHIPLIB: cardBip32GetExtendedKey: Reset memory...");
                // reset memory flag
                p2 = (byte) (p2 ^ 0x80);
                plainApdu = new ApduCommand(
                        0xB0,
                        INS_BIP32_GET_EXTENDED_KEY,
                        p1,
                        p2,
                        data
                );
                respApdu = this.cardTransmit(plainApdu);
                // reset the flag then restart
                p2 = optionFlags;
                continue;
            }
            // other (unexpected) error
            if (respApdu.getSw() != 0x9000) {
                throw new Exception("SATOCHIPLIB: cardBip32GetExtendedKey:" +
                        "Unexpected error during BIP32 derivation. SW: " +
                        respApdu.getSw() + " " + respApdu.toHexString()
                );
            }
            // success
            if (respApdu.getSw() == 0x9000) {
                logger.warning("SATOCHIPLIB: cardBip32GetExtendedKey: return 0x9000...");
                byte[] response = respApdu.getData();
                if ((optionFlags & 0x04) == 0x04) { // BIP85
                    //todo: enable?
                    logger.warning("SATOCHIPLIB: cardBip32GetExtendedKey: in BIP85");
                    extendedKey = parser.parseBip85GetExtendedKey(respApdu)[0];
                    extendedKeyHex = parser.toHexString(extendedKey);
                } else if ((optionFlags & 0x02) == 0x00) { // BIP32 pubkey
                    logger.warning("SATOCHIPLIB: cardBip32GetExtendedKey: in BIP39");
                    if ((response[32] & 0x80) == 0x80) {
                        logger.info("SATOCHIPLIB: cardBip32GetExtendedKey: Child Derivation optimization...");
                        throw new Exception("Unsupported legacy option during BIP32 derivation");
                    }
                    byte[][] extendedKeyData = parser.parseBip32GetExtendedKey(respApdu);
                    extendedKey = extendedKeyData[0];// todo: return array
                    extendedChaincode = extendedKeyData[1];
                    extendedKeyHex = parser.toHexString(extendedKey);
                    return extendedKeyData;
                } else { // BIP32 privkey
                    byte[][] extendedPrivKeyData = parser.parseBip32GetExtendedKey(respApdu);
                    extendedPrivKey = extendedPrivKeyData[0];
                    extendedPrivKeyHex = parser.toHexString(extendedPrivKey);
                    return extendedPrivKeyData;
                }
            }
        }
    }

    // todo: only for testing testCardBip32GetExtendedkeyBip85
    public byte[] getExtendedKey() {
        return extendedKey;
    }

    /*
     *  Get the BIP32 xpub for given path.
     *
     *  Parameters:
     *  path (str): the path; if given as a string, it will be converted to bytes (4 bytes for each path index)
     *  xtype (str): the type of transaction such as  'standard', 'p2wpkh-p2sh', 'p2wpkh', 'p2wsh-p2sh', 'p2wsh'
     *  is_mainnet (bool): is mainnet or testnet
     *
     *  Return:
     *  xpub (str): the corresponding xpub value
     */
    public String cardBip32GetXpub(String path, int xtype, Integer sid) throws Exception {
        Bip32Path bip32Path = parser.parseBip32PathToBytes(path);
        return cardBip32GetXpub(bip32Path, xtype, sid);
    }
    public String cardBip32GetXpub(Bip32Path bip32Path, int xtype, Integer sid) throws Exception {
        logger.warning("SATOCHIPLIB: cardBip32GetXpub");

        byte[] childPubkey, childChaincode;
        byte optionFlags = (byte) 0x40;

        // Get extended key
        logger.warning("SATOCHIPLIB: cardBip32GetXpub: getting card cardBip32GetExtendedKey");
        cardBip32GetExtendedKey(bip32Path, optionFlags, sid);
        logger.warning("SATOCHIPLIB: cardBip32GetXpub: got it "+ extendedKey.length);

        childPubkey = extendedKey;
        childChaincode = extendedChaincode;

        // Pubkey should be in compressed form
        if (extendedKey.length != 33) {
            childPubkey = parser.compressPubKey(extendedKey);
        }

        //Bip32Path parsedPath = parser.parseBip32PathToBytes(path);
        int depth = bip32Path.getDepth();
        byte[] bytePath = bip32Path.getBytes();
        byte[] fingerprintBytes = new byte[4];
        byte[] childNumberBytes = new byte[4];

        if (depth > 0) {
            // Get parent info
            //String parentPath = parser.getBip32PathParentPath(path);
            //logger.warning("SATOCHIPLIB: cardBip32GetXpub: parentPathString: "+ parentPath);
            byte[] parentBytePath = Arrays.copyOfRange(bytePath, 0, bytePath.length-4);
            //parentBytePath = Arrays.copyOfRange(bytePath, 0, bytePath.length-1-4);
            Bip32Path parentBip32Path = new Bip32Path(depth-1, parentBytePath);

            cardBip32GetExtendedKey(parentBip32Path, optionFlags, sid);
            byte[] parentPubkeyBytes = extendedKey;

            // Pubkey should be in compressed form
            if (parentPubkeyBytes.length != 33) {
                parentPubkeyBytes = parser.compressPubKey(parentPubkeyBytes);
            }

            //fingerprintBytes = Arrays.copyOfRange(digestRipeMd160(Sha256Hash.hash(parentPubkeyBytes)), 0, 4);
            fingerprintBytes = Arrays.copyOfRange(digestRipeMd160(Wally.sha256(parentPubkeyBytes)), 0, 4); // debug

            childNumberBytes = Arrays.copyOfRange(bytePath, bytePath.length - 4, bytePath.length);
        }

        byte[] xtypeBytes = ByteBuffer.allocate(4).putInt((int) xtype).array();
        byte[] xpubBytes = new byte[78];
        System.arraycopy(xtypeBytes, 0, xpubBytes, 0, 4);
        xpubBytes[4] = (byte) depth;
        System.arraycopy(fingerprintBytes, 0, xpubBytes, 5, 4);
        System.arraycopy(childNumberBytes, 0, xpubBytes, 9, 4);
        System.arraycopy(childChaincode, 0, xpubBytes, 13, 32);
        System.arraycopy(childPubkey, 0, xpubBytes, 45, 33);

        if (xpubBytes.length != 78) {
            throw new Exception("wrongXpubLength " + xpubBytes.length + " " + 78);
        }

        //String xpub = encodeChecked(xpubBytes);
        String xpub =  Wally.base58check_from_bytes(xpubBytes);
        logger.warning("SATOCHIPLIB: cardBip32GetXpub() xpub: " + xpub);
        return xpub;
    }

//    private String encodeChecked(byte[] bytes) {
//
//        byte[] checksum = calculateChecksum(bytes);
//        byte[] checksummedBytes = new byte[bytes.length + 4];
//        System.arraycopy(bytes, 0, checksummedBytes, 0, bytes.length);
//        System.arraycopy(checksum, 0, checksummedBytes, bytes.length, 4);
//        return Base58.encode(checksummedBytes);
//    }

//    private byte[] calculateChecksum(byte[] bytes) {
//        byte[] hash = Wally.sha256d(bytes); //Sha256Hash.hashTwice(bytes);
//        byte[] checksum = new byte[4];
//        System.arraycopy(hash, 0, checksum, 0, 4);
//        return checksum;
//    }

    public static byte[] digestRipeMd160(byte[] input) {
        RIPEMD160Digest digest = new RIPEMD160Digest();
        digest.update(input, 0, input.length);
        byte[] ripmemdHash = new byte[20];
        digest.doFinal(ripmemdHash, 0);
        return ripmemdHash;
    }

    // public ApduResponse cardSignMessage(int keyNbr, byte[] pubkey, String message, byte[] hmac, String altcoin){
    // }

    /****************************************
     *             SIGNATURES              *
     ****************************************/

    public ApduResponse cardSignHash(byte keynbr, byte[] txhash, byte[] chalresponse) {

        byte[] data;
        if (txhash.length != 32) {
            throw new RuntimeException("Wrong txhash length (should be 32)");
        }
        if (chalresponse == null) {
            data = new byte[32];
            System.arraycopy(txhash, 0, data, 0, txhash.length);
        } else if (chalresponse.length == 20) {
            data = new byte[32 + 2 + 20];
            int offset = 0;
            System.arraycopy(txhash, 0, data, offset, txhash.length);
            offset += 32;
            data[offset++] = (byte) 0x80; // 2 middle bytes for 2FA flag
            data[offset++] = (byte) 0x00;
            System.arraycopy(chalresponse, 0, data, offset, chalresponse.length);
        } else {
            throw new RuntimeException("Wrong challenge-response length (should be 20)");
        }
        ApduCommand plainApdu = new ApduCommand(0xB0, INS_SIGN_TRANSACTION_HASH, keynbr, 0x00, data);

        logger.info("SATOCHIPLIB: C-APDU cardSignTransactionHash:" + plainApdu.toHexString());
        ApduResponse respApdu = this.cardTransmit(plainApdu);
        logger.info("SATOCHIPLIB: R-APDU cardSignTransactionHash:" + respApdu.toHexString());
        // TODO: check SW code for particular status

        return respApdu;
    }

    /****************************************
     *               2FA commands            *
     ****************************************/



//    public String getCardLabel() {
//        logger.info("SATOCHIPLIB: getCardLabel START");
//
//        ApduCommand plainApdu = new ApduCommand(0xB0, INS_CARD_LABEL, 0x00, 0x01, new byte[0]);
//        logger.info("SATOCHIPLIB: C-APDU getCardLabel:"+ plainApdu.toHexString());
//        ApduResponse respApdu = this.cardTransmit(plainApdu);
//        logger.info("SATOCHIPLIB: R-APDU getCardLabel:"+ respApdu.toHexString());
//        int sw = respApdu.getSw();
//        String label;
//        if (sw == 0x9000){
//            byte labelSize = respApdu.getData()[0];
//            try {
//                label = new String(respApdu.getData(), 1, labelSize, StandardCharsets.UTF_8);
//            } catch (Exception e) {
//                logger.warning("SATOCHIPLIB: getCardLabel UnicodeDecodeError while decoding card label!");
//                label = new String(respApdu.getData(), 1, respApdu.getData().length - 1, StandardCharsets.UTF_8);
//            }
//        } else if (sw == 0x6D00) {
//            logger.info("SATOCHIPLIB: getCardLabel  label not set:" + sw);
//            label = "(none)";
//        } else {
//            logger.warning("SATOCHIPLIB: getCardLabel Error while recovering card label:" + sw);
//            label = "(unknown)";
//        }
//        return label;
//    }
//
//    public Boolean setCardLabel(String label) {
//        logger.info("SATOCHIPLIB: setCardLabel START");
//
//        byte[] labelData = label.getBytes(StandardCharsets.UTF_8);
//        byte[] data = new byte[1 + labelData.length];
//        data[0] = (byte) labelData.length;
//        System.arraycopy(labelData, 0, data, 1, labelData.length);
//
//        ApduCommand plainApdu = new ApduCommand(0xB0, INS_CARD_LABEL, 0x00, 0x00, data);
//        logger.info("SATOCHIPLIB: C-APDU setCardLabel:"+ plainApdu.toHexString());
//        ApduResponse respApdu = this.cardTransmit(plainApdu);
//        logger.info("SATOCHIPLIB: R-APDU setCardLabel:"+ respApdu.toHexString());
//        int sw = respApdu.getSw();
//        return sw == 0x9000;
//    }

    /****************************************
     *            PKI commands              *
     ****************************************/

//    public ApduResponse cardExportPersoPubkey(){
//
//        ApduCommand plainApdu = new ApduCommand(0xB0, INS_EXPORT_PKI_PUBKEY, 0x00, 0x00, new byte[0]);
//        logger.info("SATOCHIPLIB: C-APDU cardExportPersoPubkey:"+ plainApdu.toHexString());
//        ApduResponse respApdu = this.cardTransmit(plainApdu);
//        logger.info("SATOCHIPLIB: R-APDU cardExportPersoPubkey:"+ respApdu.toHexString());
//
//        return respApdu;
//    }
//
//    public String cardExportPersoCertificate() throws ApduException {
//
//        // init
//        byte p1 = 0x00;
//        byte p2 = 0x01; // init
//        ApduCommand plainApdu = new ApduCommand(0xB0, INS_EXPORT_PKI_CERTIFICATE, p1, p2, new byte[0]);
//        logger.info("SATOCHIPLIB: C-APDU cardExportPersoCertificate - init:"+ plainApdu.toHexString());
//        ApduResponse respApdu = this.cardTransmit(plainApdu);
//        logger.info("SATOCHIPLIB: R-APDU cardExportPersoCertificate - init:"+ respApdu.toHexString());
//        respApdu.checkOK();
//        int sw = respApdu.getSw();
//        byte[] response = null;
//        int certificate_size = 0;
//        if (sw == 0x9000){
//            response= respApdu.getData();
//            certificate_size= (response[0] & 0xFF) * 256 + (response[1] & 0xFF);
//            logger.warning("SATOCHIPLIB: personalization certificate export: code:" + sw + "certificate size: " + certificate_size);
//        } else if (sw == 0x6D00){
//            logger.warning("SATOCHIPLIB: Error during personalization certificate export: command unsupported(0x6D00)");
//            return "Error during personalization certificate export: command unsupported(0x6D00)";
//        } else if (sw == 0x0000){
//            logger.warning("SATOCHIPLIB: Error during personalization certificate export: no card present(0x0000)");
//            return "Error during personalization certificate export: no card present(0x0000)";
//        }
//
//        if (certificate_size==0){
//            return ""; //new byte[0]; //"(empty)";
//        }
//
//        // UPDATE apdu: certificate data in chunks
//        p2= 0x02; //update
//        byte[] certificate = new byte[certificate_size];//certificate_size*[0]
//        short chunk_size = 128;
//        byte[] chunk = new byte[chunk_size];
//        int remaining_size = certificate_size;
//        int cert_offset = 0;
//        byte[] data = new byte[4];
//        while(remaining_size > 128){
//            // data=[ chunk_offset(2b) | chunk_size(2b) ]
//            data[0]= (byte) ((cert_offset>>8)&0xFF);
//            data[1]= (byte) (cert_offset&0xFF);
//            data[2]= (byte) ((chunk_size>>8)&0xFF);;
//            data[3]= (byte) (chunk_size & 0xFF);
//            plainApdu = new ApduCommand(0xB0, INS_EXPORT_PKI_CERTIFICATE, p1, p2, data);
//            logger.warning("SATOCHIPLIB: C-APDU cardExportPersoCertificate - update:"+ plainApdu.toHexString());
//            respApdu = this.cardTransmit(plainApdu);
//            logger.warning("SATOCHIPLIB: R-APDU cardExportPersoCertificate - update:"+ respApdu.toHexString());
//            respApdu.checkOK();
//            // update certificate
//            response= respApdu.getData();
//            System.arraycopy(response, 0, certificate, cert_offset, chunk_size);
//            remaining_size-=chunk_size;
//            cert_offset+=chunk_size;
//        }
//
//        // last chunk
//        data[0]= (byte) ((cert_offset>>8)&0xFF);
//        data[1]= (byte) (cert_offset&0xFF);
//        data[2]= (byte) ((remaining_size>>8)&0xFF);;
//        data[3]= (byte) (remaining_size & 0xFF);
//        plainApdu = new ApduCommand(0xB0, INS_EXPORT_PKI_CERTIFICATE, p1, p2, data);
//        logger.warning("SATOCHIPLIB: C-APDU cardExportPersoCertificate - final:"+ plainApdu.toHexString());
//        respApdu = this.cardTransmit(plainApdu);
//        logger.warning("SATOCHIPLIB: R-APDU cardExportPersoCertificate - final:"+ respApdu.toHexString());
//        respApdu.checkOK();
//        // update certificate
//        response= respApdu.getData();
//        System.arraycopy(response, 0, certificate, cert_offset, remaining_size);
//        cert_offset+=remaining_size;
//
//        // parse and return raw certificate
//        String cert_pem= parser.convertBytesToStringPem(certificate);
//        logger.warning("SATOCHIPLIB: cardExportPersoCertificate checking certificate:" + Arrays.toString(certificate));
//
//        return cert_pem;
//    }
//
//    public ApduResponse cardChallengeResponsePerso(byte[] challenge_from_host){
//
//        ApduCommand plainApdu = new ApduCommand(0xB0, INS_CHALLENGE_RESPONSE_PKI, 0x00, 0x00, challenge_from_host);
//        logger.info("SATOCHIPLIB: C-APDU cardChallengeResponsePerso:"+ plainApdu.toHexString());
//        ApduResponse respApdu = this.cardTransmit(plainApdu);
//        logger.info("SATOCHIPLIB: R-APDU cardChallengeResponsePerso:"+ respApdu.toHexString());
//
//        return respApdu;
//    }
//
//    public String[] cardVerifyAuthenticity(){
//
//        String txt_error="";
//        String txt_ca="(empty)";
//        String txt_subca="(empty)";
//        String txt_device="(empty)";
//        final String FAIL= "FAIL";
//        final String OK= "OK";
//
//        // get certificate from device
//        String cert_pem="";
//        try{
//            cert_pem = cardExportPersoCertificate();
//            logger.warning("SATOCHIPLIB: Cert PEM: "+ cert_pem);
//        } catch (Exception e){
//            logger.warning("SATOCHIPLIB: Exception in cardVerifyAuthenticity:"+ e);
//            txt_error= "Unable to retrieve device certificate!";
//            //String[] out = new String [5];
//            //out[0]={"a","b","c","d"};
//            String[] out = new String [] {FAIL, txt_ca, txt_subca, txt_device, txt_error};
//            return out;
//        }
//
//        // verify certificate chain
//        boolean isValidated= false;
//        PublicKey pubkeyDevice= null;
//        try{
//            // load certs
//            InputStream isCa = this.getClass().getClassLoader().getResourceAsStream("cert/ca.cert");
//            InputStream isSubca;
//            if (cardType.equals("satochip")) {
//                isSubca = this.getClass().getClassLoader().getResourceAsStream("cert/subca-satochip.cert");
//            } else if (cardType.equals("seedkeeper")) {
//                isSubca = this.getClass().getClassLoader().getResourceAsStream("cert/subca-seedkeeper.cert");
//            } else {
//                isSubca = this.getClass().getClassLoader().getResourceAsStream("cert/subca-satodime.cert");
//            }
//            InputStream isDevice = new ByteArrayInputStream(cert_pem.getBytes(StandardCharsets.UTF_8));
//            // gen certs
//            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", "BC"); // without BC provider, validation fails...
//            Certificate certCa = certificateFactory.generateCertificate(isCa);
//            txt_ca= certCa.toString();
//            logger.warning("SATOCHIPLIB: certCa: " + txt_ca);
//            Certificate certSubca = certificateFactory.generateCertificate(isSubca);
//            txt_subca= certSubca.toString();
//            logger.warning("SATOCHIPLIB: certSubca: " + txt_subca);
//            Certificate certDevice = certificateFactory.generateCertificate(isDevice);
//            logger.warning("SATOCHIPLIB: certDevice: " + certDevice);
//            txt_device= certDevice.toString();
//            logger.warning("SATOCHIPLIB: txtCertDevice: " + txt_device);
//
//            pubkeyDevice= certDevice.getPublicKey();
//            logger.warning("SATOCHIPLIB: certDevice pubkey: " + pubkeyDevice.toString());
//
//            // cert chain
//            Certificate[] chain= new Certificate[2];
//            chain[0]= certDevice;
//            chain[1]= certSubca;
//            CertPath certPath = certificateFactory.generateCertPath(Arrays.asList(chain));
//
//            // keystore
//            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
//            ks.load(null, null);
//            KeyStore.TrustedCertificateEntry tcEntry= new KeyStore.TrustedCertificateEntry(certCa);
//            //KeyStore.TrustedCertificateEntry tcEntry= new KeyStore.TrustedCertificateEntry(certSubca);
//            ks.setEntry("SatodimeCA", tcEntry, null);
//
//            // validator
//            PKIXParameters params = new PKIXParameters(ks);
//            params.setRevocationEnabled(false);
//            CertPathValidator certValidator = CertPathValidator.getInstance(CertPathValidator.getDefaultType()); // PKIX
//            certValidator.validate(certPath, params);
//            isValidated=true;
//            logger.info("SATOCHIPLIB: Certificate chain validated!");
//
//        }catch (Exception e){
//            logger.warning("SATOCHIPLIB: Exception in cardVerifyAuthenticity:"+ e);
//            e.printStackTrace();
//            isValidated=false;
//            txt_error= "Failed to validate certificate chain! \r\n\r\n" + e.toString();
//            String[] out = new String [] {FAIL, txt_ca, txt_subca, txt_device, txt_error};
//            return out;
//        }
//
//        // perform challenge-response with the card to ensure that the key is correctly loaded in the device
//        try{
//            SecureRandom random = new SecureRandom();
//            byte[] challenge_from_host= new byte[32];
//            random.nextBytes(challenge_from_host);
//            ApduResponse rapduChalresp= cardChallengeResponsePerso(challenge_from_host);
//            byte[][] parsedData= parser.parseVerifyChallengeResponsePerso(rapduChalresp);
//            byte[] challenge_from_device= parsedData[0];
//            byte[] sig= parsedData[1];
//
//            // build challenge byte[]
//            int offset=0;
//            String chalHeaderString=  "Challenge:";
//            byte[] chalHeaderBytes= chalHeaderString.getBytes(StandardCharsets.UTF_8);
//            byte[] chalFullBytes= new byte[chalHeaderBytes.length + 32 + 32];
//            System.arraycopy(chalHeaderBytes, 0, chalFullBytes, offset, chalHeaderBytes.length);
//            offset+= chalHeaderBytes.length;
//            System.arraycopy(challenge_from_device, 0, chalFullBytes, offset, 32);
//            offset+= 32;
//            System.arraycopy(challenge_from_host, 0, chalFullBytes, offset, 32);
//
//            // verify sig with pubkeyDevice
//            byte[] pubkey= new byte[65];
//            byte[] pubkeyEncoded= pubkeyDevice.getEncoded();
//            System.arraycopy(pubkeyEncoded, (pubkeyEncoded.length-65), pubkey, 0, 65); // extract pubkey from ASN1 encoding
//            boolean isChalrespOk= parser.verifySig(chalFullBytes, sig, pubkey);
//            if (!isChalrespOk){
//                throw new RuntimeException("Failed to verify challenge-response signature!");
//            }
//            // TODO: pubkeyDevice should be equal to authentikey
//        }catch (Exception e){
//            logger.warning("SATOCHIPLIB: Exception in cardVerifyAuthenticity:"+ e);
//            e.printStackTrace();
//            txt_error= "Failed to verify challenge-response! \r\n\r\n" + e.toString();
//            String[] out = new String [] {FAIL, txt_ca, txt_subca, txt_device, txt_error};
//            return out;
//        }
//
//        String[] out =  new String [] {OK, txt_ca, txt_subca, txt_device, txt_error};
//        return out;
//    }
}
