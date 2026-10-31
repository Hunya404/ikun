// SPDX-License-Identifier: GPL-3.0-only
// Toxic trigger adapted from RatTokens' RatAddons 2.0.1 KittyKuudraBasics.handleChat.
package com.github.ikun.utils.kuudra

class AutoGfsTrigger {
    fun command(message: String, inKuudra: Boolean, toxicAmount: Int = 16): String? {
        if (!inKuudra || !isCue(message)) return null
        return "gfs toxic_arrow_poison ${toxicAmount.coerceIn(1, 32)}"
    }

    companion object {
        const val CUE = "The Ballista is finally ready!"
        private val formatting = Regex("§[0-9a-fk-or]", RegexOption.IGNORE_CASE)
        private val whitespace = Regex("\\s+")
        private val area = Regex("^(?:⏣\\s*)?Kuudra['’]s Hollow(?:\\s*\\(T[1-5]\\))?$")
        private val tabArea = Regex("^Area:\\s*Kuudra(?:['’]s Hollow(?:\\s*\\(T[1-5]\\))?)?$")

        private fun normalize(text: String) = text.replace(formatting, "").replace(whitespace, " ").trim()

        // RatAddons uses a case-sensitive substring, with no once-per-world latch.
        fun isCue(text: String) = text.replace(formatting, "").contains(CUE)
        fun isKuudraSidebarLine(text: String) = area.matches(normalize(text))
        fun isKuudraTabLine(text: String) = tabArea.matches(normalize(text))
    }
}
