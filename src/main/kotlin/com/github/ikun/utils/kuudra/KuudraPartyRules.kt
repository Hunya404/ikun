package com.github.ikun.utils.kuudra

import kotlinx.serialization.Serializable

@Serializable
data class KuudraPartyData(
    val playerName: String,
    val profileName: String,
    val fetchedAt: Long,
    val completions: Map<String, Long?> = emptyMap(),
    val pets: List<String> = emptyList(),
    val petsAvailable: Boolean = false,
    val equipment: List<String> = emptyList(),
    val hasNecroticInfernalTerror: Boolean? = null,
    val hasToxicArrowPoison: Boolean? = null,
    val hasHyperion: Boolean? = null,
    val hasTerminator: Boolean? = null,
    val hasOpenShop: Boolean? = null,
    val unavailableApiChecks: List<String> = emptyList(),
)

data class KuudraPartyRequirements(
    val minimumT5: Long? = null,
    val rcmTerror: Boolean = false,
    val tap: Boolean = false,
    val noHyp: Boolean = false,
    val requireApi: Boolean = false,
)

enum class KuudraPartyTier(val displayName: String, val apiKey: String) {
    BASIC("Basic", "none"), HOT("Hot", "hot"), BURNING("Burning", "burning"),
    FIERY("Fiery", "fiery"), INFERNAL("Infernal", "infernal");

    val label get() = "$displayName (T${ordinal + 1})"
}

data class KuudraPartyDecision(val reasons: List<String>, val unknown: List<String>, val apiBlocked: Boolean = false) {
    fun partyMessage(name: String) = if (apiBlocked) "No API Player"
    else if (reasons == listOf("No Hyp!")) "No Hyp!"
    else "$name: ${reasons.joinToString("; ")}"

    fun commands(name: String): List<String> = when {
        reasons.isEmpty() -> emptyList()
        apiBlocked -> listOf("p kick $name", "pc No API Player")
        else -> listOf("pc ${partyMessage(name)}", "p kick $name")
    }
}

object KuudraPartyRules {
    private val joinRegex = Regex("""^Party Finder > (?:\[[^]]+]\s+)?([A-Za-z0-9_]{1,16}) joined the group! \(Combat Level (\d+)\)$""")
    private val goldenDragonRegex = Regex("""^(?:COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|UNKNOWN) GOLDEN DRAGON(?: \(active\))?$""", RegexOption.IGNORE_CASE)

    fun isFreshForKick(fetchedAt: Long, now: Long): Boolean =
        fetchedAt > 0 && now > 0 && fetchedAt >= now - 60_000 && fetchedAt <= now + 5_000

    fun joinedPlayer(message: String): String? = joinRegex.matchEntire(message)?.groupValues?.get(1)

    fun parseMinimum(value: String): Long? = value.trim().takeIf { it.matches(Regex("[0-9]+")) }?.toLongOrNull()

    fun completionCount(data: KuudraPartyData, tier: KuudraPartyTier): Long =
        (data.completions[tier.apiKey] ?: 0L).coerceAtLeast(0L)

    fun hasGoldenDragon(data: KuudraPartyData) = data.pets.any(goldenDragonRegex::matches)

    fun confirmApiFailure(first: KuudraPartyData, rechecked: KuudraPartyData?, requirements: KuudraPartyRequirements): KuudraPartyDecision {
        if (rechecked == null || !first.playerName.equals(rechecked.playerName, true) ||
            first.profileName != rechecked.profileName || rechecked.fetchedAt <= first.fetchedAt) {
            return KuudraPartyDecision(emptyList(), listOf("API recheck"))
        }
        val persistent = first.unavailableApiChecks.intersect(rechecked.unavailableApiChecks.toSet())
        return evaluate(rechecked.copy(unavailableApiChecks = persistent.toList()), requirements)
    }

    fun evaluate(data: KuudraPartyData, requirements: KuudraPartyRequirements): KuudraPartyDecision {
        val requiredApiChecks = mutableSetOf("Inventory", "Hyperion", "Terminator")
        if (requirements.rcmTerror) requiredApiChecks.add("RCM Terror")
        if (requirements.tap) requiredApiChecks.add("TAP")
        if (requirements.requireApi && data.unavailableApiChecks.any { it in requiredApiChecks }) {
            return KuudraPartyDecision(listOf("No API Player"), emptyList(), apiBlocked = true)
        }
        val reasons = mutableListOf<String>()
        val unknown = mutableListOf<String>()
        requirements.minimumT5?.let { minimum ->
            val count = completionCount(data, KuudraPartyTier.INFERNAL)
            if (count < minimum) reasons.add("Kuudra T5: $count/$minimum")
        }
        fun check(enabled: Boolean, present: Boolean?, label: String, reason: String) {
            if (!enabled) return
            when (present) {
                null -> unknown.add(label)
                false -> reasons.add(reason)
                true -> Unit
            }
        }
        check(requirements.rcmTerror, data.hasNecroticInfernalTerror, "RCM Terror", "No Necrotic Infernal Terror!")
        check(requirements.tap, data.hasToxicArrowPoison, "TAP", "No Toxic Arrow Poison!")
        check(requirements.noHyp, data.hasHyperion, "Hyperion", "No Hyp!")
        return KuudraPartyDecision(reasons, unknown)
    }
}
