package com.greenaddress.greenbits.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.text.DecimalFormat;
import java.text.ParseException;

public class FormatterTextWatcher implements TextWatcher {

    private final EditText editText;
    private final DecimalFormat decimalFormat;

    public FormatterTextWatcher(final EditText editText) {
        this.decimalFormat = new DecimalFormat("#,###.########");
        this.editText = editText;
    }

    @Override
    public void beforeTextChanged(final CharSequence charSequence, final int start, final int count, final int after) {
    }

    @Override
    public void onTextChanged(final CharSequence charSequence, final int start, final int before, final int count) {
    }

    @Override
    public void afterTextChanged(final Editable editable) {
        editText.removeTextChangedListener(this);

        try {
            final int length = editText.getText().length();


            if (!editable.toString().endsWith(String.valueOf(decimalFormat.getDecimalFormatSymbols().getDecimalSeparator()))) {
                final String stringValue = editable.toString().replace(String.valueOf(decimalFormat.getDecimalFormatSymbols().getGroupingSeparator()), "");

                final Number numberValue = decimalFormat.parse(stringValue);
                final int selStart = editText.getSelectionStart();

                editText.setText(decimalFormat.format(numberValue));

                final int endlen = editText.getText().length();
                final int selection = (selStart + (endlen - length));

                if (selection > 0 && selection <= editText.getText().length()) {
                    editText.setSelection(selection);
                } else {
                    editText.setSelection(editText.getText().length() - 1);
                }
            }
        } catch (final ParseException e) {
        } catch (final NumberFormatException nfe) {
        }

        editText.addTextChangedListener(this);
    }

}
