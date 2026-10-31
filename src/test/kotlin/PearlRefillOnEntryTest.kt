import com.github.ikun.utils.kuudra.PearlRefillOnEntry
import kotlin.test.*

class PearlRefillOnEntryTest {
    @Test
    fun `first inventory check waits ten seconds and later checks wait twenty seconds`() {
        val refill = PearlRefillOnEntry()
        val world = Any()
        var reads = 0
        val pearls = { reads++; 4 }

        assertNull(refill.command(world, true, pearls, true, 1_000))
        assertNull(refill.command(world, true, pearls, true, 10_999))
        assertEquals(0, reads)
        assertEquals("getfromsacks ENDER_PEARL 12", refill.command(world, true, pearls, true, 11_000))
        assertNull(refill.command(world, true, pearls, true, 30_999))
        assertEquals(1, reads)
        assertEquals("getfromsacks ENDER_PEARL 12", refill.command(world, true, pearls, true, 31_000))
        assertEquals(2, reads)
    }

    @Test
    fun `refill only requests the missing pearls up to sixteen`() {
        val cases = listOf(
            0 to "getfromsacks ENDER_PEARL 16",
            1 to "getfromsacks ENDER_PEARL 15",
            15 to "getfromsacks ENDER_PEARL 1",
            16 to null,
            32 to null
        )

        cases.forEach { (count, expected) ->
            val refill = PearlRefillOnEntry()
            val world = Any()
            refill.command(world, true, { count }, true, 0)
            assertEquals(expected, refill.command(world, true, { count }, true, 10_000), "inventory=$count")
        }
    }

    @Test
    fun `a full inventory still starts the next twenty second interval`() {
        val refill = PearlRefillOnEntry()
        val world = Any()
        refill.command(world, true, { 16 }, true, 0)
        assertNull(refill.command(world, true, { 16 }, true, 10_000))
        assertEquals(PearlRefillOnEntry.Status.FULL, refill.status)
        assertNull(refill.command(world, true, { error("must not rescan before the interval") }, true, 29_999))
        assertEquals("getfromsacks ENDER_PEARL 1", refill.command(world, true, { 15 }, true, 30_000))
    }

    @Test
    fun `GUI or death blocking postpones an eligible check without reading inventory or consuming it`() {
        val refill = PearlRefillOnEntry()
        val world = Any()
        val unreadable = { error("blocked input must not read inventory") }
        refill.command(world, true, unreadable, true, 0)

        assertNull(refill.command(world, true, unreadable, false, 10_000))
        assertEquals(PearlRefillOnEntry.Status.WAITING_INPUT, refill.status)
        assertNull(refill.command(world, true, unreadable, false, 12_000))
        assertEquals("getfromsacks ENDER_PEARL 8", refill.command(world, true, { 8 }, true, 12_001))
        assertNull(refill.command(world, true, unreadable, true, 32_000))
        assertNull(refill.command(world, true, unreadable, false, 32_001))
        assertEquals("getfromsacks ENDER_PEARL 8", refill.command(world, true, { 8 }, true, 32_002))
    }

    @Test
    fun `an unsynchronized negative count retries immediately instead of consuming the first check`() {
        val refill = PearlRefillOnEntry()
        val world = Any()
        refill.command(world, true, { -1 }, true, 0)

        assertNull(refill.command(world, true, { -1 }, true, 10_000))
        assertEquals(PearlRefillOnEntry.Status.WAITING_SYNC, refill.status)
        assertNull(refill.pearlCount)
        assertEquals("getfromsacks ENDER_PEARL 16", refill.command(world, true, { 0 }, true, 10_001))
    }

    @Test
    fun `a new world identity receives its own ten second synchronization wait`() {
        data class World(val name: String)
        val first = World("same server")
        val second = World("same server")
        val refill = PearlRefillOnEntry()
        refill.command(first, true, { 0 }, true, 0)
        assertEquals("getfromsacks ENDER_PEARL 16", refill.command(first, true, { 0 }, true, 10_000))

        assertNull(refill.command(second, true, { error("new world inventory is not synchronized") }, true, 15_000))
        assertNull(refill.pearlCount)
        assertNull(refill.command(second, true, { error("still waiting for the new world") }, true, 24_999))
        assertEquals("getfromsacks ENDER_PEARL 16", refill.command(second, true, { 0 }, true, 25_000))
    }

    @Test
    fun `outside Kuudra never reads inventory and reset restarts entry synchronization`() {
        val refill = PearlRefillOnEntry()
        val world = Any()
        assertNull(refill.command(world, false, { error("outside Kuudra") }, true, 0))
        refill.command(world, true, { 0 }, true, 5_000)
        assertEquals("getfromsacks ENDER_PEARL 16", refill.command(world, true, { 0 }, true, 15_000))
        assertNull(refill.command(world, false, { error("outside Kuudra") }, true, 100_000))
        assertEquals(PearlRefillOnEntry.Status.OUTSIDE, refill.status)

        refill.reset()
        assertNull(refill.pearlCount)
        assertNull(refill.command(world, true, { error("reset needs another synchronization wait") }, true, 100_001))
        assertNull(refill.command(world, true, { error("not ten seconds yet") }, true, 110_000))
        assertEquals("getfromsacks ENDER_PEARL 16", refill.command(world, true, { 0 }, true, 110_001))
    }
}
