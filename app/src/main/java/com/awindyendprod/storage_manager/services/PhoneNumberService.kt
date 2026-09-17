package com.awindyendprod.storage_manager.services

import java.net.URLEncoder

object PhoneNumberService {
    fun detectPhoneNumber(clientName: String, note: String): String? {
        if (looksLikePhoneNumber(clientName)) return clientName
        if (looksLikePhoneNumber(note)) return note
        return null
    }

    fun looksLikePhoneNumber(raw: String): Boolean {
        val digitCount = cleanDigits(raw).removePrefix("+").length
        return digitCount in 7..15
    }

    fun buildWhatsAppUrl(raw: String, text: String? = null): String {
        val base = "https://wa.me/${toE164Digits(raw)}"
        val body = text?.takeIf { it.isNotBlank() } ?: return base
        // wa.me wants %20 for spaces; URLEncoder emits form encoding, which uses "+".
        return "$base?text=${URLEncoder.encode(body, "UTF-8").replace("+", "%20")}"
    }

    fun buildTelUri(raw: String) = "tel:${cleanDigits(raw)}"

    private fun cleanDigits(raw: String): String =
        raw.filterIndexed { i, c -> c.isDigit() || (i == 0 && c == '+') }

    private fun toE164Digits(raw: String): String {
        val cleaned = cleanDigits(raw)
        return when {
            cleaned.startsWith("+") -> cleaned.removePrefix("+")
            cleaned.startsWith("972") -> cleaned
            cleaned.startsWith("0") -> "972" + cleaned.removePrefix("0")
            else -> "972$cleaned"
        }
    }
}
