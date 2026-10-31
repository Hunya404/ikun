import com.github.ikun.utils.kuudra.AutoGfsTrigger
import kotlin.test.*

class AutoGfsTriggerTest {
    private val cue = "[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!"

    @Test
    fun `ballista completion cue requests toxic using the RatAddons command`() {
        val trigger = AutoGfsTrigger()
        val formatted = " \t§e[NPC] §bElle: Phew! §aThe Ballista is finally ready!§r It should be strong enough to tank Kuudra's blows now!  "

        assertEquals("gfs toxic_arrow_poison 16", trigger.command(formatted, true))
    }

    @Test
    fun `old shooting cue and altered completion phrases do not trigger`() {
        val trigger = AutoGfsTrigger()
        val otherMessages = listOf(
            "[NPC] Elle: That looks like it hurt! Quickly, while Kuudra is distracted, shoot him with the Ballista!",
            "The Ballista is finally ready",
            "the Ballista is finally ready!",
            "The Ballista is finally Ready!",
            "The Ballista is  finally ready!",
            "[NPC] Elle: OMG! Great work collecting my supplies!"
        )

        otherMessages.forEach { assertNull(trigger.command(it, true), it) }
        assertEquals("gfs toxic_arrow_poison 16", trigger.command(cue, true))
    }

    @Test
    fun `RatAddons substring matching does not require the full NPC prefix`() {
        val trigger = AutoGfsTrigger()
        listOf(
            "The Ballista is finally ready!",
            "prefix The Ballista is finally ready! suffix",
            "Party > [MVP+] Example: The Ballista is finally ready!"
        ).forEach { assertEquals("gfs toxic_arrow_poison 16", trigger.command(it, true), it) }
    }

    @Test
    fun `outside Kuudra does nothing and repeated cues in the same run each request toxic`() {
        val trigger = AutoGfsTrigger()
        assertNull(trigger.command(cue, false))
        assertEquals("gfs toxic_arrow_poison 16", trigger.command(cue, true))
        assertEquals("gfs toxic_arrow_poison 7", trigger.command(cue, true, 7))
        assertNull(trigger.command(cue, false, 32))
        assertEquals("gfs toxic_arrow_poison 32", trigger.command(cue, true, 32))
    }

    @Test
    fun `configured toxic quantity is requested directly within the one to thirty two limits`() {
        val cases = listOf(
            Int.MIN_VALUE to "gfs toxic_arrow_poison 1",
            0 to "gfs toxic_arrow_poison 1",
            1 to "gfs toxic_arrow_poison 1",
            7 to "gfs toxic_arrow_poison 7",
            16 to "gfs toxic_arrow_poison 16",
            32 to "gfs toxic_arrow_poison 32",
            33 to "gfs toxic_arrow_poison 32",
            Int.MAX_VALUE to "gfs toxic_arrow_poison 32"
        )

        cases.forEach { (amount, expected) ->
            assertEquals(expected, AutoGfsTrigger().command(cue, true, amount), "quantity=$amount")
        }
    }

    @Test
    fun `area detection accepts server location lines without matching player chat or similar locations`() {
        listOf("Kuudra's Hollow", "§c⏣ Kuudra’s Hollow (T5)§r").forEach {
            assertTrue(AutoGfsTrigger.isKuudraSidebarLine(it), it)
        }
        listOf("Area: Kuudra", "§eArea:  Kuudra's Hollow (T1)§r").forEach {
            assertTrue(AutoGfsTrigger.isKuudraTabLine(it), it)
        }
        listOf("Party > Example: Kuudra's Hollow", "Kuudra's Hollow Entrance", "Kuudra's Hollow (T6)", "Area: Kuudra").forEach {
            assertFalse(AutoGfsTrigger.isKuudraSidebarLine(it), it)
        }
        listOf("Party > Example: Area: Kuudra", "Area: Kuudra's Hollow Entrance", "Area: Kuudra's Hollow (T6)", "Kuudra's Hollow").forEach {
            assertFalse(AutoGfsTrigger.isKuudraTabLine(it), it)
        }
    }
}
