package com.greenaddress.jade.entities;

public class SignMessageResult {
    private final String signature;
    private final byte[] signerCommitment;

    public SignMessageResult(final String signature, final byte[] signerCommitment) {
        this.signature = signature;
        this.signerCommitment = signerCommitment;
    }

    public String getSignature() {
        return signature;
    }

    public byte[] getSignerCommitment() {
        return signerCommitment;
    }
}
