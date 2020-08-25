package com.greenaddress.greenbits.ui.components;

import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.widget.EditText;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class AmountTextWatcher implements TextWatcher {

    private final DecimalFormat decFormat = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
    private final DecimalFormatSymbols symbols = decFormat.getDecimalFormatSymbols();
    private final String defaultSeparator = Character.toString(symbols.getDecimalSeparator());
    private final String otherSeparator = ".".equals(defaultSeparator) ? "," : ".";
    private boolean isEditing = false;
    private EditText editText;

    public AmountTextWatcher(final EditText editText) {
        this.editText = editText;
    }

    public final String getDefaultSeparator() {
        return defaultSeparator;
    }

    @Override
    public void beforeTextChanged(final CharSequence charSequence, final int i, final int i1, final int i2) { }

    @Override
    public void onTextChanged(final CharSequence charSequence, final int i, final int i1, final int i2) { }

    @Override
    public void afterTextChanged(final Editable editable) {
        if (isEditing)
            return;
        isEditing = true;
        final int index = editable.toString().indexOf(otherSeparator);
        if (index >= 0)
            editable.replace(index,index+1, defaultSeparator);

        DigitsKeyListener digitsKeyListener = DigitsKeyListener.getInstance("0123456789.,");
        if (editable.toString().contains(".") || editable.toString().contains(","))
            digitsKeyListener = DigitsKeyListener.getInstance("0123456789");
        editText.setKeyListener(digitsKeyListener);

        isEditing =false;
    }
}
