package com.blockstream.green.utils

import android.text.Editable
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.widget.EditText
import java.text.DecimalFormat
import java.util.Locale

class AmountTextWatcher private constructor(val editText: EditText) : TextWatcher {
    private val decFormat = DecimalFormat.getInstance(Locale.getDefault()) as DecimalFormat
    private val symbols = decFormat.decimalFormatSymbols
    private val defaultSeparator = symbols.decimalSeparator.toString()
    private val otherSeparator = if ("." == defaultSeparator) "," else "."

    private val decimalKeyListener = DigitsKeyListener.getInstance("0123456789.,")
    private val onlyNumbersKeyListener = DigitsKeyListener.getInstance("0123456789")

    private var isEditing = false

    init {
        editText.addTextChangedListener(this)
        editText.keyListener = decimalKeyListener
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }

    override fun afterTextChanged(s: Editable?) {
        if (isEditing) return

        s?.let {
            isEditing = true

            val index: Int = s.indexOf(otherSeparator)
            if (index >= 0) s.replace(index, index + 1, defaultSeparator)
            editText.keyListener = if(s.contains(defaultSeparator)) onlyNumbersKeyListener else decimalKeyListener
        }

        isEditing = false
    }

    companion object{
        fun watch(editText: EditText) = AmountTextWatcher(editText)
    }
}