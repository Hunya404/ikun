import com.github.ikun.utils.kuudra.ShopConfirmationFlow
import com.github.ikun.utils.kuudra.ShopConfirmActions
import com.github.ikun.utils.kuudra.ShopLayout
import com.github.ikun.utils.kuudra.ShopCardId
import com.github.ikun.utils.kuudra.ShopPositions
import kotlin.test.*

class ShopConfirmationTest {
    @Test
    fun `confirmation must follow a recent shop action and expires`() {
        val flow = ShopConfirmationFlow()
        assertFalse(flow.pending(1_000))
        flow.selected(1_000)
        flow.observe("Are you sure?", 1_100)
        assertTrue(flow.pending(1_100))
        assertTrue(flow.pending(10_999))
        flow.observe("Are you sure?", 11_000)
        assertFalse(flow.pending(11_000))
    }

    @Test
    fun `unrelated menus and closing the flow clear confirmation eligibility`() {
        val flow = ShopConfirmationFlow()
        flow.selected(1_000)
        flow.observe("Perk Menu", 1_100)
        flow.observe(null, 1_200)
        assertTrue(flow.pending(1_200))
        flow.observe("Bazaar", 1_300)
        flow.observe("Are you sure?", 1_400)
        assertFalse(flow.pending(1_400))
        flow.selected(2_000)
        flow.reset()
        assertFalse(flow.pending(2_100))
    }

    @Test
    fun `confirmation names map to real server slots regardless of order and decoration`() {
        val screenshot = listOf(13 to "Are you sure?", 20 to "§aConfirm", 24 to "§cCancel", 31 to "Go Back")
        assertEquals(ShopConfirmActions(20, 24), ShopConfirmActions.find(screenshot, 36))
        assertEquals(ShopConfirmActions(8, 2), ShopConfirmActions.find(listOf(2 to "Cancel", 8 to "Confirm"), 36))
        assertNull(ShopConfirmActions.find(listOf(20 to "Confirm", 40 to "Cancel"), 36))
        assertNull(ShopConfirmActions.find(listOf(20 to "Confirm", 24 to "Cancel", 25 to "Confirm"), 36))
        assertNull(ShopConfirmActions.find(listOf(20 to "Confirm purchase", 24 to "Cancel"), 36))
        assertNull(ShopConfirmActions.find(listOf(20 to "Confirm"), 36))
    }

    @Test
    fun `two compact confirmation buttons have distinct scaled hit regions`() {
        val layout = ShopLayout(320, 180)
        val positions = ShopPositions()
        val buttons = listOf(ShopCardId.CONFIRM, ShopCardId.CANCEL).map { positions.rect(it, layout, 320, 180) }
        buttons.forEachIndexed { index, card ->
            val x = layout.localX(layout.left + (card.x + card.width / 2.0) * layout.scale)
            val y = layout.localY(layout.top + (card.y + card.height / 2.0) * layout.scale)
            assertTrue(card.contains(x, y))
            assertFalse(buttons[1 - index].contains(x, y))
        }
        assertTrue(buttons.none { it.contains(220.0, 160.0) })
        assertTrue(buttons.none { it.contains(60.0, 295.0) })
    }
}
