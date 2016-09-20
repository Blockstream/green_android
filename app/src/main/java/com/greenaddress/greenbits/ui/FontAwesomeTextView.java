package com.greenaddress.greenbits.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class FontAwesomeTextView extends TextView {

    private Typeface mDefaultTypeface;
    private static Typeface mAwesomeTypeface;

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
        mDefaultTypeface = getTypeface();
        // This isn't strictly thread safe the first time through but we assume
        // UI creation from XML creating these Textviews is single threaded,
        // and that seems to be the case.
        if (mAwesomeTypeface == null)
            mAwesomeTypeface = Typeface.createFromAsset(getContext().getAssets(),
                                                        "fonts/fontawesome-webfont.ttf");
        setAwesomeTypeface();
    }

    public void setDefaultTypeface() {
        setTypeface(mDefaultTypeface);
    }

    public void setAwesomeTypeface() {
        setTypeface(mAwesomeTypeface);
    }
}
