package com.greenaddress.greenbits.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class FontAwesomeTextView extends TextView {

    private Typeface defaultTypeface;

    public FontAwesomeTextView(final Context context) {
        super(context);
        if (!isInEditMode())
            init();
    }

    public FontAwesomeTextView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode())
            init();
    }

    public FontAwesomeTextView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode())
            init();
    }

    @SuppressLint("NewApi")
    public FontAwesomeTextView(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        if (!isInEditMode())
            init();
    }

    private void init() {
        defaultTypeface = getTypeface();
        setAwesomeTypeface();
    }

    public void setDefaultTypeface() {
        setTypeface(defaultTypeface);
    }

    public void setAwesomeTypeface() {
        setTypeface(
                Typeface.createFromAsset(getContext().getAssets(), "fonts/fontawesome-webfont.ttf")
        );
    }
}