package com.greenaddress.greenbits.ui;

import android.os.Bundle;
import android.util.Log;

import com.greenaddress.greenapi.JSONMap;
import com.greenaddress.greenapi.data.TransactionData;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Sha256Hash;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionItem implements Serializable {

    public enum TYPE {
        OUT,
        IN,
        REDEPOSIT
    }

    public final TYPE type;
    private final int currentBlock;
    private final Integer blockHeight;
    public final long satoshi;
    public String counterparty;
    public final JSONMap receivedOnEp;
    public final boolean replaceable;
    public final Sha256Hash txHash;
    public final String doubleSpentBy;
    public final Date date;
    public String memo;
    public boolean spvVerified;
    public Boolean isSpent;
    public final long fee;
    public final long feeRate;
    public final Integer size;
    public final Integer vSize;
    public final List<Sha256Hash> replacedHashes;
    public final String data;
    public final List<TransactionData> eps;
    public Integer subaccount;
    public boolean isAsset;
    public String asset;

    public String toString() {
        return String.format("%s %s %s %s", date.toString(), type.name(), satoshi, counterparty);
    }

    public int getConfirmations() {
        if (blockHeight == 0)
            return 0;
        if (blockHeight != null)
            return currentBlock - blockHeight + 1;
        return 0;
    }

    public boolean hasEnoughConfirmations() {
        return getConfirmations() >= 6;
    }

    public TransactionItem(final GaService service, final TransactionData txData, final int currentBlock,
                           final int subaccount) throws ParseException {
        doubleSpentBy = null; //TODO gdk;

        this.currentBlock = currentBlock;
        fee = txData.getFee();
        feeRate = txData.getFeeRate();
        size = txData.getTransactionSize();
        vSize =txData.getTransactionVsize();
        replacedHashes = new ArrayList<>();
        data = txData.getData();
        final String txhash = txData.getTxhash();
        txHash = Sha256Hash.wrap(txhash);
        memo = txData.getMemo();
        blockHeight = txData.getBlockHeight();
        counterparty = "";
        this.subaccount = subaccount;

        asset = txData.getFirstAsset() == null ? "btc" : txData.getFirstAsset();
        satoshi = txData.getSatoshi().get(asset);
        isAsset = txData.isAsset();

        switch (txData.getType()) {
        case "outgoing":
            type = TYPE.OUT;
            counterparty = txData.getAddressee();
            break;
        case "incoming":
            type = TYPE.IN;
            break;
        case "redeposit":
            // the amount is the fee
            type = TYPE.REDEPOSIT;
            break;
        default:
            throw new ParseException("cannot parse type", 0);
        }

        // TODO gdk
        eps = new ArrayList<>();
        receivedOnEp = new JSONMap();
        /////////////////

        isSpent = true;
        final Model model = service.getModel();
        final List<TransactionData> transactionDataList =
            model.getUTXODataObservable(subaccount).getTransactionDataList();
        if (transactionDataList == null) {
            isSpent = false;
        } else {
            for (TransactionData transactionData : transactionDataList) {
                if (txhash.equals(transactionData.getTxhash())) {
                    isSpent = false;
                }
            }
        }

        spvVerified = service.isSPVVerified(txHash);

        date = txData.getCreatedAt();
        replaceable = !service.isLiquid() &&
                      txData.getCanRbf() && type != TransactionItem.TYPE.IN;
    }

    public String getAmountWithUnit(final GaService service) {
        if (isAsset) {
            return (type == TYPE.IN ? "" : "-") + service.getValueString(satoshi,false,false);
        } else {
            final String unitKey = service.getUnitKey();
            try {
                final String amount = service.getSession().convertSatoshi(this.satoshi).get(unitKey).asText();
                return (type == TYPE.IN ? "" : "-") + amount + " " + service.getBitcoinUnit();
            } catch (final RuntimeException | IOException e) {
                Log.e("", "Conversion error: " + e.getLocalizedMessage());
                return "";
            }
        }
    }


}
