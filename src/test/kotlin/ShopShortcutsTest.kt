import com.github.ikun.utils.kuudra.ShopCardId
import kotlin.test.*

class ShopShortcutsTest {
    @Test
    fun `a purchased Ballista disappearing never renumbers Cannon or redirects the Ballista key`() {
        val beforePurchase = listOf(ShopCardId.BALLISTA, ShopCardId.CANNON)
        val whileUpdating = listOf(ShopCardId.CANNON)
        val afterUpdate = listOf(ShopCardId.BALLISTA, ShopCardId.CANNON)

        assertEquals(listOf(1, 2), beforePurchase.map { it.shortcut })
        assertEquals(0, ShopCardId.indexForShortcut(beforePurchase, 1))
        assertEquals(1, ShopCardId.indexForShortcut(beforePurchase, 2))
        assertEquals(listOf(2), whileUpdating.map { it.shortcut })
        assertNull(ShopCardId.indexForShortcut(whileUpdating, 1))
        assertEquals(0, ShopCardId.indexForShortcut(whileUpdating, 2))
        assertEquals(listOf(1, 2), afterUpdate.map { it.shortcut })
        assertEquals(0, ShopCardId.indexForShortcut(afterUpdate, 1))
        assertEquals(1, ShopCardId.indexForShortcut(afterUpdate, 2))
    }

    @Test
    fun `Cannon disappearing leaves key two inactive while Ballista stays on key one`() {
        val remaining = listOf(ShopCardId.BALLISTA)
        assertEquals(1, remaining.single().shortcut)
        assertEquals(0, ShopCardId.indexForShortcut(remaining, 1))
        assertNull(ShopCardId.indexForShortcut(remaining, 2))
    }

    @Test
    fun `server reordering changes target indices while rendered card numbers keep their meaning`() {
        for (visible in listOf(listOf(ShopCardId.BALLISTA, ShopCardId.CANNON),
            listOf(ShopCardId.CANNON, ShopCardId.BALLISTA))) {
            val ballistaIndex = assertNotNull(ShopCardId.indexForShortcut(visible, 1))
            val cannonIndex = assertNotNull(ShopCardId.indexForShortcut(visible, 2))
            assertEquals(ShopCardId.BALLISTA, visible[ballistaIndex])
            assertEquals(ShopCardId.CANNON, visible[cannonIndex])
            visible.forEachIndexed { index, id ->
                assertEquals(index, ShopCardId.indexForShortcut(visible, assertNotNull(id.shortcut)))
            }
        }
    }

    @Test
    fun `class entrance uses key one and confirmation buttons default to keys one and two`() {
        val entrance = listOf(ShopCardId.CLASS_ROUTE)
        assertEquals(1, entrance.single().shortcut)
        assertEquals(0, ShopCardId.indexForShortcut(entrance, 1))
        assertNull(ShopCardId.indexForShortcut(entrance, 2))

        val confirmation = listOf(ShopCardId.CONFIRM, ShopCardId.CANCEL)
        assertEquals(listOf(1, 2), confirmation.map { it.shortcut })
        assertEquals(0, ShopCardId.indexForShortcut(confirmation, 1))
        assertEquals(1, ShopCardId.indexForShortcut(confirmation, 2))
        assertEquals(0, ShopCardId.indexForShortcut(listOf(ShopCardId.CONFIRM), 1))
        assertNull(ShopCardId.indexForShortcut(listOf(ShopCardId.CONFIRM), 2))
        for (number in listOf(0, 3, 4, 5, 6, 7, 8, 9)) {
            assertNull(ShopCardId.indexForShortcut(confirmation, number))
        }
    }

    @Test
    fun `duplicate cards or conflicting entrance and perk keys cannot choose an arbitrary purchase`() {
        assertNull(ShopCardId.indexForShortcut(listOf(ShopCardId.BALLISTA, ShopCardId.BALLISTA), 1))
        assertNull(ShopCardId.indexForShortcut(listOf(ShopCardId.CANNON, ShopCardId.CANNON), 2))
        val conflicting = listOf(ShopCardId.CLASS_ROUTE, ShopCardId.BALLISTA, ShopCardId.CANNON)
        assertNull(ShopCardId.indexForShortcut(conflicting, 1))
        assertEquals(2, ShopCardId.indexForShortcut(conflicting, 2))
    }

    @Test
    fun `unsupported numbers and an empty shop have no targets`() {
        val perks = listOf(ShopCardId.BALLISTA, ShopCardId.CANNON)
        for (number in listOf(Int.MIN_VALUE, -1, 0, 3, 6, 9, Int.MAX_VALUE)) {
            assertNull(ShopCardId.indexForShortcut(perks, number))
        }
        assertNull(ShopCardId.indexForShortcut(emptyList(), 1))
        assertNull(ShopCardId.indexForShortcut(emptyList(), 2))
    }
}
