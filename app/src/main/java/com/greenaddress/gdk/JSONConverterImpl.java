package com.greenaddress.gdk;


import com.blockstream.libgreenaddress.GDK;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONConverterImpl implements GDK.JSONConverter {

    private static final ObjectMapper mObjectMapper = new ObjectMapper();

    @Override
    public Object toJSONObject(final String jsonString) {

        if (jsonString != null && !jsonString.equals("null"))
            try {
                return mObjectMapper.readTree(jsonString);
            }
            catch (final Exception e) {
                e.printStackTrace();
            }
        return null;
    }

    @Override
    public String toJSONString(final Object jsonObject) {
        return jsonObject.toString();
    }

}
