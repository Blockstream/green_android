package com.greenaddress.greenbits.ui;
import com.greenaddress.greenbits.GaService;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.codehaus.jackson.map.MappingJsonFactory;

public class TransactionItem implements Serializable {

    public static final int P2SH_FORTIFIED_OUT = 10;
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
    public final boolean replaceable;
    public final String txhash;
    public final String doubleSpentBy;
    public final Date date;
    public final String memo;
    public boolean spvVerified;
    public final boolean isSpent;
    public final long fee;
    public final int size;
    public final List<String> replacedHashes;
    public final String data;
    public final List eps;

    public String toString() {
        return String.format("%s %s %s %s", date.toString(), type.name(), amount, counterparty);
    }

    public int getConfirmations() {
        if (blockHeight != null)
            return curBlock - blockHeight + 1;
        return 0;
    }

    public boolean hasEnoughConfirmations() {
        return getConfirmations() >= 6;
    }

    public TransactionItem(final GaService service, final Map<String, Object> txJSON, final int currentBlock) throws ParseException {
        replaceable = txJSON.get("rbf_optin") != null && (Boolean) txJSON.get("rbf_optin");
        doubleSpentBy = (String) txJSON.get("double_spent_by");

        curBlock = currentBlock;
        fee = Long.valueOf((String)txJSON.get("fee"));
        size = (int) txJSON.get("size");
        replacedHashes = new ArrayList<>();
        data = (String) txJSON.get("data");
        eps = (List) txJSON.get("eps");
        txhash = (String) txJSON.get("txhash");

        memo = txJSON.containsKey("memo") ? (String) txJSON.get("memo") : null;

        blockHeight = txJSON.containsKey("block_height") && txJSON.get("block_height") != null ?
                (int) txJSON.get("block_height") : null;

        String tmpCounterparty = null;
        long tmpAmount = 0;
        boolean tmpIsSpent = true;
        String tmpReceivedOn = null;
        for (int i = 0; i < eps.size(); ++i) {
            final Map<String, Object> ep = (Map<String, Object>) eps.get(i);
            if (ep.get("social_destination") != null) {
                Map<String, Object> social_destination = null;
                try {
                    social_destination = new MappingJsonFactory().getCodec().readValue(
                            (String) ep.get("social_destination"), Map.class);
                } catch (final IOException e) {
                    //e.printStackTrace();
                }

                if (social_destination != null) {
                    tmpCounterparty = social_destination.get("type").equals("voucher") ?
                            "Voucher" : (String) social_destination.get("name");
                } else {
                    tmpCounterparty = (String) ep.get("social_destination");
                }
            }
            if (((Boolean) ep.get("is_relevant"))) {
                if (((Boolean) ep.get("is_credit"))) {
                    final boolean external_social = ep.get("social_destination") != null &&
                            ((Integer) ep.get("script_type")) != P2SH_FORTIFIED_OUT;
                    if (!external_social) {
                        tmpAmount += Long.valueOf((String) ep.get("value"));
                        if (!((Boolean) ep.get("is_spent"))) {
                            tmpIsSpent = false;
                        }
                    }
                    if (tmpReceivedOn == null) {
                        tmpReceivedOn = (String) ep.get("ad");
                    } else {
                        tmpReceivedOn += ", " + ep.get("ad");
                    }
                } else {
                    tmpAmount -= Long.valueOf((String) ep.get("value"));
                }
            }
        }
        if (tmpAmount >= 0) {
            type = TransactionItem.TYPE.IN;
            for (int i = 0; i < eps.size(); ++i) {
                final Map<String, Object> ep = (Map<String, Object>) eps.get(i);
                if (!((Boolean) ep.get("is_credit")) && ep.get("social_source") != null) {
                    tmpCounterparty = (String) ep.get("social_source");
                }
            }
        } else {
            tmpReceivedOn = null; // don't show change addresses
            final List<Map<String, Object>> recip_eps = new ArrayList<>();
            for (int i = 0; i < eps.size(); ++i) {
                final Map<String, Object> ep = (Map<String, Object>) eps.get(i);
                if (((Boolean) ep.get("is_credit")) &&
                        (!((Boolean) ep.get("is_relevant")) ||
                                ep.get("social_destination") != null)) {
                    recip_eps.add(ep);
                }
            }
            if (recip_eps.size() > 0) {
                type = TransactionItem.TYPE.OUT;
                if (tmpCounterparty == null) {
                    tmpCounterparty = (String) recip_eps.get(0).get("ad");
                }
                if (recip_eps.size() > 1) {
                    tmpCounterparty += ", ...";
                }
            } else {
                type = TransactionItem.TYPE.REDEPOSIT;
            }
        }
        amount = tmpAmount;
        counterparty = tmpCounterparty;
        isSpent = tmpIsSpent;
        receivedOn = tmpReceivedOn;
        spvVerified = service.cfgIn("verified_utxo_").getBoolean(txhash, false);
        final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        date = df.parse((String) txJSON.get("created_at"));
    }
}
