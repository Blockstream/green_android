package com.greenaddress.greenbits.ui;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;

public class CustomTypefaceSpan extends TypefaceSpan {
    private final Typeface newType;

    public CustomTypefaceSpan(final String family, final Typeface type) {
        super(family);
        newType = type;
    }

    private static void applyCustomTypeFace(@NonNull final Paint paint, @NonNull final Typeface tf) {
        final Typeface old = paint.getTypeface();
        final int oldStyle = (old == null) ? 0 : old.getStyle();


        final int fake = oldStyle & ~tf.getStyle();
        if ((fake & Typeface.BOLD) != 0) {
            paint.setFakeBoldText(true);
        }

        if ((fake & Typeface.ITALIC) != 0) {
            paint.setTextSkewX(-0.25f);
        }

        paint.setTypeface(tf);
    }

    @Override
    public void updateDrawState(@NonNull final TextPaint ds) {
        applyCustomTypeFace(ds, newType);
    }

    @Override
    public void updateMeasureState(@NonNull final TextPaint paint) {
        applyCustomTypeFace(paint, newType);
    }
}