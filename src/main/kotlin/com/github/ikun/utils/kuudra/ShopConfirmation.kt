package com.github.ikun.utils.kuudra

/** Only a recent action in our Kuudra shop can claim a generic confirmation menu. */
class ShopConfirmationFlow {
    private var selectedAt: Long? = null

    fun selected(now: Long) { selectedAt = now }
    fun pending(now: Long): Boolean {
        val start = selectedAt ?: return false
        return (now - start) in 0 until 10_000
    }
    fun observe(title: String?, now: Long) {
        if (!pending(now) || title != null && title != "Perk Menu" && title != "Are you sure?") reset()
    }
    fun reset() { selectedAt = null }
}

data class ShopConfirmActions(val confirm: Int, val cancel: Int) {
    companion object {
        /** Names, not clay colours or fixed slot numbers, define the server actions. */
        fun find(names: List<Pair<Int, String>>, containerSlots: Int): ShopConfirmActions? {
            val valid = names.filter { it.first in 0 until containerSlots }
            val confirm = valid.filter { ShopEntries.plain(it.second).trim().equals("Confirm", true) }.singleOrNull()?.first ?: return null
            val cancel = valid.filter { ShopEntries.plain(it.second).trim().lowercase() in setOf("cancel", "deny", "decline") }.singleOrNull()?.first ?: return null
            return ShopConfirmActions(confirm, cancel)
        }
    }
}
