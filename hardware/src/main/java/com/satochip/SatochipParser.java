package com.satochip;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve;
import org.bouncycastle.util.Properties;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.math.BigInteger;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;
import java.util.logging.Logger;

public class SatochipParser{
    
    private static final Logger logger = Logger.getLogger("org.satochip.client");
    
    public static final String HEXES = "0123456789ABCDEF";
    private static final X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");
    public static final ECDomainParameters CURVE;
    public static final BigInteger HALF_CURVE_ORDER, CURVE_ORDER;
    static {
        // Tell Bouncy Castle to precompute data that's needed during secp256k1 calculations.
        //FixedPointUtil.precompute(CURVE_PARAMS.getG());
        CURVE = new ECDomainParameters(CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(), CURVE_PARAMS.getN(), CURVE_PARAMS.getH());
        CURVE_ORDER= CURVE_PARAMS.getN();
        HALF_CURVE_ORDER = CURVE_PARAMS.getN().shiftRight(1);
    }
    
    private byte[] authentikey= null;
    
    public SatochipParser(){

    }

    public byte[] compressPubKey(byte[] pubkey) throws Exception {
        if (pubkey.length == 33) {
            // Already compressed
            return pubkey;
        } else if (pubkey.length == 65) {
            // In uncompressed form
            byte[] pubkeyComp = Arrays.copyOfRange(pubkey, 0, 33);
            // Compute compression byte
            int parity = pubkey[64] % 2;
            if (parity == 0) {
                pubkeyComp[0] = (byte) 0x02;
            } else {
                pubkeyComp[0] = (byte) 0x03;
            }
            return pubkeyComp;
        } else {
            throw new Exception("Wrong public key length: " + pubkey.length + ", expected: 65");
        }
    }
  
   /****************************************
   *                  PARSER                *
   ****************************************/

//   public String getBip32PathParentPath(String bip32path) throws Exception {
//       System.out.println("In getBip32PathParentPath");
//       String[] splitPath = bip32path.split("/");
//       if (splitPath.length <= 1) {
//           throw new Exception("Invalid BIP32 path: " + bip32path);
//       }
//       String[] parentPathArray = Arrays.copyOf(splitPath, splitPath.length - 1);
//       String parentPath = String.join("/", parentPathArray);
//       return parentPath;
//   }

    public byte[][] parseBip85GetExtendedKey(ApduResponse rapdu){
        logger.warning("SATOCHIPLIB: parseBip85GetExtendedKey: Start ");

        try {
            byte[] data = rapdu.getData();
            logger.warning("SATOCHIPLIB: parseBip85GetExtendedKey data: " + toHexString(data));

            int entropySize = 256 * (data[0] & 0xFF) + (data[1] & 0xFF);
            byte[] entropyBytes = Arrays.copyOfRange(data, 2, 2 + entropySize);

            return new byte[][] {entropyBytes, new byte[0]};
        } catch(Exception e) {
            System.out.println("SATOCHIPLIB parseBip85GetExtendedKey() exception: "+e);
            e.printStackTrace();
            throw new RuntimeException("Is BouncyCastle in the classpath?", e);
        }
    }

   public Bip32Path parseBip32PathToBytes(String bip32path) throws Exception {
       logger.info("SATOCHIPLIB: parseBip32PathToBytes: Start ");

       String[] splitPath = bip32path.split("/");
       if (splitPath[0].equals("m")) {
           splitPath = Arrays.copyOfRange(splitPath, 1, splitPath.length);
       }

       int depth = splitPath.length;
       byte[] bytePath = new byte[depth * 4];

       int byteIndex = 0;
       for (int index = 0; index < depth; index++) {
           String subpathString = splitPath[index];
           long subpathInt;
           if (subpathString.endsWith("'") || subpathString.endsWith("h")) {
               subpathString = subpathString.replace("'", "").replace("h", "");
               try {
                   long tmp = Long.parseLong(subpathString);
                   subpathInt = tmp + 0x80000000L;
               } catch (NumberFormatException e) {
                   throw new Exception("Failed to parse Bip32 path: " + bip32path);
               }
           } else {
               try {
                   subpathInt = Long.parseLong(subpathString);
               } catch (NumberFormatException e) {
                   throw new Exception("Failed to parse Bip32 path: " + bip32path);
               }
           }
           byte[] subPathBytes = ByteBuffer.allocate(4).putInt((int) subpathInt).array();
           System.arraycopy(subPathBytes, 0, bytePath, byteIndex, subPathBytes.length);
           byteIndex += 4;
       }

       return new Bip32Path(depth, bytePath);
   }
  
    public byte[] parseInitiateSecureChannel(ApduResponse rapdu){
    
        try{
            byte[] data= rapdu.getData();
            logger.info("SATOCHIPLIB: parseInitiateSecureChannel data: " + toHexString(data));

            // data= [coordxSize | coordx | sig1Size | sig1 |  sig2Size | sig2]
            int offset=0;
            int coordxSize= 256*data[offset++] + data[offset++];

            byte[] coordx= new byte[coordxSize];
            System.arraycopy(data, offset, coordx, 0, coordxSize);
            offset+=coordxSize;

            // msg1 is [coordx_size | coordx]
            byte[] msg1= new byte[2+coordxSize];
            System.arraycopy(data, 0, msg1, 0, msg1.length);

            int sig1Size= 256*data[offset++] + data[offset++];
            byte[] sig1= new byte[sig1Size];
            System.arraycopy(data, offset, sig1, 0, sig1Size);
            offset+=sig1Size;

            // msg2 is [coordxSize | coordx | sig1Size | sig1]
            byte[] msg2= new byte[2+coordxSize + 2 + sig1Size];
            System.arraycopy(data, 0, msg2, 0, msg2.length);

            int sig2Size= 256*data[offset++] + data[offset++];
            byte[] sig2= new byte[sig2Size];
            System.arraycopy(data, offset, sig2, 0, sig2Size);
            offset+=sig2Size;

            byte[] pubkey= recoverPubkey(msg1, sig1, coordx);
            
            return pubkey;
        } catch(Exception e) {
            throw new RuntimeException("Exception in parseInitiateSecureChannel: ", e);
        }
    }


    public List<byte[]> parseInitiateSecureChannelGetPossibleAuthentikeys(ApduResponse rapdu){
    
        try{
            byte[] data= rapdu.getData();
            int dataLength = data.length;
            logger.info("SATOCHIPLIB: parseInitiateSecureChannel data: " + toHexString(data));

            // data= [coordxSize | coordx | sig1Size | sig1 |  sig2Size | sig2 | coordxSize(optional) | coordxAuthentikey(optional)]
            int offset=0;
            int coordxSize= 256*data[offset++] + data[offset++];

            byte[] coordx= new byte[coordxSize];
            System.arraycopy(data, offset, coordx, 0, coordxSize);
            offset+=coordxSize;

            // msg1 is [coordx_size | coordx]
            byte[] msg1= new byte[2+coordxSize];
            System.arraycopy(data, 0, msg1, 0, msg1.length);

            int sig1Size= 256*data[offset++] + data[offset++];
            byte[] sig1= new byte[sig1Size];
            System.arraycopy(data, offset, sig1, 0, sig1Size);
            offset+=sig1Size;

            // msg2 is [coordxSize | coordx | sig1Size | sig1]
            byte[] msg2= new byte[2+coordxSize + 2 + sig1Size];
            System.arraycopy(data, 0, msg2, 0, msg2.length);

            int sig2Size= 256*data[offset++] + data[offset++];
            byte[] sig2= new byte[sig2Size];
            System.arraycopy(data, offset, sig2, 0, sig2Size);
            offset+=sig2Size;

            // if authentikey coordx are available
            // (currently only for Seedkeeper v0.2 and higher)
            if (dataLength>offset+1){
                int coordxAuthentikeySize = 256*data[offset++] + data[offset++];
                if (dataLength>offset+coordxAuthentikeySize){}
                byte[] coordxAuthentikey= new byte[coordxAuthentikeySize];
                System.arraycopy(data, offset, coordxAuthentikey, 0, coordxAuthentikeySize);

                byte[] authentikey= recoverPubkey(msg2, sig2, coordxAuthentikey);
                List<byte[]> possibleAuthentikeys = new ArrayList<byte[]>();
                possibleAuthentikeys.add(authentikey);
                return possibleAuthentikeys;

            } else {
                // if authentikey coordx is not provided, two possible pubkeys can be recovered as par ECDSA properties
                // recover all possible authentikeys from msg2, sig2
                List<byte[]> possibleAuthentikeys = recoverPossiblePubkeys(msg2, sig2);

                return possibleAuthentikeys;
            }

        } catch(Exception e) {
            throw new RuntimeException("Exception in parseInitiateSecureChannelGetPossibleAuthentikeys:", e);
        }
    }

  
    public byte[] parseBip32GetAuthentikey(ApduResponse rapdu){
        try{
            byte[] data= rapdu.getData();
            logger.info("SATOCHIPLIB: parseBip32GetAuthentikey data: " + toHexString(data));
            // data: [coordx_size(2b) | coordx | sig_size(2b) | sig ]

            int offset=0;
            int coordxSize= 256*(data[offset++] & 0xff) + data[offset++]; 
            byte[] coordx= new byte[coordxSize];
            System.arraycopy(data, offset, coordx, 0, coordxSize);
            offset+=coordxSize;

            // msg1 is [coordx_size | coordx]
            byte[] msg1= new byte[2+coordxSize];
            System.arraycopy(data, 0, msg1, 0, msg1.length);

            int sig1Size= 256*data[offset++] + data[offset++];
            byte[] sig1= new byte[sig1Size];
            System.arraycopy(data, offset, sig1, 0, sig1Size);
            offset+=sig1Size;

            byte[] pubkey= recoverPubkey(msg1, sig1, coordx);
            authentikey= new byte[pubkey.length];
            System.arraycopy(pubkey, 0, authentikey, 0, pubkey.length);
            return pubkey;
        } catch(Exception e) {
            throw new RuntimeException("Exception during Authentikey recovery", e);
        }
    }
  
    public byte[] parseExportPkiPubkey(ApduResponse rapdu){
        try{
            byte[] data= rapdu.getData();
            logger.info("SATOCHIPLIB: parseExportPkiPubkey data: " + toHexString(data));
            // data: [autehntikey(65b) | sig_size(2b - option) | sig(option) ]
            byte[] pubkey= new byte[65];
            System.arraycopy(data, 0, pubkey, 0, pubkey.length);
            return pubkey;
        } catch(Exception e) {
            throw new RuntimeException("Exception during Authentikey recovery", e);
        }
    }
    public byte[][] parseBip32GetExtendedKey(ApduResponse rapdu){//todo: return a wrapped

        try{
            byte[] data= rapdu.getData();
            logger.info("SATOCHIPLIB: parseBip32GetExtendedKey data: " + toHexString(data));
            //data: [chaincode(32b) | coordx_size(2b) | coordx | sig_size(2b) | sig | sig_size(2b) | sig2]

            int offset=0;
            byte[] chaincode= new byte[32];
            System.arraycopy(data, offset, chaincode, 0, chaincode.length);
            offset+=32;

            int coordxSize= 256*(data[offset++] & 0x7f) + data[offset++]; // (data[32] & 0x80) is ignored (optimization flag)
            byte[] coordx= new byte[coordxSize];
            System.arraycopy(data, offset, coordx, 0, coordxSize);
            offset+=coordxSize;

            // msg1 is [chaincode | coordx_size | coordx]
            byte[] msg1= new byte[32+2+coordxSize];
            System.arraycopy(data, 0, msg1, 0, msg1.length);

            int sig1Size= 256*data[offset++] + data[offset++];
            byte[] sig1= new byte[sig1Size];
            System.arraycopy(data, offset, sig1, 0, sig1Size);
            offset+=sig1Size;

            // msg2 is [chaincode | coordxSize | coordx | sig1Size | sig1]
            byte[] msg2= new byte[32 + 2+coordxSize + 2 + sig1Size];
            System.arraycopy(data, 0, msg2, 0, msg2.length);

            int sig2Size= 256*data[offset++] + data[offset++];
            byte[] sig2= new byte[sig2Size];
            System.arraycopy(data, offset, sig2, 0, sig2Size);
            offset+=sig2Size;

            byte[] pubkey= recoverPubkey(msg1, sig1, coordx);

            // todo: recover from si2
            return new byte[][] {pubkey, chaincode};
        } catch(Exception e) {
            System.out.println("SATOCHIPLIB parseBip32GetExtendedKey() exception: "+e);
            e.printStackTrace();
            throw new RuntimeException("Is BouncyCastle in the classpath?", e);
        }
    }
  

  
    public BigInteger[] parse_rsi_from_dersig(byte[] hash, byte[] dersig, byte[] coordx){
    
        BigInteger[] sigBig = decodeFromDER(dersig);
        
        int recid= recoverRecId(hash, sigBig, coordx);
        if (recid==-1){
            throw new RuntimeException("Exception in parse_rsv_from_dersig: could not recover recid");
        }
        
        BigInteger[] rsi= new BigInteger[3];
        rsi[0]= sigBig[0];
        rsi[1]= sigBig[1];
        rsi[2]= BigInteger.valueOf(recid);
        
        return rsi;
    }
  
   /****************************************
   *             recovery  methods          *
   ****************************************/
  
    // based on https://github.com/bitcoinj/bitcoinj/blob/4dc4cf743df9de996282b1aa3fd1d092859774cb/core/src/main/java/org/bitcoinj/core/ECKey.java#L977
  
    public int recoverRecId(byte[] hash, BigInteger[] sigBig, byte[] coordx){
    
        ECPoint point=null;
        for (int recid=0; recid<4; recid++){
            point= Recover(hash, sigBig, recid, false);

            // convert to byte[]
            byte[] pubkey= point.getEncoded(false); // uncompressed
            byte[] coordx2= new byte[32];
            System.arraycopy(pubkey, 1, coordx2, 0, 32);

            // compare with known coordx
            if (Arrays.equals(coordx, coordx2)){
                logger.info("SATOCHIPLIB: Found coordx: " + toHexString(coordx2));
                logger.info("SATOCHIPLIB: Found pubkey: " + toHexString(pubkey));
                return recid;
            }
        }
        return -1; // could not recover pubkey
  
    }
  
    public byte[] recoverPubkey(byte[] msg, byte[] sig, byte[] coordx) {
      
        // convert msg to hash
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA256", "BC");
        } catch(Exception e) {
            System.out.println("SATOCHIPLIB recoverPubkey() exception: "+e);
            e.printStackTrace();
            throw new RuntimeException("Is BouncyCastle in the classpath?", e);
        }
        byte[] hash= md.digest(msg);
        
        // convert sig array to big integer
        byte[] sigCompact= parseToCompactSignature(sig);
        byte[] r= new byte[32];
        System.arraycopy(sigCompact, 0, r, 0, 32);
        byte[] s= new byte[32];
        System.arraycopy(sigCompact, 32, s, 0, 32);
        
        BigInteger[] sigBig= new BigInteger[2] ;
        sigBig[0]= new BigInteger(1, r);
        sigBig[1]= new BigInteger(1, s);
        
        ECPoint point=null;
        for (int recid=0; recid<4; recid++){
            point= Recover(hash, sigBig, recid, false);

            // convert to byte[]
            byte[] pubkey= point.getEncoded(false); // uncompressed
            byte[] coordx2= new byte[32];
            System.arraycopy(pubkey, 1, coordx2, 0, 32);

            //BigInteger xx= point.getAffineX();
            //byte[] coordx2 = X9IntegerConverter.IntegerToBytes(xx, X9IntegerConverter.GetByteLength(CURVE));

            // compare with known coordx
            if (Arrays.equals(coordx, coordx2)){
                logger.info("SATOCHIPLIB: Found coordx: " + toHexString(coordx2));
                //BigInteger yy= point.getAffineY();
                //byte[] coordy = X9IntegerConverter.IntegerToBytes(yy, X9IntegerConverter.GetByteLength(CURVE));
                //byte[] pubkey = new byte[1 + coordx2.Length + coordy.length];
                //pubkey[0]= 0x04;
                //System.arraycopy(coordx2, 0, pubkey, 1, coordx2.lenght);
                //System.arraycopy(coordy, 0, pubkey, 33, coordy.lenght);
                logger.info("SATOCHIPLIB: Found pubkey: " + toHexString(pubkey));
                return pubkey;
            }
        }
        return null; // could not recover pubkey
    } 

    public List<byte[]> recoverPossiblePubkeys(byte[] msg, byte[] sig) {
        List<byte[]> pubkeys = new ArrayList<byte[]>();

        // convert msg to hash
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA256", "BC");
        } catch(Exception e) {
            System.out.println("SATOCHIPLIB recoverPossiblePubkeys() exception: "+e);
            e.printStackTrace();
            throw new RuntimeException("Is BouncyCastle in the classpath?", e);
        }
        byte[] hash= md.digest(msg);
        
        // convert sig array to big integer
        byte[] sigCompact= parseToCompactSignature(sig);
        byte[] r= new byte[32];
        System.arraycopy(sigCompact, 0, r, 0, 32);
        byte[] s= new byte[32];
        System.arraycopy(sigCompact, 32, s, 0, 32);
        
        BigInteger[] sigBig= new BigInteger[2] ;
        sigBig[0]= new BigInteger(1, r);
        sigBig[1]= new BigInteger(1, s);
        
        ECPoint point=null;
        for (int recid=0; recid<4; recid++){
            point= Recover(hash, sigBig, recid, false);
            if (point==null){
                logger.warning("SATOCHIPLIB: null point for recid: " + recid);
                continue;
            }

            // convert to byte[]
            byte[] pubkey= point.getEncoded(false); // uncompressed
            
            // add to list
            pubkeys.add(pubkey);
            logger.warning("SATOCHIPLIB: Found potential pubkey: " + toHexString(pubkey));
        }
        return pubkeys;
    } 
  
    public byte[] parseToCompactSignature(byte[] sigIn){
    
        // sig is DER format, starting with 30 45
        int sigInSize= sigIn.length;
        
        int offset=0;
        if (sigIn[offset++] != 0x30){
            throw new RuntimeException("Wrong signature byte (should be 0x30) !");
        }
        int lt= sigIn[offset++];
        int check= sigIn[offset++];
        if (check != 0x02){
            throw new RuntimeException("Wrong signature check byte (should be 0x02) !");
        }
        
        int lr= sigIn[offset++]; // should be 0x20 or 0x21 if first r msb is 1
        byte[] r= new byte[32];
        if (lr== 0x20){
            System.arraycopy(sigIn, offset, r, 0, 32);
            offset+=32;
        }else if (lr== 0x21){
            offset++; // skip zero byte
            System.arraycopy(sigIn, offset, r, 0, 32);
            offset+=32;
        }
        else{
            throw new RuntimeException("Wrong signature r length (should be 0x20 or 0x21) !");
        }
        
        check= sigIn[offset++];
        if (check != 0x02){
            throw new RuntimeException("Wrong signature check byte (should be 0x02) !");
        }
        
        int ls= sigIn[offset++]; // should be 0x20 or 0x21 if first s msb is 1
        byte[] s= new byte[32];
        if (ls== 0x20){
            System.arraycopy(sigIn, offset, s, 0, 32);
            offset+=32;
        } else if (ls== 0x21){
            offset++; // skip zero byte
            System.arraycopy(sigIn, offset, s, 0, 32);
            offset+=32;
        } else{
            throw new RuntimeException("Wrong signature s length (should be 0x20 or 0x21) !");
        }
        
        int sigOutSize= 64;
        byte[] sigOut= new byte[sigOutSize];
        System.arraycopy(r, 0, sigOut, 0, r.length);
        System.arraycopy(s, 0, sigOut, 32, s.length);
        
        return sigOut;
    }
  
    public ECPoint Recover(byte[] hash, BigInteger[] sig, int recId, boolean check){
    
        BigInteger r= sig[0];
        BigInteger s= sig[1];
        
        BigInteger n = CURVE.getN();  // Curve order.
        BigInteger i = BigInteger.valueOf((long) recId / 2);
        BigInteger x = r.add(i.multiply(n));
        BigInteger prime = SecP256K1Curve.q;
        
        if (x.compareTo(prime) >= 0) {
            // Cannot have point co-ordinates larger than this as everything takes place modulo Q.
            return null;
        }
        ECPoint R = decompressKey(x, (recId & 1) == 1);
        if (!R.multiply(n).isInfinity())
            return null;
              
        BigInteger e = new BigInteger(1, hash); //message.toBigInteger();
        
        BigInteger eInv = BigInteger.ZERO.subtract(e).mod(n);
        BigInteger rInv = r.modInverse(n);
        BigInteger srInv = rInv.multiply(s).mod(n);
        BigInteger eInvrInv = rInv.multiply(eInv).mod(n);
        ECPoint q = ECAlgorithms.sumOfTwoMultiplies(CURVE.getG(), eInvrInv, R, srInv);
        
        return q;
        //return ECKey.fromPublicOnly(q, compressed);
    }
  
    /** Decompress a compressed public key (x co-ord and low-bit of y-coord). */
    private ECPoint decompressKey(BigInteger xBN, boolean yBit) {
        X9IntegerConverter x9 = new X9IntegerConverter();
        byte[] compEnc = x9.integerToBytes(xBN, 1 + x9.getByteLength(CURVE.getCurve()));
        compEnc[0] = (byte)(yBit ? 0x03 : 0x02);
        return CURVE.getCurve().decodePoint(compEnc);
    }
  
  // public static String toHexString(byte[] raw) {
		
		// if ( raw == null )
      // return "";
    
    // final StringBuilder hex = new StringBuilder( 2 * raw.length );
    // for ( final byte b : raw ) {
       // hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
       //hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F))).append(" ");
    // }
    // return hex.toString();
  // }
  
  
    //based on https://github.com/bitcoinj/bitcoinj/blob/master/core/src/main/java/org/bitcoinj/core/ECKey.java
    public BigInteger[] decodeFromDER(byte[] bytes) {
        ASN1InputStream decoder = null;
        try {
            // BouncyCastle by default is strict about parsing ASN.1 integers. We relax this check, because some
            // Bitcoin signatures would not parse.
            Properties.setThreadOverride("org.bouncycastle.asn1.allow_unsafe_integer", true);
            decoder = new ASN1InputStream(bytes);
            final ASN1Primitive seqObj = decoder.readObject();
            if (seqObj == null)
                throw new RuntimeException("Reached past end of ASN.1 stream.");
            if (!(seqObj instanceof DLSequence))
                throw new RuntimeException("Read unexpected class: " + seqObj.getClass().getName());
            final DLSequence seq = (DLSequence) seqObj;
            ASN1Integer r, s;
            try {
                r = (ASN1Integer) seq.getObjectAt(0);
                s = (ASN1Integer) seq.getObjectAt(1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            // enforce low-S signature (BIP 62)
            BigInteger s2= s.getPositiveValue();
            if (s2.compareTo(HALF_CURVE_ORDER) > 0){
                s2= CURVE_ORDER.subtract(s2);
            }

            BigInteger[] sigBig= new BigInteger[2];
            sigBig[0]= r.getPositiveValue();
            sigBig[1]= s2; //s.getPositiveValue();
            return sigBig;

            // OpenSSL deviates from the DER spec by interpreting these values as unsigned, though they should not be
            // Thus, we always use the positive versions. See: http://r6.ca/blog/20111119T211504Z.html
            //return new ECDSASignature(r.getPositiveValue(), s.getPositiveValue());
        } catch (Exception e) {
            //throw new SignatureDecodeException(e);
            throw new RuntimeException("Exception in decodeFromDER() ", e);
        } finally {
            if (decoder != null)
                try { decoder.close(); } catch (IOException x) {}
            Properties.removeThreadOverride("org.bouncycastle.asn1.allow_unsafe_integer");
        }
    }
  
    public boolean verifySig(byte[] msg, byte[] dersig, byte[] pub) {
        logger.info("SATOCHIPLIB: In verifySig() ");
        logger.info("SATOCHIPLIB: verifySig: authentikey: " + toHexString(pub));
    
        // compute hash of message
        SHA256Digest digest = new SHA256Digest();
        byte[] hash= new byte[digest.getDigestSize()];
        digest.update(msg, 0, msg.length);
        digest.doFinal(hash, 0);
        logger.info("SATOCHIPLIB: verifySig: hash: " + toHexString(hash));
      
      
        // convert der-sig to bigInteger[]
        BigInteger[] rs= decodeFromDER(dersig);
        
        ECDSASigner signer = new ECDSASigner();
        ECPublicKeyParameters params = new ECPublicKeyParameters(CURVE.getCurve().decodePoint(pub), CURVE);
        signer.init(false, params);
        try {
            logger.info("SATOCHIPLIB: verifySig: hash: verifySignature: Start" );
            return signer.verifySignature(hash, rs[0], rs[1]);
        } catch (NullPointerException e) {
            logger.warning("SATOCHIPLIB: Caught NPE inside bouncy castle"+ e);
            return false;
        }
    }
  
    /**
    * PKI PARSER
    **/
    public String convertBytesToStringPem(byte[] certBytes){
        logger.info("SATOCHIPLIB: In convertBytesToStringPem");
        String certBase64Raw= Base64.getEncoder().encodeToString(certBytes);
        logger.info("SATOCHIPLIB: certBase64Raw"+ certBase64Raw);
        
        // divide in fixed size chunk
        int chunkSize=64;
        String certBase64= "-----BEGIN CERTIFICATE-----\r\n"; 
        for (int offset=0; offset<certBase64Raw.length(); offset+=chunkSize){
            certBase64+= certBase64Raw.substring(offset, Math.min(certBase64Raw.length(), offset + chunkSize));
            certBase64+= "\r\n";
        }
        certBase64+= "-----END CERTIFICATE-----";
        //certBase64= "-----BEGIN CERTIFICATE-----\r\n" + certBase64 + "\r\n-----END CERTIFICATE-----";
        logger.info("SATOCHIPLIB: certBase64"+ certBase64);
        return certBase64;
    }
    
    public byte[][] parseVerifyChallengeResponsePerso(ApduResponse rapdu){
        
        try{
            byte[] data= rapdu.getData();
            logger.info("SATOCHIPLIB: parseVerifyChallengeResponsePki data: " + toHexString(data));
            
            int offset=0;
            int dataRemain= data.length;
        
            // data= [challenge_from_device(32b) | sigSize | sig ]
            byte[][] out= new byte[2][];
            out[0]= new byte[32];
            System.arraycopy(data, offset, out[0], 0, 32);
            offset+=32;
            
            int sigSize= 256*data[offset++] + data[offset++];
            out[1]= new byte[sigSize];
            System.arraycopy(data, offset, out[1], 0, sigSize);

            return out;
        } catch(Exception e) {
            throw new RuntimeException("Parsing error in parseVerifyChallengeResponsePki: ", e);
        }   
        
    }
    
/*   ===
          cert_bytes_b64 = base64.b64encode(bytes(cert_bytes))
        cert_b64= cert_bytes_b64.decode('ascii')
        cert_pem= "-----BEGIN CERTIFICATE-----\r\n" 
        cert_pem+= '\r\n'.join([cert_b64[i:i+64] for i in range(0, len(cert_b64), 64)]) 
        cert_pem+= "\r\n-----END CERTIFICATE-----"
        return cert_pem
   */
  
  
    /**
    * Serializes the APDU to human readable hex string format
    *
    * @return the hex string representation of the APDU
    */
    public static String toHexString(byte[] raw) {
        try{
            if ( raw == null ) {
                return "";
            }
            final StringBuilder hex = new StringBuilder( 2 * raw.length );
            for ( final byte b : raw ) {
                hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
            }
            return hex.toString();
        } catch(Exception e){
            return "Exception in Util.toHexString()";
        }
    }
  
    public static byte[] fromHexString(String hex){
        
        if ((hex.length() % 2) != 0)
            throw new IllegalArgumentException("Input string must contain an even number of characters");

        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
  
}