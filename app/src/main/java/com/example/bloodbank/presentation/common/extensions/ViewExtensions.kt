package com.example.bloodbank.presentation.common.extensions

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout

// ── View visibility helpers ────────────────────────────────────────────────────

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

// ── Snackbar helpers ───────────────────────────────────────────────────────────

fun View.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_LONG) {
    Snackbar.make(this, message, duration).show()
}

fun View.showErrorSnackbar(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_LONG)
        .setBackgroundTint(resources.getColor(android.R.color.holo_red_dark, null))
        .setTextColor(resources.getColor(android.R.color.white, null))
        .show()
}

fun View.showSuccessSnackbar(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_LONG)
        .setBackgroundTint(resources.getColor(android.R.color.holo_green_dark, null))
        .setTextColor(resources.getColor(android.R.color.white, null))
        .show()
}

// ── TextInputLayout helpers ────────────────────────────────────────────────────

fun TextInputLayout.setError(message: String?) {
    error = message
    isErrorEnabled = message != null
}

fun TextInputLayout.clearError() {
    error = null
    isErrorEnabled = false
}

val TextInputLayout.text: String
    get() = editText?.text?.toString()?.trim() ?: ""

// ── Fragment helpers ───────────────────────────────────────────────────────────

fun Fragment.hideKeyboard() {
    view?.let { v ->
        val imm = requireContext().getSystemService<InputMethodManager>()
        imm?.hideSoftInputFromWindow(v.windowToken, 0)
    }
}
