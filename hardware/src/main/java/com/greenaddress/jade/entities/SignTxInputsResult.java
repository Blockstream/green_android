package com.greenaddress.jade.entities;

import java.util.List;

public class SignTxInputsResult {
    private final List<byte[]> signatures;
    private final List<byte[]> signerCommitments;

    public SignTxInputsResult(final List<byte[]> signatures, final List<byte[]> signerCommitments) {
        this.signatures = signatures;
        this.signerCommitments = signerCommitments;
    }

    public List<byte[]> getSignatures() {
        return signatures;
    }

    public List<byte[]> getSignerCommitments() {
        return signerCommitments;
    }
}
