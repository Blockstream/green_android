package com.greenaddress.greenbits.ui.transactions;

import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.greenapi.JSONMap;
import com.greenaddress.greenapi.data.AssetInfoData;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.TransactionData;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Sha256Hash;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.greenaddress.gdk.GDKSession.getSession;

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

    public Map<String, BalanceData> mAssetBalances;

    public String toString() {
        return String.format("%s %s %s", date.toString(), type.name(), counterparty);
    }

    public Map<String, BalanceData> getAssetBalances() {
        return mAssetBalances;
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
        isAsset = txData.isAsset();

        mAssetBalances = new HashMap<>();
        for (Map.Entry<String, Long> entry : txData.getSatoshi().entrySet()) {
            BalanceData balance = new BalanceData();
            balance.setSatoshi(entry.getValue());
            if (txData.getAssetInfo() != null) {
                balance.setAssetInfo(txData.getAssetInfo().get(entry.getKey()));
            }
            mAssetBalances.put(entry.getKey(), balance);
        }

        switch (txData.getType()) {
        case "outgoing":
            // don't show outgoing btc amount if it's the tx fees
            type = TYPE.OUT;
            counterparty = txData.getAddressee();
            if (mAssetBalances.get("btc").getSatoshi() == fee) {
                mAssetBalances.remove("btc");
            }
            break;
        case "incoming":
            type = TYPE.IN;
            break;
        case "redeposit":
            // the amount is the fee
            type = TYPE.REDEPOSIT;
            if (mAssetBalances.keySet().size() > 1) {
                mAssetBalances.remove("btc");
            }
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

    public String getAmountWithUnit(final GaService service, final String assetId) {
        try {
            if (type == TYPE.REDEPOSIT) {
                final String feeAmount = amountToString(fee, service.getUnitKey(), null);
                return String.format("-%s %s", feeAmount, service.getBitcoinOrLiquidUnit());
            }

            AssetInfoData assetInfo = mAssetBalances.get(assetId).getAssetInfo();
            if (assetInfo == null)
                assetInfo = new AssetInfoData(assetId, assetId, 0, "", "");
            final String amount = amountToString(mAssetBalances.get(assetId).getSatoshi(),
                                                 isAsset ? assetId : service.getUnitKey(),
                                                 isAsset ? assetInfo.toObjectNode() : null);
            final String denom =
                isAsset ? (assetInfo.getTicker() !=
                           null ? assetInfo.getTicker() : "") : service.getBitcoinOrLiquidUnit();
            return String.format("%s%s %s", type == TYPE.OUT ? "-" : "", amount, denom);
        } catch (final RuntimeException | IOException e) {
            Log.e("", "Conversion error: " + e.getLocalizedMessage());
            return "";
        }
    }

    private String amountToString(long satoshiAmount, String assetId, ObjectNode assetInfo) throws IOException {
        final ObjectNode details = mObjectMapper.createObjectNode();
        details.put("satoshi", satoshiAmount);
        if (assetInfo != null) {
            details.set("asset_info", assetInfo);
        }
        final ObjectNode converted = getSession().convert(details);
        return converted.get(assetId).asText();
    }
}
