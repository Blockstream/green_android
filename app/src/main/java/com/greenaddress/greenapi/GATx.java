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
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import com.blockstream.libwally.Wally;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import android.util.Log;

public class GATx {

    private static final String TAG = GaService.class.getSimpleName();
    private static final int SIG_LEN = 71; // Average signature length
    private static final List<byte[]> EMPTY_SIGS = ImmutableList.of(new byte[SIG_LEN], new byte[SIG_LEN]);
    private static final byte[] EMPTY_WITNESS_DATA = new byte[0];
    private static final byte[] EMPTY_WITNESS_SIG = new byte[SIG_LEN + 1]; // 1=Sighash flag byte

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
        if (getOutScriptType(scriptType) == P2SH_P2WSH_FORTIFIED_OUT) {
           // To calculate the tx weight correctly, we must set the witness data
           // to the correct number of pushes of data of the correct size.
           // Before sending the transaction, we replace this witness data with
           // just the users signature, since the server recreates the witness
           // data itself to replace the server sig and script placeholders.
            witness = new TransactionWitness(4);
            witness.setPush(0, EMPTY_WITNESS_DATA); // Dummy for off by 1 in OP_CHECKMULTISIG
            witness.setPush(1, EMPTY_WITNESS_SIG);  // Users signature
            witness.setPush(2, EMPTY_WITNESS_SIG);  // GA Server signature
            witness.setPush(3, outscript);          // Outscript
        }
        in.setSequenceNumber(0); // This ensures nlocktime is recognized
        tx.addInput(in);
        if (witness != null)
            tx.setWitness(tx.getInputs().size() - 1, witness);
    }

    private static Address createChangeAddress(final JSONMap addrInfo) {
        byte[] script = addrInfo.getBytes("script");
        if (addrInfo.getString("addr_type").equals("p2wsh"))
            script = ScriptBuilder.createP2WSHOutputScript(Wally.sha256(script)).getProgram();
        return Address.fromP2SHHash(Network.NETWORK, Utils.sha256hash160(script));
    }

    /* Add a new change output to a tx */
    public static Pair<TransactionOutput, Integer> addChangeOutput(final GaService service, final Transaction tx,
                                                                   final int subaccount) {
        final JSONMap addrInfo = service.getNewAddress(subaccount);
        if (addrInfo == null)
            return null;
        return new Pair<>(tx.addOutput(Coin.ZERO, createChangeAddress(addrInfo)),
                          addrInfo.getInt("pointer"));
    }

    /* Identify the change output in a tx */
    public static Pair<TransactionOutput, Integer> findChangeOutput(final List<JSONMap> endPoints,
                                                                    final Transaction tx) {
        int index = -1;
        int pubkey_pointer = -1;
        for (final JSONMap ep : endPoints) {
            if (!ep.getBool("is_credit") || !ep.getBool("is_relevant"))
                continue;

            if (index != -1) {
                // Found another output paying to this account. This can
                // only happend when redepositing to our own acount with
                // a change output (e.g. by manually sending some amount
                // of funds to ourself). In this case the change output
                // will have been created after the amount output by the
                // tx construction code, so the output with the highest
                // pubkey pointer is our change, as they are incremented
                // for each new output. Note that we can't use the order
                // of the output in the tx due to change randomisation.
                if (ep.getInt("pubkey_pointer") < pubkey_pointer)
                    continue; // Not our change output
            }
            index = ep.getInt("pt_idx");
            pubkey_pointer = ep.getInt("pubkey_pointer");
        }
        return new Pair<>(tx.getOutput(index), pubkey_pointer);
    }

    /* Swap the change and recipient output in a tx with 50% probability */
    public static boolean randomizeChangeOutput(final Transaction tx) {
        if (CryptoHelper.randomBytes(1)[0] < 0)
            return false;

        final TransactionOutput a = tx.getOutput(0);
        final TransactionOutput b = tx.getOutput(1);
        tx.clearOutputs();
        tx.addOutput(b);
        tx.addOutput(a);
        return true;
    }

    /* Create previous outputs for tx construction from uxtos */
    public static List<Output> createPrevouts(final GaService service, final List<JSONMap> utxos) {
        final List<Output> prevOuts = new ArrayList<>();
        for (final JSONMap utxo : utxos)
            prevOuts.add(new Output(utxo.getInt("subaccount"),
                                    utxo.getInt(utxo.getKey("pubkey_pointer", "pointer")),
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

    public static Coin getFeeEstimate(final GaService service, final boolean isInstant) {
        return getFeeEstimate(service, isInstant, 6); // Fee rate for 6 confs if not instant
    }

    public static Coin getFeeEstimateForRBF(final GaService service, final boolean isInstant) {
        return getFeeEstimate(service, isInstant, 1); // Fee rate for 1 conf if not instant
    }

    // Return the best estimate of the fee rate in satoshi/1000 bytes
    public static Coin getFeeEstimate(final GaService service, final boolean isInstant, final int forBlock) {
        Double bestInstantRate = null;

        // Iterate the estimates from shortest to longest confirmation time
        final SortedSet<Integer> keys = new TreeSet<>();
        for (final String block : service.getFeeEstimates().mData.keySet())
            keys.add(Integer.parseInt(block));

        for (final Integer blockNum : keys) {
            if (!isInstant && blockNum < forBlock)
                continue; // Non-instant: Use forBlock confirmation rate and later only

            double feeRate = service.getFeeRate(blockNum);
            if (feeRate <= 0.0)
                continue; // No estimate available: Try next confirmation rate

            if (isInstant) {
                // For instant, increase the rate to increase the likelyhood of confirmation.
                // We use the lowest value of:
                // a) 1.1 * the 1st or 2nd block fee rate
                // b) 2.0 * the first rate later than 2 blocks
                if (blockNum <= 2) {
                    if (bestInstantRate == null)
                       bestInstantRate = feeRate * 1.1; // Save earliest fast confirmation rate
                    continue; // Continue to find the first non-fast rate
                } else
                    feeRate *= 2.0;
            }

            if (bestInstantRate != null && bestInstantRate < feeRate)
                feeRate = bestInstantRate; // Use the lowest instant rate found

            return Coin.valueOf((long) (feeRate * 1000 * 1000 * 100));
        }

        if (bestInstantRate != null) {
            // No non-fast confirmation rate, return the fast confirmation rate
            return Coin.valueOf((long) (bestInstantRate * 1000 * 1000 * 100));
        }

        // We don't have a usable fee rate estimate, use a default.
        if (GaService.IS_ELEMENTS)
            return Coin.valueOf(1);
        if (Network.NETWORK == MainNetParams.get())
            return Coin.valueOf((isInstant ? 200 : 120) * 1000);
        return Coin.valueOf((isInstant ? 75 : 60) * 1000);
    }


    public static Coin addUtxo(final GaService service, final Transaction tx,
                               final List<JSONMap> utxos, final List<JSONMap> used) {
        return addUtxo(service, tx, utxos, used, null, null, null, null);
    }

    public static Coin addUtxo(final GaService service, final Transaction tx,
                               final List<JSONMap> utxos, final List<JSONMap> used,
                               final List<Long> inValues, final List<byte[]> inAssetIds,
                               final List<byte[]> inAbfs, final List<byte[]> inVbfs) {
        final JSONMap utxo = utxos.get(0);
        utxos.remove(0);
        if (utxo.getBool("confidential")) {
            inAssetIds.add(utxo.getBytes("assetId"));
            inAbfs.add(utxo.getBytes("abf"));
            inVbfs.add(utxo.getBytes("vbf"));
        }
        used.add(utxo);
        addInput(service, tx, utxo);
        if (inValues != null)
            inValues.add(utxo.getLong("value"));
        return utxo.getCoin("value");
    }

    public static int getTxVSize(final Transaction tx) {
        final int vSize;

        if (!(GaService.IS_ELEMENTS || tx.hasWitness())) {
            vSize = tx.unsafeBitcoinSerialize().length;
            Log.d(TAG, "getTxVSize(non-sw): " + vSize);
        } else {
            /* For segwit, the fee is based on the weighted size of the tx */
            tx.transactionOptions = TransactionOptions.NONE;
            final int nonSwSize = tx.unsafeBitcoinSerialize().length;
            tx.transactionOptions = TransactionOptions.ALL;
            final int swSize = tx.unsafeBitcoinSerialize().length;
            final int fullSize = swSize + estimateElementsSize(tx);
            vSize = (int) Math.ceil((nonSwSize * 3 + fullSize) / 4.0);
            Log.d(TAG, "getTxVSize(sw): " + nonSwSize + '/' + swSize + '/' + vSize);
        }
        return vSize;
    }

    // Calculate the fee that must be paid for a tx
    public static Coin getTxFee(final GaService service, final Transaction tx, final Coin feeRate) {
        final Coin minRate = service.getMinFeeRate();
        final Coin rate = feeRate.isLessThan(minRate) ? minRate : feeRate;
        Log.d(TAG, "getTxFee(rates): " + rate.value + '/' + feeRate.value + '/' + minRate.value);

        final int vSize = getTxVSize(tx);
        final double fee = (double) vSize * rate.value / 1000.0;
        final long roundedFee = (long) Math.ceil(fee); // Round up
        Log.d(TAG, "getTxFee: fee is " + roundedFee);
        return Coin.valueOf(roundedFee);
    }

    public static PreparedTransaction signTransaction(final GaService service, final Transaction tx,
                                                      final List<JSONMap> usedUtxos,
                                                      final int subAccount,
                                                      final Pair<TransactionOutput, Integer> changeOutput) {

        // Fetch previous outputs
        final List<Output> prevOuts = createPrevouts(service, usedUtxos);
        final PreparedTransaction ptx;
        ptx = new PreparedTransaction(changeOutput == null ? null : changeOutput.second,
                                      subAccount, tx,
                                      service.findSubaccountByType(subAccount, "2of3"));
        ptx.mPrevoutRawTxs = new HashMap<>();
        for (final Transaction prevTx : getPreviousTransactions(service, tx))
            ptx.mPrevoutRawTxs.put(Wally.hex_from_bytes(prevTx.getHash().getBytes()), prevTx);

        final boolean isSegwitEnabled = service.isSegwitEnabled();

        // Sign the tx
        final List<byte[]> signatures = service.signTransaction(tx, ptx, prevOuts);
        for (int i = 0; i < signatures.size(); ++i) {
            final byte[] sig = signatures.get(i);
            final JSONMap utxo = usedUtxos.get(i);
            final int scriptType = utxo.getInt("script_type");
            final byte[] outscript = createOutScript(service, utxo);
            final List<byte[]> userSigs = ImmutableList.of(new byte[]{0}, sig);
            final byte[] inscript = createInScript(userSigs, outscript, scriptType);

            tx.getInput(i).setScriptSig(new Script(inscript));
            if (isSegwitEnabled && getOutScriptType(scriptType) == P2SH_P2WSH_FORTIFIED_OUT) {
                // Replace the witness data with just the user signature:
                // the server will recreate the witness data to include the
                // dummy OP_CHECKMULTISIG push, user + server sigs and script.
                final TransactionWitness witness = new TransactionWitness(1);
                witness.setPush(0, sig);
                tx.setWitness(i, witness);
            }
        }
        return ptx;
    }

}
