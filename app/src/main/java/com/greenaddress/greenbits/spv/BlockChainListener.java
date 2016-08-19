package com.greenaddress.greenbits.spv;

import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;

import java.util.List;

class BlockChainListener implements org.bitcoinj.core.listeners.BlockChainListener {
    private SPV mSPV;

    public BlockChainListener(final SPV spv) { mSPV = spv; }

    public void onDispose() { mSPV = null; }

    @Override
    public void notifyNewBestBlock(final StoredBlock block) throws VerificationException {

    }

    @Override
    public void reorganize(final StoredBlock splitPoint, final List<StoredBlock> oldBlocks, final List<StoredBlock> newBlocks) throws VerificationException {

    }

    @Override
    public void receiveFromBlock(final Transaction tx, final StoredBlock block, final AbstractBlockChain.NewBlockType blockType, final int relativityOffset) throws VerificationException {
        if (mSPV != null)
            mSPV.getService().notifyObservers(tx.getHash());
    }

    @Override
    public boolean notifyTransactionIsInBlock(final Sha256Hash txHash, final StoredBlock block, final AbstractBlockChain.NewBlockType blockType, final int relativityOffset) throws VerificationException {
        if (mSPV == null)
            return false;
        mSPV.getService().notifyObservers(txHash);
        return mSPV.getUnspentOutputsOutpoints().containsKey(txHash);
    }
}
