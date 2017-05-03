package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

public class MnemonicEditText extends android.support.v7.widget.AppCompatEditText {
    public MnemonicEditText(final Context context) {
        super(context);
    }

    public MnemonicEditText(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public MnemonicEditText(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public InputConnection onCreateInputConnection(final EditorInfo outAttrs) {
        final InputConnection conn = super.onCreateInputConnection(outAttrs);
        outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        return conn;
    }
}
