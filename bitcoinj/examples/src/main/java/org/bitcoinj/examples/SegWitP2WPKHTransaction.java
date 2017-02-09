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
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.spongycastle.util.encoders.Hex;

/**
 * This example shows how to build a transaction spending a simple segwit output
 */

public class SegWitP2WPKHTransaction {
    public static void main(String[] args) throws Exception {
        NetworkParameters params = TestNet3Params.get();
        final Coin fee = Coin.SATOSHI.times(10000);
        final Coin fundAmount = Coin.SATOSHI.times(405000000);

        final String segwitWIF = "cRynQP5ysWF3jmz5bFy16kqKRoSYzzArJru5349ADBwsoyKoh8aq";
        final ECKey segwitKey = DumpedPrivateKey.fromBase58(params, segwitWIF).getKey();
        final Script segwitPkScript = ScriptBuilder.createP2WPKHOutputScript(segwitKey);

        final Sha256Hash fundTxHash =
            Sha256Hash.wrap("e7e953f119179f71a58865f3d1ae2157847778404f89b48d0f695f786fba1dd4");

        // Sign segwit transaction
        final Address sendTo = Address.fromBase58(params, "mvpr4mkDSPYcbN6XW6xCveCLa38x23fs7B");
        final Coin outAmount = fundAmount.minus(fee);
        final Script outPkScript = ScriptBuilder.createOutputScript(sendTo);
        final TransactionOutPoint segwitOutPoint = new TransactionOutPoint(params, 0L, fundTxHash, fundAmount);
        final Transaction outTx = new Transaction(params);
        outTx.addOutput(outAmount, outPkScript);
        outTx.addSignedInput(segwitOutPoint, segwitPkScript, segwitKey);
        final Sha256Hash outTxHash = outTx.getHash();
        System.out.println(Hex.toHexString(outTx.bitcoinSerialize()));
        System.out.println(Hex.toHexString(outTxHash.getBytes()));
    }
}
