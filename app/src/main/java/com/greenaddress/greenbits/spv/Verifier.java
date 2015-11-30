package com.greenaddress.greenbits.spv;

import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.PreparedTransaction;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;

import java.util.List;
import java.util.Map;

class Verifier {
    static Coin verify(final Map<TransactionOutPoint, Coin> countedUtxoValues, final PreparedTransaction transaction, final Address recipient, final Coin amount, final List<Boolean> input) {
        int changeIdx;
        if (input == null) {
            changeIdx = -1;
        } else if (input.get(0)) {
            changeIdx = 0;
        } else if (input.get(1)) {
            changeIdx = 1;
        } else {
            throw new IllegalArgumentException("Verification: Change output missing.");
        }
        if (input != null && input.get(0) && input.get(1)) {
            // Shouldn't happen really. In theory user can send money to a new change address
            // of themselves which they've generated manually, but it's unlikely, so for
            // simplicity we don't handle it.
            throw new IllegalArgumentException("Verification: Cannot send to a change address.");
        }
        final TransactionOutput output = transaction.decoded.getOutputs().get(1 - Math.abs(changeIdx));
        if (recipient != null) {
            final Address gotAddress = output.getScriptPubKey().getToAddress(Network.NETWORK);
            if (!gotAddress.equals(recipient)) {
                throw new IllegalArgumentException("Verification: Invalid recipient address.");
            }
        }
        if (amount != null && !output.getValue().equals(amount)) {
            throw new IllegalArgumentException("Verification: Invalid output amount.");
        }

        // 3. Verify fee value:
        Coin inValue = Coin.ZERO, outValue = Coin.ZERO;
        for (final TransactionInput in : transaction.decoded.getInputs()) {
            if (countedUtxoValues.get(in.getOutpoint()) == null) {
                final Transaction prevTx = transaction.prevoutRawTxs.get(in.getOutpoint().getHash().toString());
                if (!prevTx.getHash().equals(in.getOutpoint().getHash())) {
                    throw new IllegalArgumentException("Verification: Prev tx hash invalid");
                }
                inValue = inValue.add(prevTx.getOutput((int) in.getOutpoint().getIndex()).getValue());
            } else {
                inValue = inValue.add(countedUtxoValues.get(in.getOutpoint()));
            }
        }
        for (final TransactionOutput out : transaction.decoded.getOutputs()) {
            outValue = outValue.add(out.getValue());
        }
        final Coin fee = inValue.subtract(outValue);
        if (fee.compareTo(Coin.valueOf(1000)) == -1) {
            throw new IllegalArgumentException("Verification: Fee is too small (expected at least 1000 satoshi).");
        }
        final int kBfee = (int) (500000.0 * ((double) transaction.decoded.getMessageSize()) / 1000.0);
        if (fee.compareTo(Coin.valueOf(kBfee)) == 1) {
            throw new IllegalArgumentException("Verification: Fee is too large (expected at most 500000 satoshi per kB).");
        }
        if (amount == null) {
            return output.getValue();
        } else {
            return fee;
        }
    }
}
