import com.github.ikun.utils.kuudra.ShopLayout
import com.github.ikun.utils.kuudra.ShopCardId
import com.github.ikun.utils.kuudra.ShopPositions
import kotlin.test.*

class ShopScaleTest {
    @Test
    fun `scale shrinks and enlarges the shop while staying centered and within the window`() {
        val small = ShopLayout(1920, 1080, 0.5)
        val normal = ShopLayout(1920, 1080)
        val large = ShopLayout(1920, 1080, 2.0)
        assertEquals(0.5, small.scale)
        assertEquals(1.0, normal.scale)
        assertEquals(2.0, large.scale)
        for (layout in listOf(small, normal, large)) {
            assertEquals(960.0, layout.left + 220 * layout.scale)
            assertEquals(540.0, layout.top + 160 * layout.scale)
        }
        val cramped = ShopLayout(320, 180, 2.0)
        val positions = ShopPositions()
        for (id in ShopCardId.entries) {
            val rect = positions.rect(id, cramped, 320, 180)
            assertTrue(cramped.left + rect.x * cramped.scale >= 0)
            assertTrue(cramped.top + rect.y * cramped.scale >= 0)
            assertTrue(cramped.left + (rect.x + rect.width) * cramped.scale <= 320)
            assertTrue(cramped.top + (rect.y + rect.height) * cramped.scale <= 180)
        }
        assertEquals(1.0, ShopLayout(1920, 1080, Double.NaN).scale)
    }

    @Test
    fun `shop and confirmation hit regions remain aligned at every slider scale`() {
        val positions = ShopPositions()
        val groups = listOf(listOf(ShopCardId.CLASS_ROUTE),
            listOf(ShopCardId.BALLISTA, ShopCardId.CANNON),
            listOf(ShopCardId.CONFIRM, ShopCardId.CANCEL))
        for (step in 1..20) {
            for ((width, height) in listOf(1920 to 1080, 854 to 480, 320 to 180)) {
                val layout = ShopLayout(width, height, step / 10.0)
                val rectanglesByGroup = groups.map { ids -> ids.map { positions.rect(it, layout, width, height) } }
                for (rectangles in rectanglesByGroup) {
                    rectangles.forEachIndexed { index, rect ->
                        val x = layout.localX(layout.left + (rect.x + rect.width / 2.0) * layout.scale)
                        val y = layout.localY(layout.top + (rect.y + rect.height / 2.0) * layout.scale)
                        assertEquals(listOf(index), rectangles.indices.filter { rectangles[it].contains(x, y) })
                    }
                }
            }
        }
    }
}
