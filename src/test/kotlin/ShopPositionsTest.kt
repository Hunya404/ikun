import com.github.ikun.utils.kuudra.ShopCardId
import com.github.ikun.utils.kuudra.ShopLayout
import com.github.ikun.utils.kuudra.ShopPositions
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlin.test.*

class ShopPositionsTest {
    @Test
    fun `default sword and compact confirmation pair are centered at every supported scale`() {
        val positions = ShopPositions()
        for ((width, height) in listOf(1920 to 1080, 854 to 480, 320 to 180, 200 to 120)) {
            for (step in 1..20) {
                val layout = ShopLayout(width, height, step / 10.0)
                val sword = positions.rect(ShopCardId.CLASS_ROUTE, layout, width, height)
                assertEquals(width / 2.0, layout.left + (sword.x + sword.width / 2.0) * layout.scale, 1e-9)
                assertEquals(height / 2.0, layout.top + (sword.y + sword.height / 2.0) * layout.scale, 1e-9)
                val confirm = positions.rect(ShopCardId.CONFIRM, layout, width, height)
                val cancel = positions.rect(ShopCardId.CANCEL, layout, width, height)
                val left = minOf(confirm.x, cancel.x)
                val right = maxOf(confirm.x + confirm.width, cancel.x + cancel.width)
                val top = minOf(confirm.y, cancel.y)
                val bottom = maxOf(confirm.y + confirm.height, cancel.y + cancel.height)
                assertEquals(width / 2.0, layout.left + (left + right) / 2.0 * layout.scale, 1e-9)
                assertEquals(height / 2.0, layout.top + (top + bottom) / 2.0 * layout.scale, 1e-9)
                assertTrue(confirm.x + confirm.width < cancel.x)
                assertTrue(confirm.width < sword.width && confirm.height < sword.height)
            }
        }
    }

    @Test
    fun `saved sword and confirmation positions override the new centered defaults`() {
        val positions = ShopPositions().apply {
            read(JsonParser.parseString("""{
                "class_route": {"x": 0.25, "y": 0.3},
                "confirm": {"x": 0.4, "y": 0.7},
                "cancel": {"x": 0.8, "y": 0.7}
            }"""))
        }
        val layout = ShopLayout(1000, 800)
        for ((id, center) in listOf(ShopCardId.CLASS_ROUTE to (250.0 to 240.0),
            ShopCardId.CONFIRM to (400.0 to 560.0), ShopCardId.CANCEL to (800.0 to 560.0))) {
            val rect = positions.rect(id, layout, 1000, 800)
            assertEquals(center.first, layout.left + (rect.x + rect.width / 2.0) * layout.scale)
            assertEquals(center.second, layout.top + (rect.y + rect.height / 2.0) * layout.scale)
            assertNotEquals(id.defaultRect, rect)
        }
    }

    @Test
    fun `moving a stable card ID leaves other cards and later tiers independent`() {
        val positions = ShopPositions()
        val layout = ShopLayout(1280, 720)
        assertEquals(ShopCardId.CLASS_ROUTE.defaultRect, positions.rect(ShopCardId.CLASS_ROUTE, layout, 1280, 720))
        positions.move(ShopCardId.BALLISTA, 80.0, 120.0, layout, 1280, 720)
        val moved = positions.rect(ShopCardId.BALLISTA, layout, 1280, 720)
        assertEquals(80.0, layout.left + moved.x * layout.scale)
        assertEquals(120.0, layout.top + moved.y * layout.scale)
        assertEquals(ShopCardId.CLASS_ROUTE.defaultRect, positions.rect(ShopCardId.CLASS_ROUTE, layout, 1280, 720))
        assertEquals(ShopCardId.CANNON.defaultRect, positions.rect(ShopCardId.CANNON, layout, 1280, 720))
        positions.move(ShopCardId.CANNON, 940.0, 530.0, layout, 1280, 720)
        assertEquals(moved, positions.rect(ShopCardId.BALLISTA, layout, 1280, 720))
        assertEquals(setOf("ballista", "cannon"), positions.write().keySet())
    }

    @Test
    fun `normalized centers survive resolution and scale changes until an edge requires clamping`() {
        val positions = ShopPositions()
        val initial = ShopLayout(1000, 800)
        positions.move(ShopCardId.CANNON, 498.0, 266.0, initial, 1000, 800)
        for ((width, height) in listOf(1000 to 800, 1920 to 1080, 854 to 480)) {
            for (scale in listOf(0.1, 0.5, 1.0, 2.0)) {
                val layout = ShopLayout(width, height, scale)
                val rect = positions.rect(ShopCardId.CANNON, layout, width, height)
                val centerX = layout.left + (rect.x + rect.width / 2.0) * layout.scale
                val centerY = layout.top + (rect.y + rect.height / 2.0) * layout.scale
                assertEquals(width * 0.6, centerX, layout.scale / 2 + 1e-9)
                assertEquals(height * 0.375, centerY, layout.scale / 2 + 1e-9)
            }
        }
    }

    @Test
    fun `dragging beyond every edge keeps the complete card visible across scales and resolutions`() {
        for ((width, height) in listOf(1920 to 1080, 854 to 480, 320 to 180)) {
            for (scale in listOf(0.1, 0.7, 1.0, 2.0)) {
                val layout = ShopLayout(width, height, scale)
                for (id in ShopCardId.entries) {
                    for (edge in listOf(-10_000.0, 10_000.0)) {
                        val positions = ShopPositions()
                        positions.move(id, edge, edge, layout, width, height)
                        val rect = positions.rect(id, layout, width, height)
                        val left = layout.left + rect.x * layout.scale
                        val top = layout.top + rect.y * layout.scale
                        assertTrue(left >= -1e-9)
                        assertTrue(top >= -1e-9)
                        assertTrue(left + rect.width * layout.scale <= width + 1e-9)
                        assertTrue(top + rect.height * layout.scale <= height + 1e-9)
                    }
                }
            }
        }
    }

    @Test
    fun `resizing clamps rendering without overwriting the saved screen center`() {
        val positions = ShopPositions()
        val initial = ShopLayout(1920, 1080, 0.1)
        positions.move(ShopCardId.CONFIRM, 0.0, 0.0, initial, 1920, 1080)
        val saved = positions.write()
        val originalRect = positions.rect(ShopCardId.CONFIRM, initial, 1920, 1080)
        val cramped = ShopLayout(320, 180, 2.0)
        val rect = positions.rect(ShopCardId.CONFIRM, cramped, 320, 180)
        val left = cramped.left + rect.x * cramped.scale
        val top = cramped.top + rect.y * cramped.scale
        assertTrue(left >= -1e-9 && top >= -1e-9)
        assertTrue(left + rect.width * cramped.scale <= 320 + 1e-9)
        assertTrue(top + rect.height * cramped.scale <= 180 + 1e-9)
        assertEquals(saved, positions.write())
        assertEquals(originalRect, positions.rect(ShopCardId.CONFIRM, initial, 1920, 1080))
    }

    @Test
    fun `loading positions preserves valid entries and ignores malformed entries independently`() {
        val positions = ShopPositions()
        positions.read(JsonParser.parseString("""{
            "ballista": {"x": 0.25, "y": 0.5},
            "class_route": {"x": "0.2", "y": 0.4},
            "revive": {"x": 0.2, "y": 0.4},
            "cannon": {"x": 2.0, "y": 0.4},
            "confirm": {"x": 0.4},
            "cancel": null,
            "unknown": {"x": 0.1, "y": 0.1}
        }"""))
        assertEquals(setOf("ballista"), positions.write().keySet())
        val revision = positions.revision
        val restored = ShopPositions().apply { read(positions.write()) }
        assertEquals(positions.write(), restored.write())
        positions.read(JsonParser.parseString("{}"))
        positions.read(JsonParser.parseString("[]"))
        positions.read(JsonObject().apply {
            add("ballista", JsonObject().apply {
                addProperty("x", Double.NaN)
                addProperty("y", 0.2)
            })
            add("cannon", JsonObject().apply {
                addProperty("x", 0.3)
                addProperty("y", Double.POSITIVE_INFINITY)
            })
        })
        assertEquals(restored.write(), positions.write())
        assertEquals(revision, positions.revision)
        val layout = ShopLayout(1280, 720)
        for (id in ShopCardId.entries) {
            assertEquals(positions.rect(id, layout, 1280, 720), restored.rect(id, layout, 1280, 720))
        }
    }

    @Test
    fun `individual and full resets restore defaults and only mutations advance revision`() {
        val positions = ShopPositions()
        val layout = ShopLayout(1280, 720)
        positions.reset()
        positions.move(ShopCardId.BALLISTA, Double.NaN, 20.0, layout, 1280, 720)
        assertEquals(0, positions.revision)
        positions.move(ShopCardId.BALLISTA, 20.0, 20.0, layout, 1280, 720)
        positions.move(ShopCardId.CANNON, 60.0, 60.0, layout, 1280, 720)
        val revision = positions.revision
        positions.move(ShopCardId.CANNON, 60.0, 60.0, layout, 1280, 720)
        assertEquals(revision, positions.revision)
        positions.reset(ShopCardId.BALLISTA)
        assertEquals(revision + 1, positions.revision)
        assertEquals(ShopCardId.BALLISTA.defaultRect, positions.rect(ShopCardId.BALLISTA, layout, 1280, 720))
        assertNotEquals(ShopCardId.CANNON.defaultRect, positions.rect(ShopCardId.CANNON, layout, 1280, 720))
        positions.reset(ShopCardId.BALLISTA)
        assertEquals(revision + 1, positions.revision)
        positions.reset()
        assertEquals(revision + 2, positions.revision)
        assertEquals(0, positions.write().size())
        for (id in ShopCardId.entries) assertEquals(id.defaultRect, positions.rect(id, layout, 1280, 720))
    }
}
