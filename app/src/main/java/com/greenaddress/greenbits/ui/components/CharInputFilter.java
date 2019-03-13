package com.greenaddress.greenbits.ui.components;

import android.os.Build;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.TextView;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

public class CharInputFilter implements InputFilter {
    private CharsetEncoder asciiEncoder = Charset.forName("ISO-8859-1").newEncoder();

    public static void setIfNecessary(TextView noteText) {
        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            noteText.setFilters(new InputFilter[] {new CharInputFilter()});
        }
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        return asciiEncoder.canEncode(source) ? source : "";
    }

}
