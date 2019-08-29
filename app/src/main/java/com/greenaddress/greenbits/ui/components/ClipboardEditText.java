package com.greenaddress.greenbits.ui.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Toast;

import com.greenaddress.greenbits.ui.R;


public class ClipboardEditText extends androidx.appcompat.widget.AppCompatEditText {

    private final Context context;

    public ClipboardEditText(final Context context) {
        super(context);
        this.context = context;
    }

    public ClipboardEditText(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public ClipboardEditText(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    @Override
    public boolean onTextContextMenuItem(final int id) {
        final boolean result = super.onTextContextMenuItem(id);
        switch (id) {
        case android.R.id.cut:
        case android.R.id.copy:
            break;
        case android.R.id.paste:
            Toast.makeText(context, R.string.id_be_aware_other_apps_can_read_or, Toast.LENGTH_LONG).show();
            break;
        }
        return result;
    }
}
