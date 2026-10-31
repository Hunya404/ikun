import com.github.ikun.utils.kuudra.KuudraEntryNotice
import kotlin.test.*

class KuudraEntryNoticeTest {
    private val player = "Player_123"

    @Test
    fun `server entry for the local player accepts every tier and optional rank or formatting`() {
        val world = Any()
        listOf("Basic", "Hot", "Burning", "Fiery", "Infernal").forEach { tier ->
            val notice = KuudraEntryNotice()
            assertTrue(notice.observe("$player entered Kuudra's Hollow, $tier Tier!", player, world), tier)
            assertTrue(notice.matches(world))
        }
        val formatted = KuudraEntryNotice()
        assertTrue(formatted.observe("  §a[MVP§c+§a] §b$player entered Kuudra’s Hollow, Infernal Tier!§r  ", "player_123", world))
        assertTrue(formatted.matches(world))
    }

    @Test
    fun `server three line separators work with actual and escaped newlines`() {
        listOf("\n", "\\n").forEach { newline ->
            val notice = KuudraEntryNotice()
            val world = Any()
            val entry = "-----${newline}$player entered Kuudra's Hollow, Infernal Tier!${newline}-----"
            assertTrue(notice.observe(entry, player, world), "newline=$newline")
            assertTrue(notice.matches(world))
        }
    }

    @Test
    fun `another player and player chat cannot claim that the local player entered`() {
        val notice = KuudraEntryNotice()
        val world = Any()
        val entry = "$player entered Kuudra's Hollow, Infernal Tier!"
        val messages = listOf(
            "OtherPlayer entered Kuudra's Hollow, Infernal Tier!",
            "${player}4 entered Kuudra's Hollow, Infernal Tier!",
            "Party > [MVP+] Example: $entry",
            "Guild > Example: $entry",
            "Example: $entry",
            "$entry extra",
            entry.removeSuffix("!"),
            "Party > Example:\n$entry",
            "Party > Example:\n$entry\n-----",
            "$player entered Kuudra's Hollow, Unknown Tier!"
        )

        messages.forEach {
            assertFalse(notice.observe(it, player, world), it)
            assertFalse(notice.matches(world), it)
        }
        assertTrue(notice.observe(entry, player, world))
        assertTrue(notice.matches(world))
    }

    @Test
    fun `entry belongs only to its world identity and reset clears it`() {
        data class World(val name: String)
        val first = World("same server")
        val second = World("same server")
        val notice = KuudraEntryNotice()
        val entry = "$player entered Kuudra's Hollow, Infernal Tier!"

        assertFalse(notice.matches(first))
        assertTrue(notice.observe(entry, player, first))
        assertTrue(notice.matches(first))
        assertFalse(notice.matches(second))
        assertTrue(notice.observe(entry, player, second))
        assertFalse(notice.matches(first))
        assertTrue(notice.matches(second))
        notice.reset()
        assertFalse(notice.matches(first))
        assertFalse(notice.matches(second))
    }
}
