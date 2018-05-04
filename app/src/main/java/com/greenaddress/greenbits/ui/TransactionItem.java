package com.greenaddress.greenbits.ui;
import com.greenaddress.greenapi.GATx;
import com.greenaddress.greenapi.JSONMap;
import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TransactionItem implements Serializable {

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
    public final JSONMap receivedOnEp;
    public final boolean instant;
    public final boolean replaceable;
    public final Sha256Hash txHash;
    public final String doubleSpentBy;
    public final Date date;
    public String memo;
    public boolean spvVerified;
    public final boolean isSpent;
    public final long fee;
    public final int size;
    public final List<Sha256Hash> replacedHashes;
    public final String data;
    public final List<JSONMap> eps;

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

    public TransactionItem(final GaService service, final JSONMap m, final int currentBlock) throws ParseException {
        instant = m.getBool("instant");
        doubleSpentBy = m.get("double_spent_by");

        this.currentBlock = currentBlock;
        fee = m.getLong("fee");
        size = m.get("size");
        replacedHashes = new ArrayList<>();
        data = m.get("data");
        txHash = m.getHash("txhash");

        memo = m.get("memo", null);

        blockHeight = m.get("block_height", null);

        final List<JSONMap> recipients = new ArrayList<>();

        String tmpCounterparty = null;
        long tmpAmount = 0;
        boolean tmpIsSpent = true;
        String tmpReceivedOn = "";
        JSONMap tmpReceivedOnEp = null;
        boolean hasConfidentialRecipients = false;

        eps = m.get("eps");
        for (final JSONMap ep : eps) {
            final String socialDestination = ep.get("social_destination", null);
            boolean externalSocial = false;
            if (socialDestination != null) {
                final Integer scriptType = ep.get("script_type");
                externalSocial = scriptType != GATx.P2SH_FORTIFIED_OUT &&
                                 scriptType != GATx.P2SH_P2WSH_FORTIFIED_OUT;

                final JSONMap socialMap = m.getMap("social_destination");
                if (socialMap == null) {
                    // Old unconverted social_destination string value
                    tmpCounterparty = socialDestination;
                } else {
                    // New style JSON map of social info
                    final String socialType = socialMap.get("type");
                    if (socialType.equals("voucher"))
                        tmpCounterparty = "Voucher";
                    else
                        tmpCounterparty = socialMap.get("name");
                }
            }

            final Boolean isRelevant = ep.get("is_relevant");
            final Boolean isCredit = ep.get("is_credit");

            final boolean confidential;
            confidential = ep.getBool("confidential") || // Confidential own output
                           ep.get("value") == null;      // Confidential foreign output
            if (isCredit && (!isRelevant || socialDestination != null) && ep.get("ad") != null) {
                // Elements fees
                if (confidential)
                    hasConfidentialRecipients = true;
                else
                    recipients.add(ep);
            }

            if (!isRelevant)
                continue;

            if (!isCredit) {
                if (ep.get("value") != null)
                    tmpAmount -= ep.getLong("value");
                continue;
            }

            if (!externalSocial) {
                if (ep.get("value") != null)
                    tmpAmount += ep.getLong("value");
                if (!ep.getBool("is_spent"))
                    tmpIsSpent = false;
            }
            final String ad = ep.getString("ad");
            if (!tmpReceivedOn.isEmpty())
                tmpReceivedOn.concat(", ").concat(ad);
            else {
                tmpReceivedOn = ad;
                if (ep.getBool("confidential"))
                    tmpReceivedOnEp = ep; // Needed for regenerating the confidential address
            }
        }

        if (tmpAmount >= 0) {
            type = TransactionItem.TYPE.IN;
            for (final JSONMap ep : eps)
                if (!ep.getBool("is_credit")) {
                    final String socialSource = ep.get("social_source");
                    if (socialSource != null)
                        tmpCounterparty = socialSource;
                }
        } else {
            tmpReceivedOn = ""; // don't show change addresses
            if (recipients.isEmpty() && !hasConfidentialRecipients)
                type = TransactionItem.TYPE.REDEPOSIT;
            else {
                type = TransactionItem.TYPE.OUT;
                if (tmpCounterparty == null) {
                    if (!recipients.isEmpty())
                        tmpCounterparty = recipients.get(0).get("ad");
                    else if (hasConfidentialRecipients)
                        tmpCounterparty = "Confidential address";
                }
                if (recipients.size() > 1)
                    tmpCounterparty += ", ...";
            }
        }

        amount = tmpAmount;
        counterparty = tmpCounterparty;
        isSpent = tmpIsSpent;
        receivedOn = tmpReceivedOn;
        receivedOnEp = tmpReceivedOnEp;
        spvVerified = service.isSPVVerified(txHash);
        date = m.getDate("created_at");
        // FIXME: Implement RBF for instant transactions
        replaceable = !GaService.IS_ELEMENTS && !instant &&
                      m.getBool("rbf_optin") && type != TransactionItem.TYPE.IN;
    }

    // Return the fee rate in satoshis per 1000 bytes
    final Coin getFeePerKilobyte() {
        if (size <= 0)
            return Coin.ZERO;
        final double perKb = fee * 1000.0 / size;
        return Coin.valueOf((long) Math.ceil(perKb));
    }
}
