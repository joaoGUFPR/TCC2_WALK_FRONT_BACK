// MaskUtil.kt
package com.example.prototipopasseios.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

object MaskUtil {
    fun insert(mask: String, ediTxt: EditText): TextWatcher {
        return object : TextWatcher {
            var isUpdating = false
            var old = ""
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val str = unmask(s.toString())
                var formatted = ""
                if (isUpdating) {
                    old = str
                    isUpdating = false
                    return
                }
                var i = 0
                for (m in mask.toCharArray()) {
                    if (m != '#' && str.length > old.length) {
                        formatted += m
                        continue
                    }
                    try {
                        formatted += str[i]
                    } catch (e: Exception) { break }
                    i++
                }
                isUpdating = true
                ediTxt.setText(formatted)
                ediTxt.setSelection(formatted.length)
            }
            override fun afterTextChanged(s: Editable) {}
            private fun unmask(s: String) = s.replace("[^\\d]".toRegex(), "")
        }
    }

    fun enforcePrefix(prefix: String, ediTxt: EditText): TextWatcher {
        return object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (isUpdating) return

                val text = s.toString()
                if (!text.startsWith(prefix)) {
                    isUpdating = true
                    // remove ocorrências extras e reescreve com prefixo
                    val bare = text.replace(prefix, "")
                    val updated = prefix + bare
                    ediTxt.setText(updated)
                    ediTxt.setSelection(updated.length)
                    isUpdating = false
                }
            }
            override fun afterTextChanged(s: Editable) {}
        }
    }
}
