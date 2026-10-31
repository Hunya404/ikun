import com.github.ikun.utils.PartyState
import kotlin.test.*

class PartyStateTest {
    @Test
    fun `joining another party creates a new session and replaces the previous party roster`() {
        val state = PartyState()
        state.accept("Party Finder > Your party has been queued in the party finder!", "Owner")
        state.accept("OldMember joined the party.", "Owner")
        val previous = state.sessionId

        state.accept("You have joined [MVP+] NewLeader's party!", "Owner")
        assertNotEquals(previous, state.sessionId)
        assertTrue(state.isInParty)
        assertEquals("NewLeader", state.partyLeader)
        assertEquals(setOf("Owner", "NewLeader"), state.members.toSet())
    }

    @Test
    fun `queueing identifies the local leader for every supported party finder announcement`() {
        val announcements = listOf(
            "Party Finder > Your party has been queued!",
            "Party Finder > Your party has been queued in the party finder!",
            "Party Finder > Your party has been queued in the dungeon finder!",
            "Party Finder > Your kuudra group has been queued in the kuudra finder!"
        )
        announcements.forEach { announcement ->
            val state = PartyState()
            state.accept(announcement, "Owner")
            assertTrue(state.isInParty, announcement)
            assertEquals("Owner", state.partyLeader, announcement)
            assertEquals(listOf("Owner"), state.members, announcement)
        }
    }

    @Test
    fun `ordinary member changes do not change the party session and departed targets are removed`() {
        val state = PartyState()
        state.accept("Party Finder > Your party has been queued!", "Owner")
        val session = state.sessionId
        state.accept("[MVP+] Candidate joined the party.", "Owner")
        state.accept("Party Finder > [MVP+] OtherPlayer joined the group! (Combat Level 60)", "Owner")
        assertEquals(setOf("Owner", "Candidate", "OtherPlayer"), state.members.toSet())

        state.accept("Candidate has left the party.", "Owner")
        assertFalse("Candidate" in state.members)
        assertEquals(session, state.sessionId)
        assertTrue(state.isInParty)
        assertEquals("Owner", state.partyLeader)
    }

    @Test
    fun `leaving disbanding kicking or resetting clears the roster and changes the session`() {
        val departures = listOf(
            "You left the party.",
            "[MVP+] Leader has disbanded the party!",
            "You have been kicked from the party by [MVP+] Leader",
            "The party was disbanded because the party leader disconnected.",
            "You are not currently in a party."
        )
        departures.forEach { departure ->
            val state = PartyState()
            state.accept("You have joined Leader's party!", "Owner")
            val session = state.sessionId
            state.accept(departure, "Owner")
            assertNotEquals(session, state.sessionId, departure)
            assertFalse(state.isInParty, departure)
            assertTrue(state.members.isEmpty(), departure)
            assertNull(state.partyLeader, departure)
        }
        val state = PartyState()
        state.accept("You have joined Leader's party!", "Owner")
        val session = state.sessionId
        state.reset()
        assertNotEquals(session, state.sessionId)
        assertFalse(state.isInParty)
        assertTrue(state.members.isEmpty())
        assertNull(state.partyLeader)
    }

    @Test
    fun `leadership transfer because the local player left invalidates the whole party session`() {
        val state = PartyState()
        state.accept("Party Finder > Your party has been queued!", "Owner")
        state.accept("Candidate joined the party.", "Owner")
        val session = state.sessionId

        state.accept("The party was transferred to Candidate because OWNER left", "Owner")
        assertNotEquals(session, state.sessionId)
        assertFalse(state.isInParty)
        assertTrue(state.members.isEmpty())
        assertNull(state.partyLeader)
    }

    @Test
    fun `another player leaving leadership changes leader without ending the local party session`() {
        val state = PartyState()
        state.accept("You have joined Leader's party!", "Owner")
        state.accept("Candidate joined the party.", "Owner")
        val session = state.sessionId

        state.accept("The party was transferred to [MVP+] Candidate because Leader left", "Owner")
        assertEquals(session, state.sessionId)
        assertEquals("Candidate", state.partyLeader)
        assertEquals(setOf("Owner", "Candidate"), state.members.toSet())
        assertTrue(state.isInParty)
    }

    @Test
    fun `party chat cannot spoof disband leave or leadership announcements`() {
        val state = PartyState()
        state.accept("Party Finder > Your party has been queued!", "Owner")
        state.accept("Candidate joined the party.", "Owner")
        val session = state.sessionId
        listOf(
            "Party > Candidate: You left the party.",
            "Party > Candidate: Candidate has disbanded the party!",
            "Party > Candidate: The party was transferred to Candidate by Owner"
        ).forEach { state.accept(it, "Owner") }

        assertEquals(session, state.sessionId)
        assertTrue(state.isInParty)
        assertEquals("Owner", state.partyLeader)
        assertEquals(setOf("Owner", "Candidate"), state.members.toSet())
    }
}
