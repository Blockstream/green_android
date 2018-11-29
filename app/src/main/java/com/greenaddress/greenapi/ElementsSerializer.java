package com.greenaddress.greenapi;

import org.bitcoinj.core.BitcoinSerializer;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;

public class ElementsSerializer extends BitcoinSerializer {

    public ElementsSerializer(final NetworkParameters params, final boolean parseRetain) {
        super(params, parseRetain);
    }

    public Transaction makeTransaction(final byte[] payloadBytes, final int offset,
                                       final int length, final byte[] hash) throws ProtocolException {
        final Transaction tx = new ElementsTransaction(getParameters(), payloadBytes, offset, null, this, length);
        if (hash != null)
            tx.setHash(Sha256Hash.wrapReversed(hash));
        return tx;
    }
}
