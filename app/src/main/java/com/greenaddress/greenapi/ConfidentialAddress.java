package com.greenaddress.greenapi;

import android.util.Pair;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.VersionedChecksummedBytes;
import org.bitcoinj.core.WrongLengthException;
import org.bitcoinj.core.WrongNetworkException;
import org.bitcoinj.params.AbstractBitcoinNetParams;
import org.bitcoinj.uri.BitcoinURIParseException;
import org.bitcoinj.uri.OptionalFieldValidationException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

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

    public static Pair<String, Coin> parseBitcoinURI(NetworkParameters params, String input) throws BitcoinURIParseException {
        checkNotNull(input);

        String scheme = params == null ? AbstractBitcoinNetParams.BITCOIN_SCHEME : params.getUriScheme();

        // Attempt to form the URI (fail fast syntax checking to official standards).
        URI uri;
        try {
            uri = new URI(input);
        } catch (URISyntaxException e) {
            throw new BitcoinURIParseException("Bad URI syntax", e);
        }

        // URI is formed as  bitcoin:<address>?<query parameters>
        // blockchain.info generates URIs of non-BIP compliant form bitcoin://address?....
        // We support both until Ben fixes his code.

        // Remove the bitcoin scheme.
        // (Note: getSchemeSpecificPart() is not used as it unescapes the label and parse then fails.
        // For instance with : bitcoin:129mVqKUmJ9uwPxKJBnNdABbuaaNfho4Ha?amount=0.06&label=Tom%20%26%20Jerry
        // the & (%26) in Tom and Jerry gets interpreted as a separator and the label then gets parsed
        // as 'Tom ' instead of 'Tom & Jerry')
        String blockchainInfoScheme = scheme + "://";
        String correctScheme = scheme + ":";
        String schemeSpecificPart;
        if (input.startsWith(blockchainInfoScheme))
            schemeSpecificPart = input.substring(blockchainInfoScheme.length());
        else if (input.startsWith(correctScheme))
            schemeSpecificPart = input.substring(correctScheme.length());
        else
            throw new BitcoinURIParseException("Unsupported URI scheme: " + uri.getScheme());

        // Split off the address from the rest of the query parameters.
        String[] addressSplitTokens = schemeSpecificPart.split("\\?", 2);
        if (addressSplitTokens.length == 0)
            throw new BitcoinURIParseException("No data found after the bitcoin: prefix");
        String addressToken = addressSplitTokens[0];  // may be empty!

        String[] nameValuePairTokens;
        if (addressSplitTokens.length == 1) {
            // Only an address is specified - use an empty '<name>=<value>' token array.
            nameValuePairTokens = new String[] {};
        } else {
            // Split into '<name>=<value>' tokens.
            nameValuePairTokens = addressSplitTokens[1].split("&");
        }

        // Attempt to parse the rest of the URI parameters.
        final Map<String, Object> parameterMap = new LinkedHashMap<>();

        Coin value = null;

        for (String nameValuePairToken : nameValuePairTokens) {
            final int sepIndex = nameValuePairToken.indexOf('=');
            if (sepIndex == -1)
                throw new BitcoinURIParseException("Malformed Bitcoin URI - no separator in '" +
                        nameValuePairToken + "'");
            if (sepIndex == 0)
                throw new BitcoinURIParseException("Malformed Bitcoin URI - empty name '" +
                        nameValuePairToken + "'");
            final String nameToken = nameValuePairToken.substring(0, sepIndex).toLowerCase(Locale.US);
            final String valueToken = nameValuePairToken.substring(sepIndex + 1);


            // Parse the amount.
            if ("amount".equals(nameToken)) {
                // Decode the amount (contains an optional decimal component to 8dp).
                try {
                    value = Coin.parseCoin(valueToken);
                    if (params != null && value.isGreaterThan(params.getMaxMoney()))
                        throw new BitcoinURIParseException("Max number of coins exceeded");
                    if (value.signum() < 0)
                        throw new ArithmeticException("Negative coins specified");
                    if (parameterMap.containsKey(nameToken))
                        throw new BitcoinURIParseException(String.format(Locale.US, "'%s' is duplicated, URI is invalid", nameToken));
                    else
                        parameterMap.put(nameToken, value);
                } catch (IllegalArgumentException e) {
                    throw new OptionalFieldValidationException(String.format(Locale.US, "'%s' is not a valid amount", valueToken), e);
                } catch (ArithmeticException e) {
                    throw new OptionalFieldValidationException(String.format(Locale.US, "'%s' has too many decimal places", valueToken), e);
                }
            }
        }

        if (addressToken.isEmpty())
            throw new BitcoinURIParseException("No address found");

        // Validate
        ConfidentialAddress.fromBase58(params, addressToken);

        // Return string if correct
        return new Pair<>(addressToken, value);
    }

    public byte[] getBlindingPubKey() {
        return Arrays.copyOfRange(bytes, 1, 34);
    }
}
