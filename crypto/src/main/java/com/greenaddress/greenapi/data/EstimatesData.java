package com.greenaddress.greenapi.data;

import java.util.List;

public class EstimatesData extends JSONData {
    public List<Long> fees;

    public List<Long> getFees() {
        return fees;
    }

    public void setFees(final List<Long> fees) {
        this.fees = fees;
    }
}
