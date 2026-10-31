package com.github.ikun.utils.kuudra

object ShopKeyBindings {
    /** Bindings follow card identity; a missing or ambiguous card never redirects a key. */
    fun indexForKey(ids: List<ShopCardId>, key: Int, binding: (ShopCardId) -> Int): Int? {
        if (key < 0) return null
        return ids.indices.singleOrNull { binding(ids[it]) == key }
    }
}
