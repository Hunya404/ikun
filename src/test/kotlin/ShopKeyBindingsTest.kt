import com.github.ikun.utils.kuudra.ShopCardId
import com.github.ikun.utils.kuudra.ShopKeyBindings
import kotlin.test.*

class ShopKeyBindingsTest {
    private val custom = mapOf(ShopCardId.CLASS_ROUTE to 71, ShopCardId.BALLISTA to 66,
        ShopCardId.CANNON to 67, ShopCardId.CONFIRM to 89, ShopCardId.CANCEL to 78)
    private fun binding(id: ShopCardId): Int = custom.getValue(id)

    @Test
    fun `custom keys select the matching visible card including confirmation buttons`() {
        assertEquals(0, ShopKeyBindings.indexForKey(listOf(ShopCardId.CLASS_ROUTE), 71, ::binding))
        assertNull(ShopKeyBindings.indexForKey(listOf(ShopCardId.CLASS_ROUTE), 66, ::binding))
        val perks = listOf(ShopCardId.BALLISTA, ShopCardId.CANNON)
        assertEquals(0, ShopKeyBindings.indexForKey(perks, 66, ::binding))
        assertEquals(1, ShopKeyBindings.indexForKey(perks, 67, ::binding))
        val confirmation = listOf(ShopCardId.CONFIRM, ShopCardId.CANCEL)
        assertEquals(0, ShopKeyBindings.indexForKey(confirmation, 89, ::binding))
        assertEquals(1, ShopKeyBindings.indexForKey(confirmation, 78, ::binding))
        assertNull(ShopKeyBindings.indexForKey(confirmation, 66, ::binding))
    }

    @Test
    fun `a card disappearing cannot transfer its custom key to the remaining card`() {
        val full = listOf(ShopCardId.BALLISTA, ShopCardId.CANNON)
        val updating = listOf(ShopCardId.CANNON)
        assertEquals(0, ShopKeyBindings.indexForKey(full, 66, ::binding))
        assertEquals(1, ShopKeyBindings.indexForKey(full, 67, ::binding))
        assertNull(ShopKeyBindings.indexForKey(updating, 66, ::binding))
        assertEquals(0, ShopKeyBindings.indexForKey(updating, 67, ::binding))
        assertEquals(0, ShopKeyBindings.indexForKey(full, 66, ::binding))
        assertEquals(1, ShopKeyBindings.indexForKey(full, 67, ::binding))
    }

    @Test
    fun `server card order changes the index while keeping custom binding identity`() {
        for (visible in listOf(listOf(ShopCardId.BALLISTA, ShopCardId.CANNON),
            listOf(ShopCardId.CANNON, ShopCardId.BALLISTA),
            listOf(ShopCardId.CANCEL, ShopCardId.CONFIRM))) {
            visible.forEachIndexed { index, id ->
                val target = assertNotNull(ShopKeyBindings.indexForKey(visible, binding(id), ::binding))
                assertEquals(index, target)
                assertEquals(id, visible[target])
            }
        }
    }

    @Test
    fun `duplicate or conflicting visible targets reject the key instead of choosing a purchase`() {
        assertNull(ShopKeyBindings.indexForKey(listOf(ShopCardId.BALLISTA, ShopCardId.BALLISTA), 66, ::binding))
        val perks = listOf(ShopCardId.BALLISTA, ShopCardId.CANNON)
        assertNull(ShopKeyBindings.indexForKey(perks, 80) { 80 })
        val confirmation = listOf(ShopCardId.CONFIRM, ShopCardId.CANCEL)
        assertNull(ShopKeyBindings.indexForKey(confirmation, 89) { 89 })
        val classAndPerk = listOf(ShopCardId.CLASS_ROUTE, ShopCardId.BALLISTA, ShopCardId.CANNON)
        assertNull(ShopKeyBindings.indexForKey(classAndPerk, 49) {
            if (it == ShopCardId.CANNON) 50 else 49
        })
        assertEquals(2, ShopKeyBindings.indexForKey(classAndPerk, 50) {
            if (it == ShopCardId.CANNON) 50 else 49
        })
    }

    @Test
    fun `missing keys unbound cards and empty menus have no targets`() {
        val perks = listOf(ShopCardId.BALLISTA, ShopCardId.CANNON)
        assertNull(ShopKeyBindings.indexForKey(perks, 81, ::binding))
        assertNull(ShopKeyBindings.indexForKey(perks, -1) { -1 })
        assertNull(ShopKeyBindings.indexForKey(perks, Int.MIN_VALUE) { Int.MIN_VALUE })
        assertNull(ShopKeyBindings.indexForKey(perks, 66) { -1 })
        assertNull(ShopKeyBindings.indexForKey(emptyList(), 66, ::binding))
        assertEquals(1, ShopKeyBindings.indexForKey(perks, 67) {
            if (it == ShopCardId.BALLISTA) -1 else 67
        })
    }
}
