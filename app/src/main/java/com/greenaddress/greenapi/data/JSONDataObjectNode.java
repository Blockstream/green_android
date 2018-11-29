package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class JSONDataObjectNode extends JSONData {
    private ObjectNode mNode;

    public JSONDataObjectNode(ObjectNode mNode) {
        this.mNode = mNode;
    }

    @Override
    public String toString() {
        return mNode.toString();
    }
}
