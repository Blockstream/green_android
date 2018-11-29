package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class BumpTxData extends JSONData {
    // {
    // "previous_transaction":  prev_tx_from_tx_list,
    // "fee_rate",: new_desired_fee_rate_in_sat_per_k
    // }

    private JsonNode previousTransaction;
    private Long feeRate;

    public JsonNode getPreviousTransaction() {
        return previousTransaction;
    }

    public void setPreviousTransaction(final JsonNode previousTransaction) {
        this.previousTransaction = previousTransaction;
    }

    public Long getFeeRate() {
        return feeRate;
    }

    public void setFeeRate(final Long feeRate) {
        this.feeRate = feeRate;
    }
}
