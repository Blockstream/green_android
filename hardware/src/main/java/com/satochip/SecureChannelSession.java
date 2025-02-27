package com.satochip;

import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.macs.HMac;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.logging.Logger;
import java.nio.ByteBuffer;

/**
 * Handles a SecureChannel session with the card.
 */
public class SecureChannelSession {
  
  private static final Logger logger = Logger.getLogger("org.satochip.client");
  
  public static final int SC_SECRET_LENGTH = 16;
  public static final int SC_BLOCK_SIZE = 16; 
  public static final int IV_SIZE = 16; 
  public static final int MAC_SIZE= 20;
  
  // secure channel constants
  private final static byte INS_INIT_SECURE_CHANNEL = (byte) 0x81;
  private final static byte INS_PROCESS_SECURE_CHANNEL = (byte) 0x82;
  private final static short SW_SECURE_CHANNEL_REQUIRED = (short) 0x9C20;
  private final static short SW_SECURE_CHANNEL_UNINITIALIZED = (short) 0x9C21;
  private final static short SW_SECURE_CHANNEL_WRONG_IV= (short) 0x9C22;
  private final static short SW_SECURE_CHANNEL_WRONG_MAC= (short) 0x9C23;

  private boolean initialized_secure_channel= false;
  
  // secure channel keys
  private byte[] secret;
  private byte[] iv;
  private int ivCounter;
  byte[] derived_key;
  byte[] mac_key;
  
  // for ECDH
  ECParameterSpec ecSpec;
  private KeyPair keyPair;
  private byte[] publicKey;
    
  // for session encryption
  private Cipher sessionCipher;
  private SecretKeySpec sessionEncKey;
  private SecureRandom random;
  private boolean open;
	
  /**
   * Constructs a SecureChannel session on the client.
   */
  public SecureChannelSession() {
    random = new SecureRandom();
    open = false;
      
    try {
      // generate keypair
      Security.removeProvider("BC");
      Security.insertProviderAt(new BouncyCastleProvider(), 1);
      ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
      KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
      g.initialize(ecSpec, random);
      keyPair = g.generateKeyPair();
      publicKey = ((ECPublicKey) keyPair.getPublic()).getQ().getEncoded(false);
    } catch (Exception e) {
      logger.warning("SATOCHIPLIB: Exception in SecureChannelSession() constructor: "+ e);
      System.out.println("SATOCHIPLIB SecureChannelSession() exception: "+e);
      e.printStackTrace();
      throw new RuntimeException("Is BouncyCastle in the classpath?", e);
    }
  }
  
  
  /**
   * Generates a pairing secret. This should be called before each session. The public key of the card is used as input
   * for the EC-DH algorithm. The output is stored as the secret.
   *
   * @param keyData the public key returned by the applet as response to the SELECT command
   */
  public void initiateSecureChannel(byte[] keyData) { //TODO: check keyData format
    try {
      
      // Diffie-Hellman
      // ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
      // KeyPairGenerator g = KeyPairGenerator.getInstance("ECDH", "BC");
      // g.initialize(ecSpec, random);
      // KeyPair keyPair = g.generateKeyPair();
      // publicKey = ((ECPublicKey) keyPair.getPublic()).getQ().getEncoded(false);
      
      KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH", "BC");
      keyAgreement.init(keyPair.getPrivate());

      ECPublicKeySpec cardKeySpec = new ECPublicKeySpec(ecSpec.getCurve().decodePoint(keyData), ecSpec);
      ECPublicKey cardKey = (ECPublicKey) KeyFactory.getInstance("ECDSA", "BC").generatePublic(cardKeySpec);

      keyAgreement.doPhase(cardKey, true);
      secret = keyAgreement.generateSecret();
      
      // derive session keys
      HMac hMac = new HMac(new SHA1Digest());
      hMac.init(new KeyParameter(secret));
      byte[] msg_key= "sc_key".getBytes();
      hMac.update(msg_key, 0, msg_key.length);
      byte[] out = new byte[20];
      hMac.doFinal(out, 0);
      derived_key= new byte[16];
      System.arraycopy(out, 0, derived_key, 0, 16);
      
      hMac.reset();
      byte[] msg_mac= "sc_mac".getBytes();
      hMac.update(msg_mac, 0, msg_mac.length);
      mac_key = new byte[20];
      hMac.doFinal(mac_key, 0);
      
      ivCounter= 1;
      initialized_secure_channel= true;
    } catch (Exception e) {
      logger.warning("SATOCHIPLIB: Exception in initiateSecureChannel: "+ e);
      System.out.println("SATOCHIPLIB initiateSecureChannel() exception: "+e);
      e.printStackTrace();
      throw new RuntimeException("Is BouncyCastle in the classpath?", e);
    }
  }
  
  public ApduCommand encrypt_secure_channel(ApduCommand plainApdu){
    
    try {
      
      byte[] plainBytes= plainApdu.serialize();
      
      // set iv
      iv = new byte[SC_BLOCK_SIZE];
      random.nextBytes(iv);
      ByteBuffer bb = ByteBuffer.allocate(4); 
      bb.putInt(ivCounter);  // big endian
      byte[] ivCounterBytes= bb.array();
      System.arraycopy(ivCounterBytes, 0, iv, 12, 4);
      ivCounter+=2;
      logger.info("SATOCHIPLIB: ivCounter: "+ ivCounter);
      logger.info("SATOCHIPLIB: ivCounterBytes: "+ SatochipParser.toHexString(ivCounterBytes));
      logger.info("SATOCHIPLIB: iv: "+ SatochipParser.toHexString(iv));
      
      // encrypt data
      IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
      sessionEncKey = new SecretKeySpec(derived_key, "AES");
      sessionCipher = Cipher.getInstance("AES/CBC/PKCS7PADDING", "BC");
      sessionCipher.init(Cipher.ENCRYPT_MODE, sessionEncKey, ivParameterSpec);
      byte[] encrypted = sessionCipher.doFinal(plainBytes);
      // logger.info("SATOCHIPLIB: encrypted: "+ SatochipParser.toHexString(derived_key));
      // logger.info("SATOCHIPLIB: encrypted: "+ SatochipParser.toHexString(encrypted));
      
      // mac
      int offset= 0;
      byte[] data_to_mac= new byte[IV_SIZE + 2 + encrypted.length];
      System.arraycopy(iv, offset, data_to_mac, offset, IV_SIZE);
      offset+=IV_SIZE;
      data_to_mac[offset++]= (byte)(encrypted.length>>8);
      data_to_mac[offset++]= (byte)(encrypted.length%256);
      System.arraycopy(encrypted, 0, data_to_mac, offset, encrypted.length);
      // logger.info("SATOCHIPLIB: data_to_mac: "+ SatochipParser.toHexString(data_to_mac));
      
      HMac hMac = new HMac(new SHA1Digest());
      hMac.init(new KeyParameter(mac_key));
      hMac.update(data_to_mac, 0, data_to_mac.length);
      byte[] mac = new byte[20];
      hMac.doFinal(mac, 0);
      // logger.info("SATOCHIPLIB: mac: "+ SatochipParser.toHexString(mac));
      
      //data= list(iv) + [len(ciphertext)>>8, len(ciphertext)&0xff] + list(ciphertext) + [len(mac)>>8, len(mac)&0xff] + list(mac)
      byte[] data= new byte[IV_SIZE + 2 + encrypted.length + 2 + MAC_SIZE];
      offset= 0;
      System.arraycopy(iv, offset, data, offset, IV_SIZE);
      offset+=IV_SIZE;
      data[offset++]= (byte)(encrypted.length>>8);
      data[offset++]= (byte)(encrypted.length%256);
      System.arraycopy(encrypted, 0, data, offset, encrypted.length);
      offset+=encrypted.length;
      data[offset++]= (byte)(mac.length>>8);
      data[offset++]= (byte)(mac.length%256);
      System.arraycopy(mac, 0, data, offset, mac.length);
      // logger.info("SATOCHIPLIB: data: "+ SatochipParser.toHexString(data));
      
      // convert to C-APDU
      ApduCommand encryptedApdu= new ApduCommand(0xB0, INS_PROCESS_SECURE_CHANNEL, 0x00, 0x00, data);
      return encryptedApdu;
      
    } catch (Exception e) {
      e.printStackTrace();
      logger.warning("SATOCHIPLIB: Exception in encrypt_secure_channel: "+ e);
      System.out.println("SATOCHIPLIB encrypt_secure_channel() exception: "+e);
      e.printStackTrace();
      throw new RuntimeException("Is BouncyCastle in the classpath?", e);
    }
    
  }
  
  public ApduResponse decrypt_secure_channel(ApduResponse encryptedApdu){
	  
    try {
    
      byte[] encryptedBytes= encryptedApdu.getData();
      if (encryptedBytes.length==0){
        return encryptedApdu; // no decryption needed
      } else if (encryptedBytes.length<18){
        throw new RuntimeException("Encrypted response has wrong length!");
      }
      
      byte[] iv= new byte[IV_SIZE];
      int offset= 0;
      System.arraycopy(encryptedBytes, offset, iv, 0, IV_SIZE);
      offset+=IV_SIZE;
      int ciphertext_size= ((encryptedBytes[offset++] & 0xff)<<8) + (encryptedBytes[offset++] & 0xff);
      if ((encryptedBytes.length - offset)!= ciphertext_size){
        throw new RuntimeException("Encrypted response has wrong length!");
      }
      byte[] ciphertext= new byte[ciphertext_size];
      System.arraycopy(encryptedBytes, offset, ciphertext, 0, ciphertext.length);
      
      // decrypt data
      IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
      sessionEncKey = new SecretKeySpec(derived_key, "AES");
      sessionCipher = Cipher.getInstance("AES/CBC/PKCS7PADDING", "BC");
      sessionCipher.init(Cipher.DECRYPT_MODE, sessionEncKey, ivParameterSpec);
      byte[] decrypted = sessionCipher.doFinal(ciphertext);
      
      ApduResponse plainResponse= new ApduResponse(decrypted, (byte)0x90, (byte)0x00);
      return plainResponse;
      
    } catch (Exception e) {
      e.printStackTrace();
      logger.warning("SATOCHIPLIB: Exception in decrypt_secure_channel: "+ e);
      throw new RuntimeException("Exception during secure channel decryption: ", e);
    }
    
  }
  
  public boolean initializedSecureChannel(){
	  return initialized_secure_channel;
  }
  
  public byte[] getPublicKey(){
    return publicKey;
  }
  
  public void resetSecureChannel(){
    initialized_secure_channel= false;
  }
  
}
