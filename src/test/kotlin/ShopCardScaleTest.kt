import com.github.ikun.utils.kuudra.ShopCardId
import com.github.ikun.utils.kuudra.ShopLayout
import com.github.ikun.utils.kuudra.ShopPositions
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlin.test.*

class ShopCardScaleTest {
    @Test
    fun `each card can resize without changing other card sizes or stable shortcuts`() {
        val positions = ShopPositions()
        val layout = ShopLayout(1920, 1080)
        val untouched = positions.placement(ShopCardId.CANNON, layout, 1920, 1080)
        positions.setScale(ShopCardId.BALLISTA, 1.7)
        val ballista = positions.placement(ShopCardId.BALLISTA, layout, 1920, 1080)
        assertEquals(1.7, positions.scale(ShopCardId.BALLISTA))
        assertEquals(1.0, positions.scale(ShopCardId.CANNON))
        assertEquals(untouched, positions.placement(ShopCardId.CANNON, layout, 1920, 1080))
        assertEquals(ShopCardId.BALLISTA.defaultRect.width * 1.7, ballista.width)
        assertEquals(ShopCardId.BALLISTA.defaultRect.height * 1.7, ballista.height)
        assertEquals(0, ShopCardId.indexForShortcut(listOf(ShopCardId.BALLISTA, ShopCardId.CANNON), 1))
        assertEquals(1, ShopCardId.indexForShortcut(listOf(ShopCardId.BALLISTA, ShopCardId.CANNON), 2))
    }

    @Test
    fun `resizing keeps default and saved card centers in place`() {
        val layout = ShopLayout(1920, 1080)
        for (saved in listOf(false, true)) {
            val positions = ShopPositions()
            if (saved) positions.move(ShopCardId.BALLISTA, 498.0, 316.0, layout, 1920, 1080)
            val before = positions.placement(ShopCardId.BALLISTA, layout, 1920, 1080)
            for (scale in listOf(0.1, 0.35, 1.0, 1.5, 2.0)) {
                positions.setScale(ShopCardId.BALLISTA, scale)
                val after = positions.placement(ShopCardId.BALLISTA, layout, 1920, 1080)
                assertEquals(before.x + before.width / 2, after.x + after.width / 2, 1e-9)
                assertEquals(before.y + before.height / 2, after.y + after.height / 2, 1e-9)
                assertEquals(scale, after.scale)
            }
        }
    }

    @Test
    fun `unscaled placement preserves old rectangle coordinates for default and dragged cards`() {
        val positions = ShopPositions()
        for ((width, height) in listOf(1920 to 1080, 854 to 480, 320 to 180)) {
            val layout = ShopLayout(width, height, 1.3)
            for (id in ShopCardId.entries) {
                for (dragged in listOf(false, true)) {
                    if (dragged) positions.move(id, width * 0.3, height * 0.4, layout, width, height)
                    val rect = positions.rect(id, layout, width, height)
                    val placement = positions.placement(id, layout, width, height)
                    assertEquals(rect.x.toDouble(), placement.x)
                    assertEquals(rect.y.toDouble(), placement.y)
                    assertEquals(rect.width.toDouble(), placement.width)
                    assertEquals(rect.height.toDouble(), placement.height)
                    positions.reset(id)
                }
            }
        }
    }

    @Test
    fun `old positions and scale only entries round trip while malformed fields fail independently`() {
        val positions = ShopPositions().apply {
            read(JsonParser.parseString("""{
                "class_route": {"x": 0.4, "y": 0.2},
                "ballista": {"scale": 1.35},
                "cannon": {"x": "broken", "y": 0.6, "scale": 0.75},
                "confirm": {"x": 0.3, "y": 0.8, "scale": 2.5},
                "cancel": {"scale": "0.5"},
                "unknown": {"x": 0.5, "y": 0.5, "scale": 2.0}
            }"""))
        }
        assertEquals(1.0, positions.scale(ShopCardId.CLASS_ROUTE))
        assertEquals(1.35, positions.scale(ShopCardId.BALLISTA))
        assertEquals(0.75, positions.scale(ShopCardId.CANNON))
        assertEquals(1.0, positions.scale(ShopCardId.CONFIRM))
        assertEquals(1.0, positions.scale(ShopCardId.CANCEL))
        val saved = positions.write()
        assertEquals(setOf("class_route", "ballista", "cannon", "confirm"), saved.keySet())
        assertEquals(setOf("x", "y"), saved.getAsJsonObject("class_route").keySet())
        assertEquals(setOf("scale"), saved.getAsJsonObject("ballista").keySet())
        assertEquals(setOf("scale"), saved.getAsJsonObject("cannon").keySet())
        assertEquals(setOf("x", "y"), saved.getAsJsonObject("confirm").keySet())
        val restored = ShopPositions().apply { read(saved) }
        assertEquals(saved, restored.write())
        val layout = ShopLayout(1920, 1080)
        for (id in ShopCardId.entries) {
            assertEquals(positions.placement(id, layout, 1920, 1080), restored.placement(id, layout, 1920, 1080))
        }
        positions.read(JsonObject().apply {
            add("ballista", JsonObject().apply {
                addProperty("x", 0.6)
                addProperty("y", 0.5)
                addProperty("scale", Double.POSITIVE_INFINITY)
            })
            add("cannon", JsonObject().apply {
                addProperty("x", Double.NaN)
                addProperty("y", 0.5)
                addProperty("scale", 1.6)
            })
        })
        assertEquals(1.35, positions.scale(ShopCardId.BALLISTA))
        assertEquals(0.6, positions.write().getAsJsonObject("ballista").get("x").asDouble)
        assertEquals(1.6, positions.scale(ShopCardId.CANNON))
        assertFalse(positions.write().getAsJsonObject("cannon").has("x"))
    }

    @Test
    fun `dragging scaled cards uses their actual dimensions and aligned half open hitboxes`() {
        for (globalScale in listOf(0.5, 1.0, 2.0)) {
            val layout = ShopLayout(1920, 1080, globalScale)
            for (id in ShopCardId.entries) {
                val positions = ShopPositions()
                positions.setScale(id, 1.25)
                positions.move(id, 50.25, 100.5, layout, 1920, 1080)
                val card = positions.placement(id, layout, 1920, 1080)
                assertEquals(50.25, layout.left + card.x * layout.scale, 1e-9)
                assertEquals(100.5, layout.top + card.y * layout.scale, 1e-9)
                val screenCenterX = 50.25 + card.width * layout.scale / 2
                val screenCenterY = 100.5 + card.height * layout.scale / 2
                assertTrue(card.contains(layout.localX(screenCenterX), layout.localY(screenCenterY)))
                assertTrue(card.contains(card.x, card.y))
                assertFalse(card.contains(card.x + card.width, card.y + card.height / 2))
                assertFalse(card.contains(card.x + card.width / 2, card.y + card.height))
                assertFalse(card.contains(card.x - 0.001, card.y))
            }
        }
    }

    @Test
    fun `scaled cards clamp at all screen edges without writing rendering limits into saved settings`() {
        for ((width, height) in listOf(1920 to 1080, 854 to 480, 320 to 180, 80 to 40)) {
            val layout = ShopLayout(width, height, 2.0)
            for (id in ShopCardId.entries) {
                for (edge in listOf(-10_000.0, 10_000.0)) {
                    val positions = ShopPositions()
                    positions.setScale(id, 2.0)
                    positions.move(id, edge, edge, layout, width, height)
                    val saved = positions.write()
                    val revision = positions.revision
                    val card = positions.placement(id, layout, width, height)
                    val left = layout.left + card.x * layout.scale
                    val top = layout.top + card.y * layout.scale
                    assertTrue(left >= -1e-9 && top >= -1e-9)
                    assertTrue(left + card.width * layout.scale <= width + 1e-9)
                    assertTrue(top + card.height * layout.scale <= height + 1e-9)
                    assertEquals(id.defaultRect.width * card.scale, card.width)
                    assertEquals(id.defaultRect.height * card.scale, card.height)
                    assertEquals(2.0, positions.scale(id))
                    assertEquals(saved, positions.write())
                    assertEquals(revision, positions.revision)
                }
            }
        }
    }

    @Test
    fun `a card also fits a smaller viewport during a window resize without losing requested scale`() {
        val positions = ShopPositions()
        val previousLayout = ShopLayout(1920, 1080, 2.0)
        positions.setScale(ShopCardId.BALLISTA, 2.0)
        val saved = positions.write()
        val fitted = positions.placement(ShopCardId.BALLISTA, previousLayout, 32, 10)
        assertTrue(fitted.scale < 0.1)
        assertEquals(ShopCardId.BALLISTA.defaultRect.width * fitted.scale, fitted.width)
        assertEquals(ShopCardId.BALLISTA.defaultRect.height * fitted.scale, fitted.height)
        val left = previousLayout.left + fitted.x * previousLayout.scale
        val top = previousLayout.top + fitted.y * previousLayout.scale
        assertTrue(left >= -1e-9 && top >= -1e-9)
        assertTrue(left + fitted.width * previousLayout.scale <= 32 + 1e-9)
        assertTrue(top + fitted.height * previousLayout.scale <= 10 + 1e-9)
        assertEquals(saved, positions.write())
        assertEquals(2.0, positions.scale(ShopCardId.BALLISTA))
        assertEquals(2.0, positions.placement(ShopCardId.BALLISTA, previousLayout, 1920, 1080).scale)
        positions.move(ShopCardId.BALLISTA, 500.0, 500.0, previousLayout, 32, 10)
        val dragged = positions.placement(ShopCardId.BALLISTA, previousLayout, 32, 10)
        assertEquals(32.0, previousLayout.left + (dragged.x + dragged.width) * previousLayout.scale, 1e-9)
        assertEquals(10.0, previousLayout.top + (dragged.y + dragged.height) * previousLayout.scale, 1e-9)
    }

    @Test
    fun `scale reset keeps positions and full resets clear both with revision only on changes`() {
        val positions = ShopPositions()
        val layout = ShopLayout(1920, 1080)
        positions.setScale(ShopCardId.BALLISTA, Double.NaN)
        positions.setScale(ShopCardId.BALLISTA, Double.POSITIVE_INFINITY)
        positions.setScale(ShopCardId.BALLISTA, 1.0)
        assertEquals(0, positions.revision)
        positions.setScale(ShopCardId.BALLISTA, 50.0)
        assertEquals(2.0, positions.scale(ShopCardId.BALLISTA))
        val firstChange = positions.revision
        positions.setScale(ShopCardId.BALLISTA, 3.0)
        assertEquals(firstChange, positions.revision)
        positions.setScale(ShopCardId.CANNON, -5.0)
        assertEquals(0.1, positions.scale(ShopCardId.CANNON))
        positions.move(ShopCardId.BALLISTA, 100.0, 200.0, layout, 1920, 1080)
        val savedPosition = positions.write().getAsJsonObject("ballista")
        val beforeReset = positions.revision
        positions.resetScale(ShopCardId.BALLISTA)
        assertEquals(beforeReset + 1, positions.revision)
        assertEquals(1.0, positions.scale(ShopCardId.BALLISTA))
        val remainingPosition = positions.write().getAsJsonObject("ballista")
        assertEquals(savedPosition.get("x"), remainingPosition.get("x"))
        assertEquals(savedPosition.get("y"), remainingPosition.get("y"))
        assertFalse(remainingPosition.has("scale"))
        positions.resetScale(ShopCardId.BALLISTA)
        assertEquals(beforeReset + 1, positions.revision)
        positions.setScale(ShopCardId.BALLISTA, 1.4)
        positions.reset(ShopCardId.BALLISTA)
        assertFalse(positions.write().has("ballista"))
        assertEquals(ShopCardId.BALLISTA.defaultRect, positions.rect(ShopCardId.BALLISTA, layout, 1920, 1080))
        assertEquals(0.1, positions.scale(ShopCardId.CANNON))
        val beforeAllReset = positions.revision
        positions.reset()
        assertEquals(beforeAllReset + 1, positions.revision)
        assertEquals(0, positions.write().size())
        positions.reset()
        assertEquals(beforeAllReset + 1, positions.revision)
    }
}
