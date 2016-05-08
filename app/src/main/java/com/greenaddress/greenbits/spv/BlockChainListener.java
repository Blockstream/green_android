package com.greenaddress.greenbits.spv;

import android.support.annotation.NonNull;

import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;

import java.util.List;

class BlockChainListener implements org.bitcoinj.core.listeners.BlockChainListener {
    private final GaService gaService;

    public BlockChainListener(final GaService gaService) {
        this.gaService = gaService;
    }

    @Override
    public void notifyNewBestBlock(final StoredBlock block) throws VerificationException {

    }

    @Override
    public void reorganize(final StoredBlock splitPoint, final List<StoredBlock> oldBlocks, final List<StoredBlock> newBlocks) throws VerificationException {

    }

    @Override
    public void receiveFromBlock(@NonNull final Transaction tx, final StoredBlock block, final AbstractBlockChain.NewBlockType blockType, final int relativityOffset) throws VerificationException {
        gaService.notifyObservers(tx.getHash());
    }

    @Override
    public boolean notifyTransactionIsInBlock(@NonNull final Sha256Hash txHash, final StoredBlock block, final AbstractBlockChain.NewBlockType blockType, final int relativityOffset) throws VerificationException {
        gaService.notifyObservers(txHash);
        return gaService.getUnspentOutputsOutpoints().keySet().contains(txHash);
    }
}