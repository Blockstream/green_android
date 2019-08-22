package com.greenaddress.greenbits.ui.components;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

public class FontAwesomeTextView extends android.support.v7.widget.AppCompatTextView {

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

    public void setAwesomeTypeface() {
        setTypeface(mAwesomeTypeface);
    }
}
