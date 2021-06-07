package com.blockstream.green.filters

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.DigitsKeyListener


class NumberValueFilter(private val digits: Int = 2) : DigitsKeyListener(false, true) {

    override fun filter(
        source: CharSequence, start: Int, end: Int,
        dest: Spanned, dstart: Int, dend: Int
    ): CharSequence {
        // replace ',' to '.'
        var source : CharSequence = source.replace(Regex(","), ".")
        var start = start
        var end = end
        val out = super.filter(source, start, end, dest, dstart, dend)

        // if changed, replace the source
        if (out != null) {
            source = out
            start = 0
            end = out.length
        }
        val len = end - start

        // if deleting, source is empty
        // and deleting can't break anything
        if (len == 0) {
            return source
        }
        val dlen = dest.length

        // Find the position of the decimal .
        for (i in 0 until dstart) {
            if (dest[i] == '.') {
                // being here means, that a number has
                // been inserted after the dot
                // check if the amount of digits is right
                return if (dlen - (i + 1) + len > digits) "" else SpannableStringBuilder(
                    source,
                    start,
                    end
                )
            }
        }
        for (i in start until end) {
            if (source[i] == '.') {
                // being here means, dot has been inserted
                // check if the amount of digits is right
                return if (dlen - dend + (end - (i + 1)) > digits) "" else break // return new SpannableStringBuilder(source, start, end);
            }
        }

        // if the dot is after the inserted part,
        // nothing can break
        return SpannableStringBuilder(source, start, end)
    }
}