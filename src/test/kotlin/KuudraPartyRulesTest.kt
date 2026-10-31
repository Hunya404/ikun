import com.github.ikun.utils.kuudra.*
import kotlin.test.*

class KuudraPartyRulesTest {
    private fun data(t5: Long? = 10, terror: Boolean? = true, tap: Boolean? = true, hyp: Boolean? = true) =
        KuudraPartyData("hu_nya", "Coconut", 0, mapOf("infernal" to t5),
            hasNecroticInfernalTerror = terror, hasToxicArrowPoison = tap, hasHyperion = hyp)

    @Test
    fun `recognizes Kuudra and optional ranks without triggering on dungeon or player chat`() {
        assertEquals("hu_nya", KuudraPartyRules.joinedPlayer("Party Finder > hu_nya joined the group! (Combat Level 52)"))
        assertEquals("hu_nya", KuudraPartyRules.joinedPlayer("Party Finder > [MVP+] hu_nya joined the group! (Combat Level 52)"))
        assertNull(KuudraPartyRules.joinedPlayer("Party Finder > hu_nya joined the dungeon group! (Tank Level 42)"))
        assertNull(KuudraPartyRules.joinedPlayer("Party > hu_nya: Party Finder > test joined the group! (Combat Level 52)"))
        assertNull(KuudraPartyRules.joinedPlayer("Party Finder > bad/name joined the group! (Combat Level 52)"))
    }

    @Test
    fun `T5 uses a strict lower than comparison and rejects invalid input`() {
        assertTrue(KuudraPartyRules.evaluate(data(), KuudraPartyRequirements(10)).reasons.isEmpty())
        assertEquals(listOf("Kuudra T5: 10/11"), KuudraPartyRules.evaluate(data(), KuudraPartyRequirements(11)).reasons)
        assertEquals(100L, KuudraPartyRules.parseMinimum(" 100 "))
        listOf("", "-1", "1.5", "NaN", "9999999999999999999999999").forEach {
            assertNull(KuudraPartyRules.parseMinimum(it))
        }
    }

    @Test
    fun `missing completions count as zero while unknown item checks are skipped`() {
        val decision = KuudraPartyRules.evaluate(data(t5 = null, terror = null, tap = false, hyp = null),
            KuudraPartyRequirements(100, rcmTerror = true, tap = true, noHyp = true))
        assertEquals(listOf("Kuudra T5: 0/100", "No Toxic Arrow Poison!"), decision.reasons)
        assertEquals(listOf("RCM Terror", "Hyperion"), decision.unknown)
    }

    @Test
    fun `display tiers map correctly and missing or negative counts become zero`() {
        val all = data().copy(completions = mapOf("none" to 1L, "hot" to 2L, "burning" to 3L, "fiery" to 4L, "infernal" to 5L))
        assertEquals(listOf("Basic (T1)", "Hot (T2)", "Burning (T3)", "Fiery (T4)", "Infernal (T5)"), KuudraPartyTier.entries.map { it.label })
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), KuudraPartyTier.entries.map { KuudraPartyRules.completionCount(all, it) })
        val missing = data().copy(completions = mapOf("none" to null, "hot" to -1L))
        KuudraPartyTier.entries.forEach { assertEquals(0L, KuudraPartyRules.completionCount(missing, it)) }
        assertTrue(KuudraPartyRules.evaluate(missing, KuudraPartyRequirements(0)).reasons.isEmpty())
    }

    @Test
    fun `Golden Dragon ownership includes inactive pets and ignores other dragons`() {
        listOf("LEGENDARY GOLDEN DRAGON", "LEGENDARY GOLDEN DRAGON (active)", "MYTHIC GOLDEN DRAGON").forEach {
            assertTrue(KuudraPartyRules.hasGoldenDragon(data().copy(pets = listOf(it))))
        }
        assertFalse(KuudraPartyRules.hasGoldenDragon(data().copy(pets = listOf("LEGENDARY ENDER DRAGON", "LEGENDARY NOT GOLDEN DRAGON"))))
        assertFalse(KuudraPartyRules.hasGoldenDragon(data().copy(petsAvailable = false)))
    }

    @Test
    fun `disabled rules never kick and all failures share one announcement`() {
        val missing = data(0, false, false, false)
        assertTrue(KuudraPartyRules.evaluate(missing, KuudraPartyRequirements()).reasons.isEmpty())
        val decision = KuudraPartyRules.evaluate(missing, KuudraPartyRequirements(1, true, true, true))
        assertEquals(4, decision.reasons.size)
        assertEquals("hu_nya: Kuudra T5: 0/1; No Necrotic Infernal Terror!; No Toxic Arrow Poison!; No Hyp!", decision.partyMessage("hu_nya"))
        assertEquals("No Hyp!", KuudraPartyRules.evaluate(missing, KuudraPartyRequirements(noHyp = true)).partyMessage("hu_nya"))
    }

    @Test
    fun `API gate is opt in and takes priority with kick before announcement`() {
        val blocked = data(hyp = null).copy(unavailableApiChecks = listOf("Inventory", "Hyperion", "Terminator"))
        assertFalse(KuudraPartyRules.evaluate(blocked, KuudraPartyRequirements()).apiBlocked)
        val decision = KuudraPartyRules.evaluate(blocked, KuudraPartyRequirements(100, true, true, true, requireApi = true))
        assertTrue(decision.apiBlocked)
        assertEquals(listOf("No API Player"), decision.reasons)
        assertTrue(decision.unknown.isEmpty())
        assertEquals(listOf("p kick hu_nya", "pc No API Player"), decision.commands("hu_nya"))
    }

    @Test
    fun `API gate checks selected gear scopes and leaves parsing unknowns alone`() {
        val sacksPrivate = data(tap = null).copy(unavailableApiChecks = listOf("TAP"))
        assertFalse(KuudraPartyRules.evaluate(sacksPrivate, KuudraPartyRequirements(requireApi = true)).apiBlocked)
        assertTrue(KuudraPartyRules.evaluate(sacksPrivate, KuudraPartyRequirements(tap = true, requireApi = true)).apiBlocked)
        val armorPrivate = data(terror = null).copy(unavailableApiChecks = listOf("RCM Terror"))
        assertFalse(KuudraPartyRules.evaluate(armorPrivate, KuudraPartyRequirements(requireApi = true)).apiBlocked)
        assertTrue(KuudraPartyRules.evaluate(armorPrivate, KuudraPartyRequirements(rcmTerror = true, requireApi = true)).apiBlocked)
        val malformed = data(hyp = null, tap = null).copy(hasTerminator = null)
        val decision = KuudraPartyRules.evaluate(malformed, KuudraPartyRequirements(tap = true, noHyp = true, requireApi = true))
        assertFalse(decision.apiBlocked)
        assertTrue(decision.commands("hu_nya").isEmpty())
        assertEquals(listOf("TAP", "Hyperion"), decision.unknown)
        assertFalse(KuudraPartyRules.evaluate(data().copy(hasTerminator = false), KuudraPartyRequirements(requireApi = true)).apiBlocked)
        assertEquals(listOf("pc No Hyp!", "p kick hu_nya"),
            KuudraPartyRules.evaluate(data(hyp = false), KuudraPartyRequirements(noHyp = true)).commands("hu_nya"))
    }

    @Test
    fun `API kick needs the same missing scope in two fresh successful queries`() {
        val first = data(hyp = null).copy(fetchedAt = 100, unavailableApiChecks = listOf("Hyperion"))
        val requirements = KuudraPartyRequirements(requireApi = true)
        assertTrue(KuudraPartyRules.confirmApiFailure(first, first.copy(fetchedAt = 200), requirements).apiBlocked)
        val recovered = first.copy(fetchedAt = 200, hasHyperion = true, unavailableApiChecks = emptyList())
        assertTrue(KuudraPartyRules.confirmApiFailure(first, recovered, requirements).commands("hu_nya").isEmpty())
        val different = first.copy(fetchedAt = 200, unavailableApiChecks = listOf("Terminator"))
        assertFalse(KuudraPartyRules.confirmApiFailure(first, different, requirements).apiBlocked)
    }

    @Test
    fun `failed cached and mismatched rechecks never produce kick commands`() {
        val first = data().copy(fetchedAt = 100, unavailableApiChecks = listOf("Inventory"))
        val requirements = KuudraPartyRequirements(100, true, true, true, requireApi = true)
        listOf(null, first, first.copy(fetchedAt = 99), first.copy(fetchedAt = 200, playerName = "someone_else"),
            first.copy(fetchedAt = 200, profileName = "Apple")).forEach {
            val result = KuudraPartyRules.confirmApiFailure(first, it, requirements)
            assertTrue(result.commands("hu_nya").isEmpty())
            assertFalse(result.apiBlocked)
        }
    }

    @Test
    fun `recovered API uses fresh gear and completion data for ordinary checks`() {
        val first = data(hyp = null).copy(fetchedAt = 100, unavailableApiChecks = listOf("Hyperion"))
        val rechecked = data(t5 = 200, hyp = false).copy(fetchedAt = 200)
        val result = KuudraPartyRules.confirmApiFailure(first, rechecked,
            KuudraPartyRequirements(100, noHyp = true, requireApi = true))
        assertFalse(result.apiBlocked)
        assertEquals(listOf("pc No Hyp!", "p kick hu_nya"), result.commands("hu_nya"))
    }

    @Test
    fun `Open Shop display does not introduce a new kick requirement`() {
        listOf(true, false, null).forEach { shop ->
            val player = data().copy(hasOpenShop = shop, unavailableApiChecks = listOf("Open Shop"))
            assertTrue(KuudraPartyRules.evaluate(player, KuudraPartyRequirements(requireApi = true)).commands("hu_nya").isEmpty())
        }
    }

    @Test
    fun `kick data freshness accepts only the sixty second past and five second future window`() {
        val now = 100_000L
        assertFalse(KuudraPartyRules.isFreshForKick(39_999, now))
        assertTrue(KuudraPartyRules.isFreshForKick(40_000, now))
        assertTrue(KuudraPartyRules.isFreshForKick(now, now))
        assertTrue(KuudraPartyRules.isFreshForKick(105_000, now))
        assertFalse(KuudraPartyRules.isFreshForKick(105_001, now))
        assertFalse(KuudraPartyRules.isFreshForKick(0, now))
        assertFalse(KuudraPartyRules.isFreshForKick(-1, now))
        assertFalse(KuudraPartyRules.isFreshForKick(1, 0))
        assertFalse(KuudraPartyRules.isFreshForKick(1, -1))
    }

    @Test
    fun `ender chest availability alone does not introduce an extra API kick scope`() {
        val storagePrivate = data(hyp = null).copy(unavailableApiChecks = listOf("Ender Chest"))
        val decision = KuudraPartyRules.evaluate(storagePrivate, KuudraPartyRequirements(noHyp = true, requireApi = true))
        assertFalse(decision.apiBlocked)
        assertTrue(decision.commands("hu_nya").isEmpty())
        assertEquals(listOf("Hyperion"), decision.unknown)

        val inventoryUnavailable = storagePrivate.copy(unavailableApiChecks = listOf("Ender Chest", "Hyperion"))
        assertEquals(listOf("p kick hu_nya", "pc No API Player"),
            KuudraPartyRules.evaluate(inventoryUnavailable, KuudraPartyRequirements(requireApi = true)).commands("hu_nya"))
    }
}

