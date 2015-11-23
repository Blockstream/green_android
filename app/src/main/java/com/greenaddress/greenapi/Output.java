package com.greenaddress.greenapi;

import java.util.Map;

public class Output {
    private Integer subaccount;
    private Integer pointer;
    private Integer branch;
    private String value;
    private String script;

    public Output(final Map<?, ?> values) {
        this.setSubaccount((Integer) values.get("subaccount"));
        this.setPointer((Integer) values.get("pointer"));
        this.setBranch((Integer) values.get("branch"));
        this.setValue((String) values.get("value"));
        this.setScript((String) values.get("script"));
    }

    public Integer getSubaccount() {
        return subaccount;
    }

    private void setSubaccount(Integer subaccount) {
        this.subaccount = subaccount;
    }

    public Integer getPointer() {
        return pointer;
    }

    private void setPointer(final Integer pointer) {
        this.pointer = pointer;
    }

    public Integer getBranch() {
        return branch;
    }

    private void setBranch(Integer branch) {
        this.branch = branch;
    }

    private void setValue(String value) {
        this.value = value;
    }

    public String getScript() {
        return script;
    }

    private void setScript(String script) {
        this.script = script;
    }
}
