package com.greenaddress.greenbits.spv;

import org.bitcoinj.core.BloomFilter;

class PeerFilterProvider implements org.bitcoinj.core.PeerFilterProvider {
    private final SPV mSPV;

    public PeerFilterProvider(final SPV spv) { mSPV = spv; }

    @Override
    public long getEarliestKeyCreationTime() {
        return mSPV.getService().getLoginData().earliestKeyCreationTime;
    }

    @Override
    public int getBloomFilterElementCount() {
        return mSPV.getBloomFilterElementCount();
    }

    @Override
    public BloomFilter getBloomFilter(final int size, final double falsePositiveRate, final long nTweak) {
        return mSPV.getBloomFilter(size, falsePositiveRate, nTweak);
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
