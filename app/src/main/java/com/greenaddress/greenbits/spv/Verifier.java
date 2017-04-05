package com.greenaddress.greenbits.spv;

import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.PreparedTransaction;
import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.params.RegTestParams;

import java.util.List;
import java.util.Map;

class Verifier {
    private static Coin feeError(final String smallLarge, final Coin feeRate, final Coin limit) {
            final String msg = "Verification: Fee is too " + smallLarge + " (" +
                               feeRate.toFriendlyString() + " vs limit " +
                               limit.toFriendlyString() + ')';
            throw new IllegalArgumentException(msg);
    }

    static Coin verify(final GaService service,
                       final Map<TransactionOutPoint, Coin> countedUtxoValues, final PreparedTransaction ptx,
                       final Address recipient, final Coin amount, final List<Boolean> input) {
        int changeIdx;
        if (input == null)
            changeIdx = -1;
        else if (input.get(0))
            changeIdx = 0;
        else if (input.get(1))
            changeIdx = 1;
        else
            throw new IllegalArgumentException("Verification: Change output missing.");

        if (input != null && input.get(0) && input.get(1)) {
            // Shouldn't happen really. In theory user can send money to a new change address
            // of themselves which they've generated manually, but it's unlikely, so for
            // simplicity we don't handle it.
            throw new IllegalArgumentException("Verification: Cannot send to a change address.");
        }
        final TransactionOutput output = ptx.mDecoded.getOutputs().get(1 - Math.abs(changeIdx));
        if (recipient != null) {
            final Address gotAddress = output.getScriptPubKey().getToAddress(Network.NETWORK);
            if (!gotAddress.equals(recipient))
                throw new IllegalArgumentException("Verification: Invalid recipient address.");
        }
        if (amount != null && !output.getValue().equals(amount))
            throw new IllegalArgumentException("Verification: Invalid output amount.");

        // 3. Verify fee value
        Coin fee = Coin.ZERO;
        for (final TransactionInput in : ptx.mDecoded.getInputs()) {
            if (countedUtxoValues.get(in.getOutpoint()) != null) {
                fee = fee.add(countedUtxoValues.get(in.getOutpoint()));
                continue;
            }

            final Transaction prevTx = ptx.mPrevoutRawTxs.get(in.getOutpoint().getHash().toString());
            if (!prevTx.getHash().equals(in.getOutpoint().getHash()))
                throw new IllegalArgumentException("Verification: Prev tx hash invalid");
            fee = fee.add(prevTx.getOutput((int) in.getOutpoint().getIndex()).getValue());
        }
        for (final TransactionOutput out : ptx.mDecoded.getOutputs())
            fee = fee.subtract(out.getValue());

        final double messageSize = (double) ptx.mDecoded.getMessageSize();
        final double satoshiPerByte = (double) fee.value / messageSize;
        final double satoshiPerKiloByte = satoshiPerByte * 1000.0;
        final Coin feeRate = Coin.valueOf((int) satoshiPerKiloByte);

        final Coin minFeeRate = service.getMinFeeRate();
        if (feeRate.isLessThan(minFeeRate) && Network.NETWORK != RegTestParams.get())
            feeError("small", feeRate, minFeeRate);

        final Coin maxFeeRate = Coin.valueOf(15000 * 1000); // FIXME: Get max fee rate from server
        if (feeRate.isGreaterThan(maxFeeRate))
            feeError("large", feeRate, maxFeeRate);

        return amount == null ? output.getValue() : fee;
    }
}
