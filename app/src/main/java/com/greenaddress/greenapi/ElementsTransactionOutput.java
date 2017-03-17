package com.greenaddress.greenapi;

import com.blockstream.libwally.Wally;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.MessageSerializer;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VarInt;

import java.io.IOException;
import java.io.OutputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public class ElementsTransactionOutput extends TransactionOutput {

    private long unblindedValue;
    private byte[] assetId;
    private byte[] assetTag;
    private byte[] commitment;
    private byte[] abf;
    private byte[] vbf;
    private byte[] blindingPubKey;

    public void setAbfVbf(final byte[] newAbf, final byte[] newVbf, final byte[] assetId) {
        if (newAbf != null) {
            abf = newAbf;
            assetTag = Wally.asset_generator_from_bytes(assetId, abf);
        }
        vbf = newVbf;

        commitment = Wally.asset_value_commitment(unblindedValue, vbf, assetTag);
    }

    public long getUnblindedValue() {
        return unblindedValue;
    }

    public byte[] getAbf() {
        return abf;
    }

    public byte[] getVbf() {
        return vbf;
    }

    public byte[] getAssetId() {
        return assetId;
    }

    public byte[] getBlindingPubKey() {
        return blindingPubKey;
    }

    public byte[] getAssetTag() {
        return assetTag;
    }

    public void setUnblindedAssetTagFromAssetId(final byte[] assetId) {
        assetTag = new byte[33];
        assetTag[0] = 1;
        System.arraycopy(assetId, 0, assetTag, 1, 32);
        this.assetId = assetId;
    }


    public byte[] getCommitment() {
        return commitment;
    }


    public ElementsTransactionOutput(final NetworkParameters params, final Transaction parent, final byte[] payload, final int offset, final MessageSerializer serializer) throws ProtocolException {
        super(params, parent, payload, offset, serializer);
    }

    public ElementsTransactionOutput(final NetworkParameters params, final Transaction parent, final Coin value, final ConfidentialAddress to) {
        super(params, parent, value, to.getBitcoinAddress());
    }

    public ElementsTransactionOutput(final NetworkParameters params, final Transaction parent, final Coin value) {
        super(params, parent, value, new byte[0]);

        this.abf = new byte[32];
        this.vbf = new byte[32];
        length += 1 + 33; // commitment prefix + tag
    }

    public ElementsTransactionOutput(final NetworkParameters params, final Transaction parent, final byte[] assetId, final Coin value, final ConfidentialAddress to) {
        this(params, parent, Coin.ZERO, to);

        blindingPubKey = to.getBlindingPubKey();
        unblindedValue = value.getValue();
        this.assetId = assetId;
        setAbfVbf(CryptoHelper.randomBytes(32), CryptoHelper.randomBytes(32), assetId);
        length += 2*33 - 8; // remove value (8), add tag/commitment (2*33)
    }

    @Override
    protected void parse() throws ProtocolException {
        assetTag = readBytes(33);
        final byte first = readBytes(1)[0];
        if (first == 1) {
            value =  (payload[cursor + 7] & 0xffL) |
                    ((payload[cursor + 6] & 0xffL) << 8) |
                    ((payload[cursor + 5] & 0xffL) << 16) |
                    ((payload[cursor + 4] & 0xffL) << 24) |
                    ((payload[cursor + 3] & 0xffL) << 32) |
                    ((payload[cursor + 2] & 0xffL) << 40) |
                    ((payload[cursor + 1] & 0xffL) << 48) |
                    ((payload[cursor] & 0xffL) << 56);
            cursor += 8;
        } else {
            final byte[] commitmentSuffix = readBytes(32);
            commitment = new byte[33];
            commitment[0] = first;
            System.arraycopy(commitmentSuffix, 0, commitment, 1, 32);
        }

        scriptLen = (int) readVarInt();
        length = cursor - offset + scriptLen;
        scriptBytes = readBytes(scriptLen);
    }

    protected void bitcoinSerializeToStream(final OutputStream stream) throws IOException {
        checkNotNull(getScriptBytes());

        stream.write(assetTag);

        if (value != 0) {
            stream.write(new byte[] { 1 });
            final byte[] out = new byte[8];
            Utils.uint64ToByteArrayLE(value, out, 0);
            for (int i = 0; i < 4; ++i) {
                final byte tmp = out[i];
                out[i] = out[7-i];
                out[7-i] = tmp;
            }
            stream.write(out);
        } else {
            stream.write(commitment);
        }

        // TODO: Move script serialization into the Script class, where it belongs.
        stream.write(new VarInt(getScriptBytes().length).encode());
        stream.write(getScriptBytes());
    }

    public void setUnblindedValue(final long value) {
        unblindedValue = value;
        commitment = Wally.asset_value_commitment(unblindedValue, vbf, assetTag);
    }
}
