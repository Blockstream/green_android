package com.greenaddress.greenapi;

import java.util.Map;

public class Output {

    public final Integer subaccount;
    public final Integer pointer;
    public final Integer branch;
    public final Integer scriptType;
    public final String script;
    public final Long value;

    public Output(final Map<?, ?> values) {
        this.subaccount =(Integer) values.get("subaccount");
        this.pointer = (Integer) values.get("pointer");
        this.branch = (Integer) values.get("branch");
        this.scriptType = (Integer) values.get("script_type");
        this.script = (String) values.get("script");
        this.value = Long.valueOf((String) values.get("value"));
    }
}
