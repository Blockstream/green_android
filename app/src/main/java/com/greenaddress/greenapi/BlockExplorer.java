package com.greenaddress.greenapi;

public class BlockExplorer {
    private final String address;
    private final String tx;

    public BlockExplorer(final String address, final String tx) {
        this.address = address;
        this.tx = tx;
    }

    public String getAddress() {
        return address;
    }

    public String getTx() {
        return tx;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BlockExplorer {");
        sb.append("address='").append(address).append('\'');
        sb.append(", tx='").append(tx).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
