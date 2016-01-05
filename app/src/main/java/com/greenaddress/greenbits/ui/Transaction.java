package com.greenaddress.greenbits.ui;

import java.io.Serializable;
import java.util.Date;

public class Transaction implements Serializable {

    public enum TYPE {
        OUT,
        IN,
        REDEPOSIT
    }

    public final TYPE type;
    private final int curBlock;
    private final Integer blockHeight;
    public final long amount;
    public final String counterparty;
    public final String receivedOn;
    public final String txhash;
    public final Date date;
    public final String memo;
    public boolean spvVerified;
    public final boolean isSpent;
    public final long fee;
    public final int size;

    public Transaction(final TYPE type, final long amount, final String counterparty, final Date date, final String txhash, final String memo, final int curBlock, final Integer blockHeight, final boolean spvVerified, final boolean isSpent, final String receivedOn, final long fee, final int size) {
        this.type = type;
        this.amount = amount;
        this.counterparty = counterparty;
        this.date = date;
        this.txhash = txhash;
        this.memo = memo;
        this.curBlock = curBlock;
        this.blockHeight = blockHeight;
        this.spvVerified = spvVerified;
        this.isSpent = isSpent;
        this.receivedOn = receivedOn;
        this.fee = fee;
        this.size = size;
    }

    public String toString() {
        return String.format("%s %s %s %s", date.toString(), type.name(), amount, counterparty);
    }

    public int getConfirmations() {
        if (blockHeight != null)
            return curBlock - blockHeight + 1;
        else
            return 0;
    }

    public boolean hasEnoughConfirmations() {
        return getConfirmations() >= 6;
    }
}
