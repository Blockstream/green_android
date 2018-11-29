package com.greenaddress.gdk;


import com.blockstream.libgreenaddress.GDK;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONConverterImpl implements GDK.JSONConverter {

    private static ObjectMapper mObjectMapper = new ObjectMapper();

    @Override
    public Object toJSONObject(String jsonString) {

        if (jsonString == null || jsonString.equals("null")) {
            return null;
        }
        try {
            return mObjectMapper.readTree(jsonString);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toJSONString(Object jsonObject) {
        return jsonObject.toString();
    }



}
