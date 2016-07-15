/*
 * Copyright 2013 Google Inc.
 * Copyright 2014 Andreas Schildbach
 * Copyright 2015 GreenAddress
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

package org.bitcoinj.params;


import org.bitcoinj.core.Transaction;

import java.math.BigInteger;

/**
 * Parameters for alpha, a sidechain of Bitcoin TestNetV3
 */
public class AlphaNetParams extends TestNet3Params {
    public AlphaNetParams() {
        super();
        id = ID_ALPHANET;
        dnsSeeds = new String[] {
                "alpha-seed.bluematt.me"  // Matt Corallo
        };
        packetMagic = 0xa11ffa;
        port = 4242;

        Transaction genesisTx = genesisBlock.getTransactions().get(0);
        genesisTx.setFeeCT(BigInteger.ZERO);
        genesisTx.getOutput(0).setRangeProof(new byte[]{});
        genesisTx.getOutput(0).setCommitment(new byte[]{});
        genesisTx.getOutput(0).setNonceCommitment(new byte[]{});
    }

    private static AlphaNetParams instance;
    public static synchronized AlphaNetParams get() {
        if (instance == null) {
            instance = new AlphaNetParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_ALPHANET;
    }
}
