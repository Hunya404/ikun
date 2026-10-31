package com.github.ikun.utils.kuudra


data class ShopItem(val slot: Int, val itemId: String, val name: String)

/** Layout and filtering use server slot indices, never the compacted card index. */
object ShopEntries {
    private val formatting = Regex("§.")
    private val ballistaName = Regex("Ballista Mechanic(?: (?:[IVXLCDM]+|[0-9]+))?", RegexOption.IGNORE_CASE)
    /** The class selector contains a sword and a gold chestplate in its second row. */
    fun visibleSlots(items: List<ShopItem>, containerSlots: Int): Set<Int> {
        val valid = items.filter { it.slot in 0 until containerSlots }
        val classRow = valid.filter { it.slot in 9 until 18 }
        val sword = classRow.filter { it.itemId == "minecraft:diamond_sword" }.singleOrNull()?.slot
        val classSelector = sword != null && classRow.any { it.itemId == "minecraft:golden_chestplate" }
        return valid.filter {
            if (isOpenShop(it.slot, it.name, containerSlots)) true
            else if (classSelector) it.slot == sword
            else wantedPerk(it.name)
        }.map { it.slot }.toSet()
    }

    /** This utility changes its name after purchase, but always occupies the final chest slot. */
    fun isOpenShop(slot: Int, name: String, containerSlots: Int): Boolean {
        if (containerSlots <= 0 || slot != containerSlots - 1) return false
        val title = plain(name).trim()
        return title.equals("Fill your Quiver", true) || title.equals("Open Shop", true)
    }

    private fun wantedPerk(name: String): Boolean {
        val title = plain(name).trim()
        return ballistaName.matches(title) || title.equals("Human Cannonball", true)
    }

    fun include(slot: Int, containerSlots: Int, itemId: String, name: String, lore: List<String>): Boolean {
        if (slot !in 0 until containerSlots || itemId == "minecraft:air") return false
        val title = plain(name).trim()
        if (title.isEmpty() || title.equals("Close", true)) return false
        val actionable = lore.any { plain(it).contains("click", true) || plain(it).contains("cost", true) }
        return !itemId.endsWith("glass_pane") || actionable
    }

    fun plain(text: String) = text.replace(formatting, "")

    fun summary(lore: List<String>): List<String> {
        val lines = lore.map(::plain).map(String::trim).filter(String::isNotEmpty)
        val price = lines.firstOrNull { it.contains("cost", true) || it.contains("price", true) }
        val action = lines.lastOrNull {
            it.contains("click", true) || it.contains("max", true) || it.contains("purchased", true) ||
                it.contains("unlocked", true) || it.contains("enough", true)
        }
        val progress = lines.firstOrNull { it.startsWith("Next Tier:", true) }
        return listOfNotNull(price ?: lines.firstOrNull(), action ?: progress).distinct().take(2)
    }
}

data class ShopRect(val x: Int, val y: Int, val width: Int, val height: Int) {
    fun contains(px: Double, py: Double) = px >= x && px < x + width && py >= y && py < y + height
}

class ShopLayout(screenWidth: Int, screenHeight: Int, requestedScale: Double = 1.0) {
    private val preferredScale = if (requestedScale.isFinite()) requestedScale.coerceIn(0.1, 2.0) else 1.0
    val scale = minOf(preferredScale, (screenWidth - 12).coerceAtLeast(1) / 440.0, (screenHeight - 12).coerceAtLeast(1) / 320.0)
    val left = (screenWidth - 440 * scale) / 2
    val top = (screenHeight - 320 * scale) / 2
    fun localX(x: Double) = (x - left) / scale
    fun localY(y: Double) = (y - top) / scale
}
