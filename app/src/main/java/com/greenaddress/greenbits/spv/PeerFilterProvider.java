package com.greenaddress.greenbits.spv;

import org.bitcoinj.core.BloomFilter;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;

import java.util.Set;

class PeerFilterProvider implements org.bitcoinj.core.PeerFilterProvider {
    private SPV mSPV;

    public PeerFilterProvider(final SPV spv) { mSPV = spv; }

    public void onDispose() { mSPV = null; }

    @Override
    public long getEarliestKeyCreationTime() {
        if (mSPV == null)
            return 0;
        return mSPV.getService().getLoginData().earliest_key_creation_time;
    }

    @Override
    public int getBloomFilterElementCount() {
        // 1 to avoid downloading full blocks (empty bloom filters are ignored by bitcoinj)
        if (mSPV == null || mSPV.getUnspentOutpoints().isEmpty())
            return 1;
        return mSPV.getUnspentOutpoints().size();
    }

    @Override
    public BloomFilter getBloomFilter(final int size, final double falsePositiveRate, final long nTweak) {

        final BloomFilter filter = new BloomFilter(size, falsePositiveRate, nTweak);
        if (mSPV != null && mSPV.populateBloomFilter(filter) == 0) {
            // Add a fake entry to avoid downloading blocks when filter is empty,
            // as empty bloom filters are ignored by bitcoinj.
            filter.insert(new byte[]{(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef});
        }
        return filter;
    }

    @Override
    public boolean isRequiringUpdateAllBloomFilter() {
        return false;
    }

    public void beginBloomFilterCalculation(){
        //TODO: ??
    }
    public void endBloomFilterCalculation(){
        //TODO: ??
    }
}
