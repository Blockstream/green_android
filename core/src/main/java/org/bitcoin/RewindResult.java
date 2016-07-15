package org.bitcoin;

public class RewindResult {
    private byte[] blindingFactor;
    private long value;

    public byte[] getBlindingFactor() {
        return blindingFactor;
    }

    public long getValue() {
        return value;
    }

    public RewindResult(byte[] blindingFactor, long value) {
        this.blindingFactor = blindingFactor;
        this.value = value;
    }
}
