package com.greenaddress.greenapi;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Message;
import org.bitcoinj.core.MessageSerializer;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOptions;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.TransactionWitness;
import org.bitcoinj.core.VarInt;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.bitcoinj.core.Utils.uint32ToByteStreamLE;


public class ElementsTransaction extends Transaction {

    List<List<byte[]>> outWitness;

    public ElementsTransaction(final NetworkParameters params) {
        super(params);
        outWitness = new ArrayList<>();
    }

    public ElementsTransaction(final NetworkParameters params, final byte[] payload, final int offset, final Message parent, final MessageSerializer setSerializer, final int length) throws ProtocolException {
        super(params, payload, offset, parent, setSerializer, length);
        outWitness = new ArrayList<>();
    }

    public void addOutWitness(final byte[] surjectionProof, final byte[] rangeProof, final byte[] nonceCommitment) {
        outWitness.add(new ArrayList<byte[]>());
        outWitness.get(outWitness.size() - 1).add(surjectionProof);
        outWitness.get(outWitness.size() - 1).add(rangeProof);
        outWitness.get(outWitness.size() - 1).add(nonceCommitment);
    }

    @Override
    protected void readOutputs() {
        final long numOutputs = readVarInt();
        optimalEncodingMessageSize += VarInt.sizeOf(numOutputs);
        outputs = new ArrayList<>((int) numOutputs);
        for (long i = 0; i < numOutputs; i++) {
            final TransactionOutput output = new ElementsTransactionOutput(params, this, payload, cursor, serializer);
            outputs.add(output);
            final int len = (output.getValue().getValue() != 0) ? 42 : 66;  // 33+9 or 33+33
            final long scriptLen = readVarInt(len);
            optimalEncodingMessageSize += len + VarInt.sizeOf(scriptLen) + scriptLen;
            cursor += scriptLen;
        }
    }

    public ElementsTransactionOutput addOutput(final byte[] assetId, final Coin amount, final ConfidentialAddress confidentialAddress) {
        final ElementsTransactionOutput eto = new ElementsTransactionOutput(params, this, assetId, amount, confidentialAddress);
        addOutput(eto);
        return eto;
    }

    @Override
    protected void bitcoinSerializeToStream(final OutputStream stream, final int transactionOptions) throws IOException {
        final boolean witSupported = (protocolVersion >= NetworkParameters.ProtocolVersion.WITNESS_VERSION.getBitcoinProtocolVersion())
                && (transactionOptions & TransactionOptions.WITNESS) != 0;
        final boolean serializeWit = hasWitness() && witSupported;
        uint32ToByteStreamLE(getVersion(), stream);
        if (serializeWit) {
            stream.write(new byte[]{0, (byte) (!outWitness.isEmpty() ? 3 : 1)});
        } else if (!outWitness.isEmpty() && (transactionOptions & TransactionOptions.WITNESS) != 0) {
            stream.write(new byte[]{0, 2});
        }
        stream.write(new VarInt(getInputs().size()).encode());
        for (final TransactionInput in : getInputs())
            in.bitcoinSerialize(stream);
        stream.write(new VarInt(outputs.size()).encode());
        for (final TransactionOutput out : outputs)
            out.bitcoinSerialize(stream);

        if (serializeWit) {
            for (int i = 0; i < getInputs().size(); i++) {
                final TransactionWitness witness = getWitness(i);
                stream.write(new VarInt(witness.getPushCount()).encode());
                for (int y = 0; y < witness.getPushCount(); y++) {
                    final byte[] push = witness.getPush(y);
                    stream.write(new VarInt(push.length).encode());
                    stream.write(push);
                }
            }
        }

        if ((transactionOptions & TransactionOptions.WITNESS) != 0) {
            for (final List<byte[]> outwit : outWitness) {
                for (final byte[] ow : outwit) {
                    stream.write(new VarInt(ow.length).encode());
                    stream.write(ow);
                }
            }
        }

        uint32ToByteStreamLE(getLockTime(), stream);
    }


    protected void parse() throws ProtocolException {
        final boolean witSupported = (protocolVersion >= NetworkParameters.ProtocolVersion.WITNESS_VERSION.getBitcoinProtocolVersion())
                && (transactionOptions & TransactionOptions.WITNESS) != 0;
        cursor = offset;

        version = readUint32();
        optimalEncodingMessageSize = 4;

        // First come the inputs.
        readInputs();
        byte flags = 0;
        if (witSupported && getInputs().isEmpty()) {
            flags = readBytes(1)[0];
            optimalEncodingMessageSize += 1;
            if (flags != 0) {
                readInputs();
                readOutputs();
            } else {
                outputs = new ArrayList<>(0);
            }
        } else {
            readOutputs();
        }
        if (((flags & 1) != 0) && witSupported) {
            flags ^= 1;
            readWitness();
        }
        if (((flags & 2) != 0) && witSupported) {
            flags ^= 2;
            readOutWitness();
        }
        if (flags != 0) {
            throw new ProtocolException("Unknown transaction optional data");
        }

        lockTime = readUint32();
        optimalEncodingMessageSize += 4;
        length = cursor - offset;

        witnesses = witnesses == null ? new ArrayList<TransactionWitness>() : witnesses;
    }


    protected void readOutWitness() {
        outWitness = new ArrayList<>(getOutputs().size());
        for (int i = 0; i < getOutputs().size(); i++) {
            final long pushCount = 3;
            outWitness.add(new ArrayList<byte[]>());
            for (int y = 0; y < pushCount; y++) {
                final long pushSize = readVarInt();
                optimalEncodingMessageSize += VarInt.sizeOf(pushSize) + pushSize;
                outWitness.get(outWitness.size() - 1).add(readBytes((int) pushSize));
            }
        }
    }

}
