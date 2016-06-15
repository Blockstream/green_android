package com.greenaddress.greenapi;

import java.util.Map;

public class Output {

    public final int subAccount;
    public final Integer pointer;
    public final Integer branch;
    public final Integer scriptType;
    public final String script;
    public final Long value;

    public Output(final Integer subAccount, final Integer pointer, final Integer branch, final Integer scriptType, final String script, final Long value) {
        this.subAccount = getInt(subAccount, 0);
        this.pointer = pointer;
        this.branch = branch;
        this.scriptType = scriptType;
        this.script = script;
        this.value = value;
    }

    public Output(final Map<?, ?> values) {
        this.subAccount = getInt((Integer) values.get("subaccount"), 0);
        this.pointer = (Integer) values.get("pointer");
        this.branch = (Integer) values.get("branch");
        this.scriptType = (Integer) values.get("script_type");
        this.script = (String) values.get("script");
        this.value = Long.valueOf((String) values.get("value"));
    }

    private int getInt(final Integer v, final int def) {
        return (v == null ? def : v);
    }
 }
