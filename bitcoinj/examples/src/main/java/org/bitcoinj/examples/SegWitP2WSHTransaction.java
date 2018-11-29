/*
 * Copyright 2016 Jean-Pierre Rupp
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

package org.bitcoinj.examples;

import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.spongycastle.util.encoders.Hex;

/**
 * This example shows how to build a transaction spending a simple segwit output
 */

public class SegWitP2WSHTransaction {
    public static void main(String[] args) throws Exception {
        NetworkParameters params = TestNet3Params.get();
        final Coin fee = Coin.SATOSHI.times(10000);
        final Coin fundAmount = Coin.SATOSHI.times(60700000);

        // Funding segwit address
        final String segwitWIF = "cU4tWJk3BGymoJgbGbxNA6NJapTwrbfWWaPsz1bCZBzkoeszb4ML";
        final ECKey segwitKey = DumpedPrivateKey.fromBase58(params, segwitWIF).getKey();
        final Script segwitScript = ScriptBuilder.createOutputScript(segwitKey);
        final Sha256Hash fundTxHash =
            Sha256Hash.wrap("09888a2fcfb9b982e2765e56c866f12c426687fc35be2a6de52ea956501c7778");

        // Sign segwit transaction
        final Address sendTo = Address.fromBase58(params, "mkoC1zHJJeNnyr8ttonNzh2ZZV4b9qAJtd");
        final Coin outAmount = fundAmount.minus(fee);
        final Script outPkScript = ScriptBuilder.createOutputScript(sendTo);
        final Transaction outTx = new Transaction(params);
        outTx.addOutput(outAmount, outPkScript);

        final TransactionInput input = outTx.addInput(fundTxHash, 0L, new Script(new byte[0]));
        final TransactionWitness witness = new TransactionWitness(2);
        Sha256Hash sigHash =
            outTx.hashForSignatureWitness(0, segwitScript, fundAmount, Transaction.SigHash.ALL, false);
        ECKey.ECDSASignature sig = segwitKey.sign(sigHash);
        TransactionSignature txSig = new TransactionSignature(sig, Transaction.SigHash.ALL, false);
        witness.setPush(0, txSig.encodeToBitcoin());
        witness.setPush(1, segwitScript.getProgram());

        outTx.setWitness(0, witness);

        final Sha256Hash outTxHash = outTx.getHash();
        System.out.println("Has witnesses: " + outTx.hasWitness());
        System.out.println(Hex.toHexString(outTx.bitcoinSerialize()));
        System.out.println(Hex.toHexString(outTxHash.getBytes()));
    }
}
