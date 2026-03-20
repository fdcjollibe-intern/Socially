package com.apollo.socially.utils

import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.graphics.Typeface

/**
 * Watches an EditText and automatically applies Bold StyleSpan
 * to any word starting with # or @ (one word, no spaces).
 *
 * Example:
 *   "@johndoe says #hello_world" →
 *   "@johndoe" bold, "#hello_world" bold, rest normal
 */
class HashMentionTextWatcher : TextWatcher {

    private var isFormatting = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        if (isFormatting || s == null) return
        isFormatting = true

        // Remove all existing bold spans
        val existingSpans = s.getSpans(0, s.length, StyleSpan::class.java)
        existingSpans.forEach { s.removeSpan(it) }

        // Find and bold each #word and @word
        val pattern = Regex("([#@][\\w]+)")
        pattern.findAll(s).forEach { match ->
            s.setSpan(
                StyleSpan(Typeface.BOLD),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        isFormatting = false
    }
}
