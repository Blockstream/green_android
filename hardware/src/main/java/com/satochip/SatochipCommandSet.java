package com.satochip;


import static com.satochip.Constants.*;


import androidx.annotation.NonNull;

import com.blockstream.libwally.Wally;

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

    private byte[] extendedKey = null;
    private byte[] extendedChaincode = null;

    // should be Satochip
    private String cardType = null;

    public static final byte[] SATOCHIP_AID = Hex.decode("5361746f43686970"); //SatoChip

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


    public SatochipParser getParser() {
        return parser;
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
     * Selects a Satochip instance. The applet is assumed to have been installed with its default AID.
     *
     * @return the raw card response
     * @throws IOException communication error
     */
    public ApduResponse cardSelect(String cardType) throws IOException {

        ApduCommand selectApplet;
        if (cardType.equals("satochip")) {
            selectApplet = new ApduCommand(0x00, 0xA4, 0x04, 0x00, SATOCHIP_AID);
        } else {
            selectApplet = new ApduCommand(0x00, 0xA4, 0x04, 0x00, null);
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

        } catch (WrongPINException | WrongPINLegacyException | BlockedPINException e) {
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

    /****************************************
     *                 BIP32                     *
     ****************************************/

    public byte[][] cardBip32GetExtendedKey(String stringPath) throws Exception {
        Bip32Path parsedPath = parser.parseBip32PathToBytes(stringPath);
        return cardBip32GetExtendedKey(parsedPath);
    }

    public byte[][] cardBip32GetExtendedKey(@NonNull Bip32Path parsedPath) throws Exception {
        logger.warning("SATOCHIPLIB: cardBip32GetExtendedKey");
        if (parsedPath.getDepth() > 10) {
            throw new Exception("Path length exceeds maximum depth of 10: " + parsedPath.getDepth());
        }

        byte p1 = parsedPath.getDepth().byteValue();
        byte optionFlags = (byte) 0x40;
        byte p2 = optionFlags;

        byte[] data = parsedPath.getBytes();

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
                logger.warning("SATOCHIPLIB: cardBip32GetExtendedKey: return 0x9000");
                byte[] response = respApdu.getData();
                if ((response[32] & 0x80) == 0x80) {
                    logger.info("SATOCHIPLIB: cardBip32GetExtendedKey: Child Derivation optimization...");
                    throw new Exception("Unsupported legacy option during BIP32 derivation");
                }
                byte[][] extendedKeyData = parser.parseBip32GetExtendedKey(respApdu);
                extendedKey = extendedKeyData[0];
                extendedChaincode = extendedKeyData[1];
                //String extendedKeyHex = SatochipParser.toHexString(extendedKey);
                return extendedKeyData;
            }
        }
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
    public String cardBip32GetXpub(String path, int xtype) throws Exception {
        Bip32Path bip32Path = parser.parseBip32PathToBytes(path);
        return cardBip32GetXpub(bip32Path, xtype);
    }
    public String cardBip32GetXpub(Bip32Path bip32Path, int xtype) throws Exception {
        logger.warning("SATOCHIPLIB: cardBip32GetXpub");

        byte[] childPubkey, childChaincode;

        // Get extended key
        logger.warning("SATOCHIPLIB: cardBip32GetXpub: getting card cardBip32GetExtendedKey");
        cardBip32GetExtendedKey(bip32Path);
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
            byte[] parentBytePath = Arrays.copyOfRange(bytePath, 0, bytePath.length-4);
            Bip32Path parentBip32Path = new Bip32Path(depth-1, parentBytePath);

            cardBip32GetExtendedKey(parentBip32Path);
            byte[] parentPubkeyBytes = extendedKey;

            // Pubkey should be in compressed form
            if (parentPubkeyBytes.length != 33) {
                parentPubkeyBytes = parser.compressPubKey(parentPubkeyBytes);
            }

            fingerprintBytes = Arrays.copyOfRange(digestRipeMd160(Wally.sha256(parentPubkeyBytes)), 0, 4);

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

        String xpub =  Wally.base58check_from_bytes(xpubBytes);
        logger.info("SATOCHIPLIB: cardBip32GetXpub() xpub: " + xpub);
        return xpub;
    }

    public byte[] cardBip32GetLiquidMasterBlindingKey() throws Exception {
        logger.warning("SATOCHIPLIB: cardBip32GetLiquidMasterBlindingKey");

        byte p1 = 0x00;
        byte p2 = 0x00;
        byte[] data = new byte[0];
        ApduCommand plainApdu = new ApduCommand(
                0xB0,
                0x7D,
                p1,
                p2,
                data
        );
        ApduResponse respApdu = this.cardTransmit(plainApdu);

        if (respApdu.getSw() != 0x9000) {
            throw new Exception("SATOCHIPLIB: cardBip32GetLiquidMasterBlindingKey error: " + respApdu.toHexString());
        }

        byte[] response = respApdu.getData();
        int offset=0;
        int keySize= 256*(response[offset++] & 0xff) + response[offset++];
        byte[] blindingKey= new byte[keySize];
        System.arraycopy(response, offset, blindingKey, 0, keySize);
        offset+=keySize;

        int sigSize= 256*response[offset++] + response[offset++];
        byte[] sig= new byte[sigSize];
        System.arraycopy(response, offset, sig, 0, sigSize);
        offset+=sigSize;

        return blindingKey;
    }

    public static byte[] digestRipeMd160(byte[] input) {
        RIPEMD160Digest digest = new RIPEMD160Digest();
        digest.update(input, 0, input.length);
        byte[] ripmemdHash = new byte[20];
        digest.doFinal(ripmemdHash, 0);
        return ripmemdHash;
    }

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

}
