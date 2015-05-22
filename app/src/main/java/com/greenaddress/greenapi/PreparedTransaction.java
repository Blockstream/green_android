package com.greenaddress.greenapi;

import org.bitcoinj.core.Transaction;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreparedTransaction {
    public final String change_pointer;
    public final int subaccount_pointer;
    public final Boolean requires_2factor;
    public final String tx;
    public final List<Output> prev_outputs;
    public final Transaction decoded;
    public final Map<String, Transaction> prevoutRawTxs;
    public final String twoOfThreeBackupChaincode;
    public final String twoOfThreeBackupPubkey;

    public PreparedTransaction(final Map<?, ?> values, final int subaccount_pointer, final String twoOfThreeBackupChaincode, final String twoOfThreeBackupPubkey) {
        final List tmp = (List) values.get("prev_outputs");
        final List<Output> outputs = new ArrayList<>();
        for (final Object obj : tmp) {
            outputs.add(new Output((Map<?, ?>) obj));
        }
        this.prev_outputs = outputs;
        this.change_pointer = values.get("change_pointer").toString();
        this.requires_2factor = (Boolean) values.get("requires_2factor");
        this.tx = values.get("tx").toString();
        this.decoded = new Transaction(Network.NETWORK, Hex.decode(this.tx));
        Map<String, String> prevoutRawTxStrings = (Map) values.get("prevout_rawtxs");
        prevoutRawTxs = new HashMap<>();
        for (String k : prevoutRawTxStrings.keySet()) {
            prevoutRawTxs.put(k, new Transaction(Network.NETWORK,
                    Hex.decode(prevoutRawTxStrings.get(k))));
        }
        this.subaccount_pointer = subaccount_pointer;
        this.twoOfThreeBackupChaincode = twoOfThreeBackupChaincode;
        this.twoOfThreeBackupPubkey = twoOfThreeBackupPubkey;
    }
}
