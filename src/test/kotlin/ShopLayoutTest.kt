import com.github.ikun.utils.kuudra.ShopEntries
import com.github.ikun.utils.kuudra.ShopLayout
import com.github.ikun.utils.kuudra.ShopItem
import com.github.ikun.utils.kuudra.ShopCardId
import com.github.ikun.utils.kuudra.ShopPositions
import kotlin.test.*

class ShopLayoutTest {
    @Test
    fun `last slot quiver entrance stays visible before and after purchase on both shop pages`() {
        val classSelector = listOf(ShopItem(12, "minecraft:golden_chestplate", "Tank Route"),
            ShopItem(14, "minecraft:diamond_sword", "Specialist Route"),
            ShopItem(30, "minecraft:player_head", "Human Cannonball"))
        val perks = listOf(ShopItem(13, "minecraft:crafting_table", "Ballista Mechanic II"),
            ShopItem(25, "minecraft:player_head", "Human Cannonball"))
        for (title in listOf("§aFill your Quiver", "§aOpen Shop")) {
            val entrance = ShopItem(53, "minecraft:player_head", title)
            assertEquals(setOf(14, 53), ShopEntries.visibleSlots(classSelector + entrance, 54))
            assertEquals(setOf(13, 25, 53), ShopEntries.visibleSlots(perks + entrance, 54))
            assertEquals(setOf(13, 25), ShopEntries.visibleSlots(perks + entrance.copy(slot = 52), 54))
            assertEquals(setOf(13, 25), ShopEntries.visibleSlots(perks + entrance.copy(slot = 54), 54))
        }
        assertFalse(ShopEntries.isOpenShop(53, "Kuudra", 54))
        assertFalse(ShopEntries.isOpenShop(-1, "Open Shop", 0))
    }

    @Test
    fun `class selector shows only the sword and hides even the wanted perks until class selection`() {
        val screenshot = listOf(ShopItem(10, "minecraft:player_head", "Cannoneer Route"),
            ShopItem(12, "minecraft:golden_chestplate", "Tank Route"),
            ShopItem(14, "minecraft:diamond_sword", "§aSpecialist Route"),
            ShopItem(16, "minecraft:player_head", "Support Route"),
            ShopItem(29, "minecraft:player_head", "Auto Revive"),
            ShopItem(30, "minecraft:player_head", "Human Cannonball"),
            ShopItem(31, "minecraft:crafting_table", "Remote Perk Shop"),
            ShopItem(32, "minecraft:fishing_rod", "Elle's Lava Rod"),
            ShopItem(33, "minecraft:iron_pickaxe", "Elle's Pickaxe"),
            ShopItem(49, "minecraft:barrier", "Close"),
            ShopItem(53, "minecraft:player_head", "Kuudra"),
            ShopItem(54, "minecraft:diamond_sword", "Player's sword"))
        assertEquals(setOf(14), ShopEntries.visibleSlots(screenshot, 54))
    }

    @Test
    fun `perk shop includes Ballista and Cannon while excluding Auto Revive from the server screenshot`() {
        val screenshot = listOf(ShopItem(12, "minecraft:chest", "Steady Hands I"),
            ShopItem(13, "minecraft:crafting_table", "§aBallista Mechanic I"),
            ShopItem(14, "minecraft:potion", "Rapid Fire I"),
            ShopItem(16, "minecraft:player_head", "§aAuto Revive"),
            ShopItem(22, "minecraft:diamond_pickaxe", "Mining Frenzy I"),
            ShopItem(25, "minecraft:player_head", "§aHuman Cannonball"),
            ShopItem(34, "minecraft:fishing_rod", "Elle's Lava Rod"),
            ShopItem(43, "minecraft:iron_pickaxe", "Elle's Pickaxe"),
            ShopItem(49, "minecraft:arrow", "Go Back"),
            ShopItem(53, "minecraft:player_head", "Kuudra"),
            ShopItem(54, "minecraft:crafting_table", "Ballista Mechanic III"))
        val visible = ShopEntries.visibleSlots(screenshot, 54)
        assertEquals(setOf(13, 25), visible)
        assertFalse(16 in visible, "Auto Revive must not become a shop card")
    }

    @Test
    fun `perk names survive tier changes and slot moves without accepting unrelated worktables or heads`() {
        for (tier in listOf("", " I", " II", " III", " IV", " X", " 5")) {
            val moved = listOf(ShopItem(31, "minecraft:crafting_table", "§aBallista Mechanic$tier"),
                ShopItem(8, "minecraft:player_head", "Auto Revive"),
                ShopItem(50, "minecraft:player_head", "Human Cannonball"),
                ShopItem(13, "minecraft:crafting_table", "Other building perk"),
                ShopItem(16, "minecraft:player_head", "Other head"))
            assertEquals(setOf(31, 50), ShopEntries.visibleSlots(moved, 54))
        }
        assertTrue(ShopEntries.visibleSlots(listOf(ShopItem(12, "minecraft:golden_chestplate", "Other perk"),
            ShopItem(29, "minecraft:diamond_sword", "Sword upgrade")), 54).isEmpty())
    }

    @Test
    fun `server products survive filtering while player inventory and decoration never become cards`() {
        val count = 54
        val slots = listOf(0, 10, 23, 53, 54, 80).filter {
            ShopEntries.include(it, count, "minecraft:fire_charge", "Human Cannonball", listOf("Cost: 2,000 Tokens", "Click to purchase!"))
        }
        assertEquals(listOf(0, 10, 23, 53), slots)
        assertFalse(ShopEntries.include(1, count, "minecraft:air", "Something", emptyList()))
        assertFalse(ShopEntries.include(1, count, "minecraft:black_stained_glass_pane", "§8 ", emptyList()))
        assertFalse(ShopEntries.include(1, count, "minecraft:black_stained_glass_pane", "Decoration", emptyList()))
        assertFalse(ShopEntries.include(1, count, "minecraft:barrier", "§cClose", emptyList()))
        assertTrue(ShopEntries.include(1, count, "minecraft:green_stained_glass_pane", "Upgrade", listOf("Click to upgrade!")))
        assertTrue(ShopEntries.include(1, count, "minecraft:blaze_rod", "Unknown future perk", emptyList()))
    }

    @Test
    fun `scaled mouse coordinates target only the remaining visible cards and not empty space`() {
        val positions = ShopPositions()
        val groups = listOf(listOf(ShopCardId.CLASS_ROUTE, ShopCardId.OPEN_SHOP),
            listOf(ShopCardId.BALLISTA, ShopCardId.CANNON, ShopCardId.OPEN_SHOP),
            listOf(ShopCardId.CONFIRM, ShopCardId.CANCEL))
        for ((width, height) in listOf(1920 to 1080, 854 to 480, 320 to 180, 200 to 120)) {
            val layout = ShopLayout(width, height)
            for (group in groups) {
                val cards = group.map { positions.rect(it, layout, width, height) }
                cards.forEachIndexed { index, card ->
                    val x = layout.localX(layout.left + (card.x + card.width / 2.0) * layout.scale)
                    val y = layout.localY(layout.top + (card.y + card.height / 2.0) * layout.scale)
                    assertEquals(listOf(index), cards.indices.filter { cards[it].contains(x, y) })
                    assertFalse(card.contains((card.x + card.width).toDouble(), y))
                }
                assertTrue(cards.none { it.contains(60.0, 295.0) })
            }
            assertTrue(groups[1].none { positions.rect(it, layout, width, height).contains(220.0, 80.0) })
            assertTrue(groups[2].none { positions.rect(it, layout, width, height).contains(220.0, 160.0) })
        }
    }

    @Test
    fun `summaries prefer server cost and purchase status without inventing a price`() {
        assertEquals(listOf("Cost: 2,000 Tokens", "Click to purchase!"), ShopEntries.summary(listOf("§7Ability description", "", "§6Cost: 2,000 Tokens", "§eClick to purchase!")))
        assertEquals(listOf("MAXED OUT!"), ShopEntries.summary(listOf("§cMAXED OUT!")))
        assertEquals(emptyList(), ShopEntries.summary(emptyList()))
        assertEquals(listOf("Cost: 80 Tokens", "Next Tier: +10% building speed"), ShopEntries.summary(listOf(
            "Increase how fast you build Elle's Ballista.", "Current Tier: +0% building speed",
            "Next Tier: +10% building speed", "Cost: 80 Tokens", "Preview Mode")))
    }
}
