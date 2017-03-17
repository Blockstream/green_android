package com.greenaddress.greenapi;

import android.util.Pair;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.VersionedChecksummedBytes;
import org.bitcoinj.core.WrongLengthException;
import org.bitcoinj.core.WrongNetworkException;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;

public class ConfidentialAddress extends VersionedChecksummedBytes {

    public ConfidentialAddress(final NetworkParameters params, final int version, final byte[] hash, final byte[] blindingPubKey)
            throws WrongNetworkException, WrongLengthException {
        super(4, getBytes(params, version, hash, blindingPubKey));
        checkNotNull(params);
        if (!isAcceptableVersion(params, version))
            throw new WrongNetworkException(version, params.getAcceptableAddressCodes());
        if (!isAcceptableLength(params, version, hash.length))
            throw new WrongLengthException(hash.length);
    }

    protected ConfidentialAddress(final NetworkParameters params, final String address) throws AddressFormatException {
        super(address);

        checkNotNull(params);

        if (version != 4)
            throw new WrongNetworkException(version, params.getAcceptableAddressCodes());

        final byte[] hash = bytes;
        if (!isAcceptableLength(params, version, hash.length - 1 - 33)) // len - version - pubkey
            throw new WrongLengthException(hash.length);
    }

    public static ConfidentialAddress fromBase58(final NetworkParameters params, final String base58) throws AddressFormatException {
        return new ConfidentialAddress(params, base58);
    }

    private static boolean isAcceptableVersion(final NetworkParameters params, final int version) {
        for (int v : params.getAcceptableAddressCodes())
            if (version == v)
                return true;
        return false;
    }

    private static boolean isAcceptableLength(final NetworkParameters params, final int version, final int length) {
        switch (length) {
            case 20: return (version != params.getP2WSHHeader());
            default: return false;
        }
    }

    public static ConfidentialAddress fromP2SHHash(final NetworkParameters params, final byte[] hash160, final byte[] blindingPubKey) {
        try {
            return new ConfidentialAddress(params, params.getP2SHHeader(), hash160, blindingPubKey);
        } catch (WrongNetworkException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    private static byte[] getBytes(final NetworkParameters params, final int version, final byte[] hash, final byte[] blindingPubKey) {
        if (version == params.getAddressHeader() || version == params.getP2SHHeader()) {
            byte[] bytes = new byte[1 + 33 + 20]; // p2sh/p2pkh version + pubkey + hash
            bytes[0] = (byte) version;
            System.arraycopy(blindingPubKey, 0, bytes, 1, 33);
            System.arraycopy(hash, 0, bytes, 34, 20);
            return bytes;
        }
        throw new IllegalArgumentException("Could not figure out how to serialize address");
    }

    public Address getBitcoinAddress() {
        if (bytes[0] == Network.NETWORK.getP2SHHeader())
            return Address.fromP2SHHash(Network.NETWORK, Arrays.copyOfRange(bytes, 34, 54));
        if (bytes[0] == Network.NETWORK.getP2WPKHHeader())
            return Address.fromP2WPKHHash(Network.NETWORK, Arrays.copyOfRange(bytes, 34, 54));
        throw new RuntimeException();  // Cannot happen.
    }

    public byte[] getBlindingPubKey() {
        return Arrays.copyOfRange(bytes, 1, 34);
    }
}
