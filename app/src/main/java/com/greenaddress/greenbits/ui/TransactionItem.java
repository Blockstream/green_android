package com.greenaddress.greenbits.ui;

import android.os.Bundle;
import android.util.Log;

import static com.greenaddress.gdk.GDKSession.getSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.greenapi.JSONMap;
import com.greenaddress.greenapi.data.AssetInfoData;
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
import java.util.Map;

public class TransactionItem implements Serializable {

    private static final ObjectMapper mObjectMapper = new ObjectMapper();

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
    public final Integer subaccount;
    public final boolean isAsset;
    public final String assetId;
    public final AssetInfoData assetInfo;

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
        vSize = txData.getTransactionVsize();
        replacedHashes = new ArrayList<>();
        data = txData.getData();
        final String txhash = txData.getTxhash();
        txHash = Sha256Hash.wrap(txhash);
        memo = txData.getMemo();
        blockHeight = txData.getBlockHeight();
        counterparty = "";
        this.subaccount = subaccount;

        assetId = txData.getFirstAsset() == null ? "btc" : txData.getFirstAsset();
        assetInfo = txData.getAssetInfo() == null ? null : txData.getAssetInfo().get(assetId);
        satoshi = txData.getSatoshi().get(assetId);
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

    public String getAssetName() {
        return "btc".equals(assetId) ? "L-BTC" : assetInfo != null &&
               assetInfo.getName() != null ? assetInfo.getName() : assetId;
    }

    public String getAssetTicker() {
        return "btc".equals(assetId) ? "L-BTC" : assetInfo != null &&
               assetInfo.getTicker() != null ? assetInfo.getTicker() : "";
    }

    private AssetInfoData getAssetInfo() {
        final AssetInfoData assetInfoDefault = new AssetInfoData(assetId, assetId, 0, "");
        return assetInfo == null ? assetInfoDefault : assetInfo;
    }

    public String getAmountWithUnit(final GaService service) {
        try {
            final ObjectNode details = mObjectMapper.createObjectNode();
            details.put("satoshi", satoshi);
            if (isAsset)
                details.set("asset_info", getAssetInfo().toObjectNode());
            final ObjectNode converted = getSession().convert(details);
            final String amount = converted.get(isAsset ? assetId : service.getUnitKey()).asText();
            return String.format("%s%s %s", type == TYPE.IN ? "" : "-", amount,
                                 isAsset ? getAssetTicker() : service.getBitcoinUnit());
        } catch (final RuntimeException | IOException e) {
            Log.e("", "Conversion error: " + e.getLocalizedMessage());
            return "";
        }
    }
}
