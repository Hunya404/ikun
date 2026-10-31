import com.github.ikun.features.impl.cheat.RendRotate
import com.github.ikun.config.types.NumberSetting
import com.github.ikun.ui.clickgui.enums.CategoryType
import com.github.ikun.utils.kuudra.RendPhaseState
import com.github.ikun.utils.kuudra.RendRotationQueue
import com.github.ikun.utils.kuudra.RendRotationRules
import net.minecraft.world.phys.Vec3
import kotlin.test.*

class RendRotationRulesTest {
    private val entry = Vec3(-101.5, 6.0, -105.5)

    private fun stun() = RendPhaseState().apply {
        observeTab("Area: Kuudra")
        observeChat(RendPhaseState.STUN_START, "Player")
    }

    @Test fun `original entry box includes both boundary corners`() {
        assertTrue(RendRotationRules.isEntry(entry))
        assertTrue(RendRotationRules.isEntry(Vec3(-102.0, 5.0, -106.0)))
        assertTrue(RendRotationRules.isEntry(Vec3(-101.0, 7.0, -105.0)))
    }

    @Test fun `entry box rejects positions just outside each face`() {
        for (position in listOf(Vec3(-102.001, 6.0, -105.5), Vec3(-100.999, 6.0, -105.5),
            Vec3(-101.5, 4.999, -105.5), Vec3(-101.5, 7.001, -105.5),
            Vec3(-101.5, 6.0, -106.001), Vec3(-101.5, 6.0, -104.999))) {
            assertFalse(RendRotationRules.isEntry(position), position.toString())
        }
    }

    @Test fun `all four original targets and speed defaults are retained`() {
        val directions = listOf(
            Vec3(-129.0, 10.0, -105.0) to RendRotationRules.Direction.RIGHT,
            Vec3(-100.0, 10.0, -83.0) to RendRotationRules.Direction.FRONT,
            Vec3(-71.0, 10.0, -105.0) to RendRotationRules.Direction.LEFT,
            Vec3(-100.0, 10.0, -133.0) to RendRotationRules.Direction.BACK)
        directions.forEach { (pos, expected) -> assertEquals(expected, RendRotationRules.direction(pos)) }
        assertEquals(Vec3(-123.5, 16.0, -105.5), RendRotationRules.Direction.RIGHT.target)
        assertEquals(Vec3(-104.5, 16.0, -82.5), RendRotationRules.Direction.FRONT.target)
        assertEquals(Vec3(-80.5, 16.0, -104.5), RendRotationRules.Direction.LEFT.target)
        assertEquals(Vec3(-98.5, 16.0, -129.5), RendRotationRules.Direction.BACK.target)
        assertEquals(85.0 to 145.0, RendRotationRules.Direction.FRONT.let { it.minimum to it.maximum })
        assertEquals(235.0 to 295.0, RendRotationRules.Direction.RIGHT.let { it.minimum to it.maximum })
        assertEquals(235.0 to 295.0, RendRotationRules.Direction.LEFT.let { it.minimum to it.maximum })
        assertEquals(285.0 to 345.0, RendRotationRules.Direction.BACK.let { it.minimum to it.maximum })
    }

    @Test fun `direction uses strict boundaries and source corner priority`() {
        for (p in listOf(Vec3(-128.0, 0.0, -105.0), Vec3(-100.0, 0.0, -84.0),
            Vec3(-72.0, 0.0, -105.0), Vec3(-100.0, 0.0, -132.0), entry)) {
            assertNull(RendRotationRules.direction(p), p.toString())
        }
        assertEquals(RendRotationRules.Direction.RIGHT, RendRotationRules.direction(Vec3(-129.0, 0.0, -80.0)))
        assertEquals(RendRotationRules.Direction.FRONT, RendRotationRules.direction(Vec3(-70.0, 0.0, -80.0)))
        assertEquals(RendRotationRules.Direction.LEFT, RendRotationRules.direction(Vec3(-70.0, 0.0, -140.0)))
    }

    @Test fun `normal magma cubes and dead bosses cannot supply a rotation goal`() {
        assertTrue(RendRotationRules.isBoss(true, 30, 100_000.0))
        assertFalse(RendRotationRules.isBoss(false, 30, 100_000.0))
        assertFalse(RendRotationRules.isBoss(true, 29, 100_000.0))
        assertFalse(RendRotationRules.isBoss(true, 30, 99_999.0))
    }

    @Test fun `only enabled stun or dps encounter entry can trigger`() {
        val state = RendPhaseState()
        state.observeTab("Area: Kuudra")
        assertFalse(state.canTrigger(true, entry))
        state.observeChat(RendPhaseState.SUPPLIES_START, "Player")
        assertFalse(state.canTrigger(true, entry))
        state.observeChat(RendPhaseState.BUILD_START, "Player")
        assertFalse(state.canTrigger(true, entry))
        state.observeChat(RendPhaseState.STUN_START, "Player")
        assertTrue(state.canTrigger(true, entry))
        assertFalse(state.canTrigger(false, entry))
        state.observeHeight(6.0)
        assertEquals(RendPhaseState.Phase.DPS, state.phase)
        assertTrue(state.canTrigger(true, entry))
        assertFalse(state.canTrigger(true, Vec3(0.0, 6.0, 0.0)))
    }

    @Test fun `stun transition preserves original twenty block height boundary`() {
        val state = stun()
        state.observeHeight(20.0)
        assertEquals(RendPhaseState.Phase.STUN, state.phase)
        state.observeHeight(19.999)
        assertEquals(RendPhaseState.Phase.DPS, state.phase)
    }

    @Test fun `only own final death prevents future trigger until world reset`() {
        val state = stun()
        state.observeChat("Other was FINAL KILLED by Kuudra!", "Player")
        assertTrue(state.canTrigger(true, entry))
        state.observeChat("Player was FINAL KILLED by Kuudra!", "Player")
        assertFalse(state.canTrigger(true, entry))
        state.observeChat(RendPhaseState.STUN_START, "Player")
        assertFalse(state.canTrigger(true, entry))
        state.reset()
        assertFalse(state.isDead)
        assertFalse(state.inKuudra)
        assertEquals(RendPhaseState.Phase.NONE, state.phase)
    }

    @Test fun `tab area tracking accepts original area and dungeon markers`() {
        val state = RendPhaseState()
        assertTrue(state.observeTab("§rArea: §bKuudra's Hollow"))
        assertTrue(state.inKuudra)
        assertFalse(state.observeTab("Other tab line"))
        assertTrue(state.inKuudra)
        assertTrue(state.observeTab("Dungeon: KUUDRA"))
        assertTrue(state.inKuudra)
        assertTrue(state.observeTab("Area: Crimson Isle"))
        assertFalse(state.inKuudra)
    }

    @Test fun `source singleplayer exception still requires encounter and entry coordinates`() {
        val state = RendPhaseState()
        state.observeChat(RendPhaseState.STUN_START, "Player")
        assertFalse(state.canTrigger(true, entry))
        assertTrue(state.canTrigger(true, entry, singleplayer = true))
        assertFalse(state.canTrigger(false, entry, singleplayer = true))
    }

    @Test fun `delays preserve source post decrement scheduling`() {
        val queue = RendRotationQueue()
        queue.schedule(5)
        repeat(5) { assertEquals(0, queue.tick()) }
        assertEquals(1, queue.tick())
        assertEquals(0, queue.tick())
        queue.schedule(0)
        assertEquals(1, queue.tick())
    }

    @Test fun `separate position packets remain independently queued`() {
        val queue = RendRotationQueue()
        queue.schedule(1)
        queue.schedule(1)
        queue.schedule(3)
        assertEquals(0, queue.tick())
        assertEquals(2, queue.tick())
        assertEquals(0, queue.tick())
        assertEquals(1, queue.tick())
    }

    @Test fun `world change or disabling cancels every pending turn`() {
        val queue = RendRotationQueue()
        queue.schedule(0)
        queue.schedule(20)
        queue.reset()
        repeat(30) { assertEquals(0, queue.tick()) }
        queue.schedule(-1)
        assertEquals(0, queue.tick())
    }

    @Test fun `standalone rotation defaults off in cheat with no movement or item controls`() {
        assertFalse(RendRotate.enabled)
        assertEquals(CategoryType.CHEAT, RendRotate.category)
        assertEquals(5.0, (RendRotate.getSettingByName("Rotation Delay") as NumberSetting).value)
        assertEquals(13, RendRotate.configSettings.size)
        for (name in listOf("Auto Walk", "Auto Jump", "Auto Sneak", "Auto Rod", "Auto Bone", "Auto Loadout")) {
            assertNull(RendRotate.getSettingByName(name))
        }
    }
}
