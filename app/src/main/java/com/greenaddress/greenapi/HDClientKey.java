package com.greenaddress.greenapi;

import android.util.SparseArray;

import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;

import java.util.ArrayList;
import java.util.Map;

public class HDClientKey {
    private static final SparseArray<DeterministicKey> mClientKeys = new SparseArray<>();

    //
    // Temporary methods for use while converting from DeterministicKey
    private static DeterministicKey deriveChildKey(final DeterministicKey parent, final Integer childNum) {
        return HDKeyDerivation.deriveChildKey(parent, new ChildNumber(childNum));
    }

    public static DeterministicKey getMyPublicKey(final int subAccount, final Integer pointer) {
        final DeterministicKey ret = deriveChildKey(mClientKeys.get(subAccount), HDKey.BRANCH_REGULAR);
        if (pointer == null)
            return ret;
        return deriveChildKey(ret, pointer); // Child
    }

    public static void resetCache(final ArrayList<Map<String, Object>> subAccounts,
                                  final ISigningWallet hdParent) {
        synchronized (mClientKeys) {
            mClientKeys.clear();
            if (hdParent == null)
                return;
            mClientKeys.put(0, hdParent.getSubAccountPublicKey(0));
            for (final Map<String, Object> subaccount : subAccounts) {
                final DeterministicKey key = hdParent.getSubAccountPublicKey((Integer) subaccount.get("pointer"));
                mClientKeys.put((Integer) subaccount.get("pointer"), key);
            }
        }
    }
}
