package com.greenaddress.greenapi;

import com.blockstream.libwally.Wally;

import java.security.KeyStore;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.netty.handler.ssl.util.SimpleTrustManagerFactory;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.InternalThreadLocalMap;

public class FingerprintTrustManagerFactorySHA256 extends SimpleTrustManagerFactory {
    private static final Pattern FINGERPRINT_PATTERN = Pattern.compile("^[0-9a-fA-F:]+$");
    private static final Pattern FINGERPRINT_STRIP_PATTERN = Pattern.compile(":");
    private static final int SHA256_HEX_LEN = Wally.SHA256_LEN * 2;

    private final TrustManager tm = new X509TrustManager() {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String s) throws CertificateException {
            checkTrusted("client", chain);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String s) throws CertificateException {
            checkTrusted("server", chain);
        }

        private void checkTrusted(String type, X509Certificate[] chain) throws CertificateException {
            X509Certificate cert = chain[0];
            byte[] fingerprint = fingerprint(cert);
            boolean found = false;
            for (byte[] allowedFingerprint: fingerprints) {
                if (Arrays.equals(fingerprint, allowedFingerprint)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new CertificateException(
                        type + " certificate with unknown fingerprint: " + cert.getSubjectDN());
            }
        }

        private byte[] fingerprint(X509Certificate cert) throws CertificateEncodingException {
            return Wally.sha256(cert.getEncoded());
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return EmptyArrays.EMPTY_X509_CERTIFICATES;
        }
    };

    private final byte[][] fingerprints;


    public FingerprintTrustManagerFactorySHA256(final String...  fp) {
        final byte[][] fingerprints = toFingerprintArray(Arrays.asList(fp));

        List<byte[]> list = InternalThreadLocalMap.get().arrayList();
        for (byte[] f: fingerprints) {
            if (f == null) {
                break;
            }
            if (f.length != Wally.SHA256_LEN) {
                throw new IllegalArgumentException("malformed fingerprint: " +
                        Wally.hex_from_bytes(f) + " (expected: SHA256)");
            }
            list.add(f.clone());
        }

        this.fingerprints = list.toArray(new byte[list.size()][]);
    }

    private static byte[][] toFingerprintArray(Iterable<String> fingerprints) {
        if (fingerprints == null) {
            throw new NullPointerException("fingerprints");
        }

        List<byte[]> list = InternalThreadLocalMap.get().arrayList();
        for (String f: fingerprints) {
            if (f == null) {
                break;
            }

            if (!FINGERPRINT_PATTERN.matcher(f).matches()) {
                throw new IllegalArgumentException("malformed fingerprint: " + f);
            }
            f = FINGERPRINT_STRIP_PATTERN.matcher(f).replaceAll("");
            if (f.length() != SHA256_HEX_LEN) {
                throw new IllegalArgumentException("malformed fingerprint: " + f + " (expected: SHA256)");
            }

            byte[] farr = new byte[Wally.SHA256_LEN];
            for (int i = 0; i < farr.length; i++) {
                int strIdx = i << 1;
                farr[i] = (byte) Integer.parseInt(f.substring(strIdx, strIdx + 2), 16);
            }
            list.add(farr);
        }

        return list.toArray(new byte[list.size()][]);
    }

    @Override
    protected void engineInit(KeyStore keyStore) throws Exception { }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws Exception { }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
        return new TrustManager[] { tm };
    }
}
