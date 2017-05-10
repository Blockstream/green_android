package com.greenaddress.greenapi;

import android.util.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Bytes;
import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOptions;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionWitness;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VarInt;
import org.bitcoinj.script.ScriptBuilder;
import com.blockstream.libwally.Wally;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GATx {

    private static final List<byte[]> EMPTY_SIGS = ImmutableList.of(new byte[71], new byte[71]);

    // Script types in end points
    public static final int P2SH_FORTIFIED_OUT = 10;
    public static final int P2SH_P2WSH_FORTIFIED_OUT = 14;
    public static final int REDEEM_P2SH_FORTIFIED = 150;
    public static final int REDEEM_P2SH_P2WSH_FORTIFIED = 159;
    public static final int MAX_BLOCK_NUM = 500000000 - 1;  // From nTimeLock field definition


    public static int getOutScriptType(final int scriptType) {
        switch (scriptType) {
            case REDEEM_P2SH_FORTIFIED:
                return P2SH_FORTIFIED_OUT;
            case REDEEM_P2SH_P2WSH_FORTIFIED:
                return P2SH_P2WSH_FORTIFIED_OUT;
            default:
                return scriptType;
        }
    }

    public static void sortUtxos(final List<JSONMap> utxos, final boolean minimizeInputs) {
        Collections.sort(utxos, new Comparator<JSONMap>() {
            @Override
            public int compare(final JSONMap lhs, final JSONMap rhs) {
                int cmp = 0;
                if (!minimizeInputs) {
                    // When not minimizing inputs, prefer earlier block times;
                    // By spending earlier utxos we can avoid re-deposits.
                    cmp = lhs.getInt("block_height", MAX_BLOCK_NUM).compareTo(rhs.getInt("block_height", MAX_BLOCK_NUM));
                }
                if (cmp == 0)
                    cmp = lhs.getBigInteger("value").compareTo(rhs.getBigInteger("value"));
                return cmp;
            }
        });
    }

    public static byte[] createOutScript(final GaService service, final JSONMap ep) {
        return service.createOutScript(ep.getInt("subaccount", 0),
                                       ep.getInt(ep.getKey("pubkey_pointer", "pointer")));
    }

    public static byte[] createInScript(final List<byte[]> sigs, final byte[] outScript,
                                  final int scriptType) {
        if (scriptType == P2SH_FORTIFIED_OUT || scriptType == REDEEM_P2SH_FORTIFIED)
            // FIXME: investigate P2SH_ vs REDEEM_P2SH_ and ideally make it consistent here
            return ScriptBuilder.createMultiSigInputScriptBytes(sigs, outScript).getProgram();
        // REDEEM_P2SH_P2WSH_FORTIFIED: PUSH(OP_0 PUSH(sha256(outScript)))
        return Bytes.concat(Wally.hex_to_bytes("220020"), Wally.sha256(outScript));
    }

    public static void addInput(final GaService service, final Transaction tx, final JSONMap ep) {
        final int scriptType = ep.getInt("script_type");
        final byte[] outscript = createOutScript(service, ep);
        final byte[] inscript = createInScript(EMPTY_SIGS, outscript, scriptType);
        final TransactionOutPoint op;
        op = new TransactionOutPoint(Network.NETWORK, ep.getInt("pt_idx"), ep.getHash("txhash"));
        final TransactionInput in = new TransactionInput(Network.NETWORK, null, inscript, op, ep.getCoin("value"));
        TransactionWitness witness = null;
        if (service.isSegwitEnabled() && scriptType == REDEEM_P2SH_P2WSH_FORTIFIED) {
            witness = new TransactionWitness(1);
            witness.setPush(0, EMPTY_SIGS.get(0));
        }
        in.setSequenceNumber(0); // This ensures nlocktime is recognized
        tx.addInput(in);
        if (witness != null)
            tx.setWitness(tx.getInputs().size() - 1, witness);
    }

    /* Add a new change output to a tx */
    public static Pair<TransactionOutput, Integer> addChangeOutput(final GaService service, final Transaction tx,
                                                                   final int subaccount) {
            final JSONMap addr = service.getNewAddress(subaccount);
            if (addr == null)
                return null;
            final byte[] script;
            if (addr.getString("addr_type").equals("p2wsh")) {
                script = ScriptBuilder.createP2WSHOutputScript(
                        Wally.sha256(addr.getBytes("script"))).getProgram();
            } else {
                script = addr.getBytes("script");
            }
            return new Pair<>(
                    tx.addOutput(Coin.ZERO, Address.fromP2SHHash(Network.NETWORK,
                                                                Utils.sha256hash160(script))),
                    addr.getInt("pointer")
            );
    }

    /* Create previous outputs for tx construction from uxtos */
    public static List<Output> createPrevouts(final GaService service, final List<JSONMap> utxos) {
        final List<Output> prevOuts = new ArrayList<>();
        for (final JSONMap utxo : utxos)
            prevOuts.add(new Output(utxo.getInt("subaccount"),
                                    utxo.getInt("pointer"),
                                    HDKey.BRANCH_REGULAR,
                                    getOutScriptType(utxo.getInt("script_type")),
                                    Wally.hex_from_bytes(createOutScript(service, utxo)),
                                    utxo.getLong("value")));
        return prevOuts;
    }

    /* Return the previous transactions for each of a txs inputs */
    public static List<Transaction> getPreviousTransactions(final GaService service, final Transaction tx) {
        final List<Transaction> previousTxs = new ArrayList<>();
        try {
            for (final TransactionInput in : tx.getInputs()) {
                final String txhex = service.getRawOutputHex(in.getOutpoint().getHash());
                previousTxs.add(GaService.buildTransaction(txhex));
            }
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
        return previousTxs;
    }

    // Estimate the size of Elements specific parts of a tx
    private static int estimateElementsSize(final Transaction tx) {
        if (!GaService.IS_ELEMENTS)
            return 0;

        final int sjSize = Wally.asset_surjectionproof_size(tx.getInputs().size());
        final int cmtSize = Wally.EC_PUBLIC_KEY_LEN;
        // Estimate the rangeproof len as 160 bytes per 2 bits used to express the
        // output value (currently 32), plus fixed overhead of 128 (this is a slight
        // over-estimate given that only up to +100 has been seen in the wild).
        // FIXME: This assumes 32 bit maximum amounts as per current wally impl.
        final int rpSize = ((32 / 2) * 160) + 128;
        final int singleOutputSize = sjSize + VarInt.sizeOf(sjSize) +
                                     cmtSize + VarInt.sizeOf(cmtSize) +
                                     rpSize + VarInt.sizeOf(rpSize);
        return singleOutputSize * tx.getOutputs().size();
    }

    // Calculate the fee that must be paid for a tx
    public static Coin getTxFee(final GaService service, final Transaction tx, final Coin feeRate) {
        final Coin minRate = service.getMinFeeRate();
        final Coin rate = feeRate.isLessThan(minRate) ? minRate : feeRate;
        final int vSize;

        if (!GaService.IS_ELEMENTS && !service.isSegwitEnabled())
            vSize = tx.unsafeBitcoinSerialize().length;
        else {
            /* For segwit, the fee is based on the weighted size of the tx */
            tx.transactionOptions = TransactionOptions.NONE;
            final int nonSwSize = tx.unsafeBitcoinSerialize().length;
            tx.transactionOptions = TransactionOptions.ALL;
            final int swSize = tx.unsafeBitcoinSerialize().length;
            final int fullSize = swSize + estimateElementsSize(tx);
            vSize = (int) Math.ceil((nonSwSize * 3 + fullSize) / 4.0);
        }
        final double fee = (double) vSize * rate.value / 1000.0;
        return Coin.valueOf((long) Math.ceil(fee));
    }

    // Swap the change and recipient output in a tx with 50% probability */
    public static boolean randomizeChange(final Transaction tx) {
        if (CryptoHelper.randomBytes(1)[0] < 0)
            return false;

        final TransactionOutput a = tx.getOutput(0);
        final TransactionOutput b = tx.getOutput(1);
        tx.clearOutputs();
        tx.addOutput(b);
        tx.addOutput(a);
        return true;
    }
}
