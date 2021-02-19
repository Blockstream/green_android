package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class JSONData {

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
