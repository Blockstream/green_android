package com.greenaddress.greenbits.ui;
import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Sha256Hash;
import org.codehaus.jackson.map.MappingJsonFactory;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class TransactionItem implements Serializable {

    public static final int P2SH_FORTIFIED_OUT = 10;
    public enum TYPE {
        OUT,
        IN,
        REDEPOSIT
    }

    public final TYPE type;
    private final int currentBlock;
    private final Integer blockHeight;
    public final long amount;
    public final String counterparty;
    public final String receivedOn;
    public final boolean replaceable;
    public final Sha256Hash txHash;
    public final String doubleSpentBy;
    public final Date date;
    public final String memo;
    public boolean spvVerified;
    public final boolean isSpent;
    public final long fee;
    public final int size;
    public final List<Sha256Hash> replacedHashes;
    public final String data;
    public final List eps;

    public String toString() {
        return String.format("%s %s %s %s", date.toString(), type.name(), amount, counterparty);
    }

    public int getConfirmations() {
        if (blockHeight != null)
            return currentBlock - blockHeight + 1;
        return 0;
    }

    public boolean hasEnoughConfirmations() {
        return getConfirmations() >= 6;
    }

    private Boolean boolVal(final Map<String, Object> m, final String k) { return (Boolean) m.get(k); }
    private String strVal(final Map<String, Object> m, final String k) { return (String) m.get(k); }
    private Map<String, Object> mapVal(final Map<String, Object> m, final String k) {
        try {
            return new MappingJsonFactory().getCodec().readValue(strVal(m, k), Map.class);
        } catch (final IOException e) {
            // e.printStackTrace();
            return null;
        }
    }

    public TransactionItem(final GaService service, final Map<String, Object> txJSON, final int currentBlock) throws ParseException {
        replaceable = txJSON.get("rbf_optin") != null && (Boolean) txJSON.get("rbf_optin");
        doubleSpentBy = strVal(txJSON, "double_spent_by");

        this.currentBlock = currentBlock;
        fee = Long.valueOf(strVal(txJSON, "fee"));
        size = (int) txJSON.get("size");
        replacedHashes = new ArrayList<>();
        data = strVal(txJSON, "data");
        eps = (List) txJSON.get("eps");
        txHash = Sha256Hash.wrap(strVal(txJSON, "txhash"));

        memo = txJSON.containsKey("memo") ? strVal(txJSON, "memo") : null;

        blockHeight = txJSON.containsKey("block_height") && txJSON.get("block_height") != null ?
                (int) txJSON.get("block_height") : null;

        String tmpCounterparty = null;
        long tmpAmount = 0;
        boolean tmpIsSpent = true;
        String tmpReceivedOn = null;
        for (int i = 0; i < eps.size(); ++i) {
            final Map<String, Object> ep = (Map) eps.get(i);
            final boolean isSocial = ep.get("social_destination") != null;
            if (isSocial) {
                final Map<String, Object> socialDestination = mapVal(ep, "social_destination");
                if (socialDestination != null) {
                    tmpCounterparty = socialDestination.get("type").equals("voucher") ?
                            "Voucher" : (String) socialDestination.get("name");
                } else
                    tmpCounterparty = strVal(ep, "social_destination");
            }
            if (boolVal(ep, "is_relevant")) {
                if (boolVal(ep, "is_credit")) {
                    final boolean externalSocial = isSocial && ((Integer) ep.get("script_type")) != P2SH_FORTIFIED_OUT;
                    if (!externalSocial) {
                        tmpAmount += Long.valueOf(strVal(ep, "value"));
                        if (!boolVal(ep, "is_spent"))
                            tmpIsSpent = false;
                    }
                    if (tmpReceivedOn == null)
                        tmpReceivedOn = strVal(ep, "ad");
                    else
                        tmpReceivedOn += ", " + strVal(ep, "ad");
                } else
                    tmpAmount -= Long.valueOf(strVal(ep, "value"));
            }
        }
        if (tmpAmount >= 0) {
            type = TransactionItem.TYPE.IN;
            for (int i = 0; i < eps.size(); ++i) {
                final Map<String, Object> ep = (Map) eps.get(i);
                if (!boolVal(ep, "is_credit") && ep.get("social_source") != null)
                    tmpCounterparty = strVal(ep, "social_source");
            }
        } else {
            tmpReceivedOn = null; // don't show change addresses
            final List<Map<String, Object>> recip_eps = new ArrayList<>();
            for (int i = 0; i < eps.size(); ++i) {
                final Map<String, Object> ep = (Map) eps.get(i);
                if (boolVal(ep, "is_credit") &&
                    (!boolVal(ep, "is_relevant") || ep.get("social_destination") != null)) {
                    recip_eps.add(ep);
                }
            }
            if (recip_eps.size() > 0) {
                type = TransactionItem.TYPE.OUT;
                if (tmpCounterparty == null)
                    tmpCounterparty = (String) recip_eps.get(0).get("ad");
                if (recip_eps.size() > 1)
                    tmpCounterparty += ", ...";
            } else
                type = TransactionItem.TYPE.REDEPOSIT;
        }
        amount = tmpAmount;
        counterparty = tmpCounterparty;
        isSpent = tmpIsSpent;
        receivedOn = tmpReceivedOn;
        spvVerified = service.isSPVVerified(txHash);
        final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        date = df.parse(strVal(txJSON, "created_at"));
    }
}
