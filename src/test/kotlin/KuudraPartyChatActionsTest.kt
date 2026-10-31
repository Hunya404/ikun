import com.github.ikun.utils.PartyState
import com.github.ikun.utils.kuudra.KuudraPartyChatActions
import net.minecraft.network.chat.ClickEvent
import kotlin.test.*

class KuudraPartyChatActionsTest {
    @Test
    fun `each action ID resolves only its bound player and requires the same generation and party session`() {
        val actions = KuudraPartyChatActions()
        val first = actions.track("FirstPlayer", 5, 10)
        val second = actions.track("SecondPlayer", 5, 10)

        assertNotEquals(first.id, second.id)
        assertEquals("FirstPlayer", actions.resolve(first.id, 5, 10)?.playerName)
        assertEquals("SecondPlayer", actions.resolve(second.id, 5, 10)?.playerName)
        assertNull(actions.resolve("FirstPlayer", 5, 10))
        assertNull(actions.resolve("missing-action", 5, 10))
        assertNull(actions.resolve(first.id, 6, 10))
        assertNull(actions.resolve(first.id, 5, 11))
        assertNull(actions.consumeKick(first.id, 6, 10, "Owner", true, listOf("Owner", "FirstPlayer")))
        assertNull(actions.consumeKick(first.id, 5, 11, "Owner", true, listOf("Owner", "FirstPlayer")))
    }

    @Test
    fun `retrying a player replaces their old action without expiring another player's action`() {
        val actions = KuudraPartyChatActions()
        val old = actions.track("Candidate", 1, 1)
        val other = actions.track("OtherPlayer", 1, 1)
        val fresh = actions.track("candidate", 1, 1)

        assertNotEquals(old.id, fresh.id)
        assertNull(actions.resolve(old.id, 1, 1))
        assertEquals(fresh, actions.resolve(fresh.id, 1, 1))
        assertEquals(other, actions.resolve(other.id, 1, 1))
        assertNull(actions.consumeKick(old.id, 1, 1, "Owner", true, listOf("Owner", "Candidate")))
    }

    @Test
    fun `kick is sent once and only for the player bound to the selected action`() {
        val actions = KuudraPartyChatActions()
        val first = actions.track("FirstPlayer", 1, 1)
        val second = actions.track("SecondPlayer", 1, 1)
        val members = listOf("Owner", "FirstPlayer", "SecondPlayer")

        assertEquals("p kick SecondPlayer", actions.consumeKick(second.id, 1, 1, "Owner", true, members))
        assertNull(actions.consumeKick(second.id, 1, 1, "Owner", true, members))
        assertEquals("p kick FirstPlayer", actions.consumeKick(first.id, 1, 1, "Owner", true, members))
    }

    @Test
    fun `nonleader missing membership and self kick are rejected without consuming a valid kick`() {
        val actions = KuudraPartyChatActions()
        val target = actions.track("Candidate", 1, 1)

        assertNull(actions.consumeKick(target.id, 1, 1, "Owner", false, listOf("Owner", "Candidate")))
        assertNull(actions.consumeKick(target.id, 1, 1, "Owner", true, listOf("Candidate")))
        assertNull(actions.consumeKick(target.id, 1, 1, "Owner", true, listOf("Owner")))
        assertNull(actions.consumeKick(target.id, 1, 1, "CANDIDATE", true, listOf("Owner", "Candidate")))
        assertEquals("p kick Candidate", actions.consumeKick(target.id, 1, 1, "owner", true, listOf("OWNER", "CANDIDATE")))
    }

    @Test
    fun `chat components invoke ikun commands with opaque IDs instead of arbitrary player text`() {
        val actions = KuudraPartyChatActions()
        val target = actions.track("Candidate", 1, 1)
        val retry = actions.retryComponent(target)
        val kick = actions.kickComponent(target)

        assertEquals("/ikun retry ${target.id}", assertIs<ClickEvent.RunCommand>(retry.style.clickEvent).command())
        assertEquals("/ikun kick ${target.id}", assertIs<ClickEvent.RunCommand>(kick.style.clickEvent).command())
        assertTrue(retry.string.contains("Candidate"))
        assertFalse(kick.string.contains("ILOVEKU", ignoreCase = true))
    }

    @Test
    fun `clear and bounded history invalidate old actions`() {
        val actions = KuudraPartyChatActions()
        val oldest = actions.track("Candidate0", 1, 1)
        val remaining = (1..100).map { actions.track("Candidate$it", 1, 1) }

        assertNull(actions.resolve(oldest.id, 1, 1))
        assertNotNull(actions.resolve(remaining.first().id, 1, 1))
        assertNotNull(actions.resolve(remaining.last().id, 1, 1))
        actions.clear()
        remaining.forEach { assertNull(actions.resolve(it.id, 1, 1)) }
    }

    @Test
    fun `player names cannot inject commands into tracked targets`() {
        val actions = KuudraPartyChatActions()
        listOf("", "bad/name", "two names", "Name\np kick Owner", "a".repeat(17)).forEach { name ->
            assertFailsWith<IllegalArgumentException>(name) { actions.track(name, 1, 1) }
        }
        assertEquals("Valid_Name123", actions.track("Valid_Name123", 1, 1).playerName)
    }

    @Test
    fun `leaving and rejoining the same party cannot reactivate an old chat action`() {
        val state = PartyState()
        val actions = KuudraPartyChatActions()
        state.accept("Party Finder > Your kuudra group has been queued!", "Owner")
        state.accept("Candidate joined the party.", "Owner")
        val oldSession = state.sessionId
        val old = actions.track("Candidate", 1, oldSession)

        state.accept("You left the party.", "Owner")
        assertNull(actions.consumeKick(old.id, 1, state.sessionId, "Owner", false, state.members))
        state.accept("Party Finder > Your kuudra group has been queued!", "Owner")
        state.accept("Candidate joined the party.", "Owner")
        assertNotEquals(oldSession, state.sessionId)
        assertNull(actions.resolve(old.id, 1, state.sessionId))
        assertNull(actions.consumeKick(old.id, 1, state.sessionId, "Owner", true, state.members))

        val current = actions.track("Candidate", 1, state.sessionId)
        assertEquals("p kick Candidate", actions.consumeKick(current.id, 1, state.sessionId, "Owner", true, state.members))
    }
}
