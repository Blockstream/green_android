/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Giannis Dzegoutanis
 * Copyright 2015 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.bitcoinj.params.Networks;
import org.bitcoinj.script.Script;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>A Bitcoin address looks like 1MsScoe2fTJoq4ZPdQgqyhgWeoNamYPevy and is derived from an elliptic curve public key
 * plus a set of network parameters. Not to be confused with a {@link PeerAddress} or {@link AddressMessage}
 * which are about network (TCP) addresses.</p>
 *
 * <p>A standard address is built by taking the RIPE-MD160 hash of the public key bytes, with a version prefix and a
 * checksum suffix, then encoding it textually as base58. The version prefix is used to both denote the network for
 * which the address is valid (see {@link NetworkParameters}, and also to indicate how the bytes inside the address
 * should be interpreted. Whilst almost all addresses today are hashes of public keys, another (currently unsupported
 * type) can contain a hash of a script instead.</p>
 */
public class Address extends VersionedChecksummedBytes {
    /**
     * <p>A regular address is a RIPEMD160 hash of a public key, therefore is always 160 bits or 20 bytes.</p>
     * <p>This does not apply to SegWit addresses. Parameter remains for compatibility reasons.</p>
     */
    @Deprecated
    public static final int LENGTH = 20;

    private transient NetworkParameters params;

    /**
     * Construct an address from parameters, the address version, and the hash160 form. Example:<p>
     *
     * <pre>new Address(MainNetParams.get(), NetworkParameters.getAddressHeader(), Hex.decode("4a22c3c4cbb31e4d03b15550636762bda0baf85a"));</pre>
     */
    public Address(NetworkParameters params, int version, byte[] hash)
        throws WrongNetworkException, WrongLengthException {
        super(version, getBytes(params, version, hash));
        checkNotNull(params);
        if (!isAcceptableVersion(params, version))
            throw new WrongNetworkException(version, params.getAcceptableAddressCodes());
        if (!isAcceptableLength(params, version, hash.length))
            throw new WrongLengthException(hash.length);
        this.params = params;
    }

    /** Returns an Address that represents the given P2SH script hash. */
    public static Address fromP2SHHash(NetworkParameters params, byte[] hash160) {
        try {
            return new Address(params, params.getP2SHHeader(), hash160);
        } catch (WrongNetworkException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    /** Returns an Address that represents the script hash extracted from the given scriptPubKey. */
    public static Address fromP2SHScript(NetworkParameters params, Script scriptPubKey) {
        checkArgument(scriptPubKey.isPayToScriptHash(), "Not a P2SH script");
        return fromP2SHHash(params, scriptPubKey.getPubKeyHash());
    }

    /** Returns an Address that represents the given P2WSH script hash. */
    public static Address fromP2WSHHash(NetworkParameters params, byte[] hash) {
        try {
            return new Address(params, params.getP2WSHHeader(), hash);
        } catch (WrongNetworkException e) {
            throw new RuntimeException(e);
        } catch (WrongLengthException e) {
            throw new RuntimeException(e);
        }
    }

    /** Returns an Address that represents the given P2WPKH public key hash. */
    public static Address fromP2WPKHHash(NetworkParameters params, byte[] hash) {
        try {
            return new Address(params, params.getP2WPKHHeader(), hash);
        } catch (WrongNetworkException e) {
            throw new RuntimeException(e);
        } catch (WrongLengthException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Construct an address from its Base58 representation.
     * @param params
     *            The expected NetworkParameters or null if you don't want validation.
     * @param base58
     *            The textual form of the address, such as "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL".
     * @throws AddressFormatException
     *             if the given base58 doesn't parse or the checksum is invalid
     * @throws WrongNetworkException
     *             if the given address is valid but for a different chain (eg testnet vs mainnet)
     */
    public static Address fromBase58(@Nullable NetworkParameters params, String base58) throws AddressFormatException {
        return new Address(params, base58);
    }

    /**
     * Construct an address from parameters and the hash160 form. Example:<p>
     *
     * <pre>new Address(MainNetParams.get(), Hex.decode("4a22c3c4cbb31e4d03b15550636762bda0baf85a"));</pre>
     */
    public Address(NetworkParameters params, byte[] hash160) {
        super(params.getAddressHeader(), hash160);
        checkArgument(hash160.length == 20, "Addresses are 160-bit hashes, so you must provide 20 bytes");
        this.params = params;
    }

    /** @deprecated Use {@link #fromBase58(NetworkParameters, String)} */
    @Deprecated
    public Address(@Nullable NetworkParameters params, String address) throws AddressFormatException {
        super(address);
        if (params != null) {
            if (!isAcceptableVersion(params, version)) {
                throw new WrongNetworkException(version, params.getAcceptableAddressCodes());
            }
            final byte[] hash = getHash();
            if (!isAcceptableLength(params, version, hash.length)) {
                throw new WrongLengthException(hash.length);
            }
            this.params = params;
        } else {
            NetworkParameters paramsFound = null;
            for (NetworkParameters p : Networks.get()) {
                if (isAcceptableVersion(p, version) && isAcceptableLength(p, version, getHash(p).length)) {
                    paramsFound = p;
                    break;
                }
            }
            if (paramsFound == null)
                throw new AddressFormatException("No network found for " + address);

            this.params = paramsFound;
        }
    }

    /** The (big endian) 20 byte hash that is the core of a Bitcoin address. */
    public byte[] getHash160() {
        final byte[] hash = getHash();
        checkArgument(hash.length == 20, "Expcted RIPMED160 hash does not have 20 bytes");
        return hash;
    }

    /** Get hash of Bitcoin address. */
    public byte[] getHash() {
        return getHash(params);
    }

    /** Get hash embedded in address. */
    private byte[] getHash(NetworkParameters params) {
        if (bytes.length == 20)
            return bytes;
        else {
            final byte[] hash = new byte[bytes.length - 2];
            System.arraycopy(bytes, 2, hash, 0, bytes.length - 2);
            return hash;
        }
    }

    /**
     * Returns true if this address is a Pay-To-Script-Hash (P2SH) address.
     * See also https://github.com/bitcoin/bips/blob/master/bip-0013.mediawiki: Address Format for pay-to-script-hash
     */
    public boolean isP2SHAddress() {
        final NetworkParameters parameters = getParameters();
        return parameters != null && this.version == parameters.p2shHeader;
    }

    /**
     * Returns true if this address is a Pay-To-Witness-Public-Key-Hash (P2WPKH) address.
     * See also https://github.com/bitcoin/bips/blob/master/bip-0142.mediawiki: Address Format for Segregated Witness
     */
    public boolean isP2WPKHAddress() {
        final NetworkParameters parameters = getParameters();
        return parameters != null && this.version == parameters.p2wpkhHeader;
    }

    /**
     * Returns true if this address is a Pay-To-Witness-Script-Hash (P2WSH) address.
     * See also https://github.com/bitcoin/bips/blob/master/bip-0142.mediawiki: Address Format for Segregated Witness
     */
    public boolean isP2WSHAddress() {
        final NetworkParameters parameters = getParameters();
        return parameters != null && this.version == parameters.p2wshHeader;
    }

    /**
     * Examines the version byte of the address and attempts to find a matching NetworkParameters. If you aren't sure
     * which network the address is intended for (eg, it was provided by a user), you can use this to decide if it is
     * compatible with the current wallet. You should be able to handle a null response from this method. Note that the
     * parameters returned is not necessarily the same as the one the Address was created with.
     *
     * @return a NetworkParameters representing the network the address is intended for.
     */
    public NetworkParameters getParameters() {
        return params;
    }

    /**
     * Given an address, examines the version byte and attempts to find a matching NetworkParameters. If you aren't sure
     * which network the address is intended for (eg, it was provided by a user), you can use this to decide if it is
     * compatible with the current wallet.
     * @return a NetworkParameters of the address
     * @throws AddressFormatException if the string wasn't of a known version
     */
    public static NetworkParameters getParametersFromAddress(String address) throws AddressFormatException {
        try {
            return Address.fromBase58(null, address).getParameters();
        } catch (WrongNetworkException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    /**
     * Check if a given address version is valid given the NetworkParameters.
     */
    private static boolean isAcceptableVersion(NetworkParameters params, int version) {
        for (int v : params.getAcceptableAddressCodes()) {
            if (version == v) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check the length of an address.
     */
    private static boolean isAcceptableLength(NetworkParameters params, int version, int length) {
        switch (length) {
            case 20: return (version != params.getP2WSHHeader());
            case 32: return (version == params.getP2WSHHeader());
            default: return false;
        }
    }

    /**
     * This implementation narrows the return type to <code>Address</code>.
     */
    @Override
    public Address clone() throws CloneNotSupportedException {
        return (Address) super.clone();
    }

    // Java serialization

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeUTF(params.id);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        params = NetworkParameters.fromID(in.readUTF());
    }

    private static byte[] getBytes(final NetworkParameters params, final int version, final byte[] hash) {
        if (version == params.getAddressHeader())
            return hash;
        if (version == params.getP2SHHeader())
            return hash;
        if (version == params.getP2WPKHHeader() || version == params.getP2WSHHeader()) {
            final byte[] bytes = new byte[hash.length + 2];
            System.arraycopy(hash, 0, bytes, 2, hash.length);
            return bytes;
        }
        throw new IllegalArgumentException("Could not figure out how to serialize address");
    }
}
