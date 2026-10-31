package com.github.ikun.features.impl.general

import com.github.ikun.Ikun.mc
import com.github.ikun.Ikun.scope
import com.github.ikun.config.types.DropdownSetting
import com.github.ikun.config.types.TextInputSetting
import com.github.ikun.config.types.ToggleSetting
import com.github.ikun.event.impl.ChatMessageEvent
import com.github.ikun.event.impl.WorldChangeEvent
import com.github.ikun.features.Feature
import com.github.ikun.utils.ChatUtils
import com.github.ikun.utils.PartyUtils
import com.github.ikun.utils.kuudra.*
import com.github.ikun.utils.location.LocationUtils
import com.github.ikun.utils.network.WebUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import kotlin.coroutines.resume

object KuudraParty: Feature("Shows Kuudra party join stats and optionally kicks players who fail selected requirements.", name = "KuudraParty", toggled = true) {
    private val joinStats by ToggleSetting("Join Stats", true).section("Player Stats")
    private val currentTier by DropdownSetting("Current Tier", 4, KuudraPartyTier.entries.map { it.displayName })
        .withDescription("Only show completions for this tier. The separate Kuudra T5 AutoKick requirement still checks Infernal.")
    private val autoKick by ToggleSetting("AutoKick", false).section("AutoKick")
        .withDescription("As party leader, kick players who fail enabled checks. Missing completions count as 0; API controls kicks for omitted gear data.")
    private val api by ToggleSetting("API", false).showIf { autoKick.value }
        .withDescription("Recheck missing inventory, backpack or ender chest data before kicking. Two successful queries must omit the same required data. Failed rechecks never kick. Kick first, then send No API Player.")
    private val checkT5 by ToggleSetting("Check Kuudra T5", false).showIf { autoKick.value }
    private val minimumT5 by TextInputSetting("Kuudra T5", "0").showIf { autoKick.value && checkT5.value }
        .withDescription("Minimum Infernal completions. Enter a non-negative whole number.")
    private val rcmTerror by ToggleSetting("RCM Terror", false).showIf { autoKick.value }
        .withDescription("Require any one owned Necrotic Infernal Terror piece, including stored armor sets.")
    private val tap by ToggleSetting("TAP", false).showIf { autoKick.value }
        .withDescription("Require Toxic Arrow Poison in inventory, backpacks, ender chest, quiver or sacks.")
    private val noHyp by ToggleSetting("No Hyp", false).showIf { autoKick.value }
        .withDescription("Require Hyperion in inventory, backpacks or ender chest.")
    private const val QUERY_ENDPOINT = "https://newspapers-characteristics-repeat-amongst.trycloudflare.com"

    private val queries = mutableMapOf<String, Job>()
    private val manualKicks = mutableMapOf<String, Job>()
    private val chatActions = KuudraPartyChatActions()
    private val recentJoins = mutableMapOf<String, Long>()
    private val kickMutex = Mutex()
    private var generation = 0L
    private var lastPartyLookup = 0L

    override fun init() {
        register<WorldChangeEvent> { cancelQueries() }
        register<ChatMessageEvent> {
            if (!LocationUtils.onHypixel || !LocationUtils.inSkyblock) return@register
            if (!joinStats.value && !autoKick.value) return@register
            val name = KuudraPartyRules.joinedPlayer(event.unformattedText) ?: return@register
            if (name.equals(mc.user.name, true)) return@register
            val key = name.lowercase()
            val now = System.currentTimeMillis()
            if (queries[key]?.isActive == true || now - (recentJoins[key] ?: 0) < 5_000) return@register
            recentJoins.entries.removeIf { now - it.value > 60_000 }
            recentJoins[key] = now
            if (autoKick.value && PartyUtils.partyLeader == null && now - lastPartyLookup > 5_000) {
                lastPartyLookup = now
                ChatUtils.sendCommand("p list")
            }
            queryPlayer(chatActions.track(name, generation, PartyUtils.sessionId))
        }
    }

    private fun queryPlayer(target: KuudraPartyChatActions.Target, refresh: Boolean = false, manual: Boolean = false) {
        val name = target.playerName
        val key = name.lowercase()
        val expectedGeneration = target.generation
        val expectedPartySession = target.partySession
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val data = withTimeout(45_000) {
                    val suffix = if (refresh) "?refresh=1" else ""
                    WebUtils.getAs<KuudraPartyData>("$QUERY_ENDPOINT/v1/kuudra/$name$suffix").getOrThrow()
                }
                require(data.playerName.equals(name, true)) { "Query returned a different player" }
                mc.execute {
                    if (resolveTarget(target.id) === target && (joinStats.value || manual)) {
                        printStats(data)
                        printActions(target)
                    }
                }
                // A cached profile can arrive before the /p list response.
                for (attempt in 0 until 20) {
                    if (onClient { !enabled || !autoKick.value || generation != expectedGeneration || PartyUtils.partyLeader != null }) break
                    delay(100)
                }
                kickMutex.withLock {
                    val snapshot = onClient {
                        if (!canKick(name, expectedGeneration, expectedPartySession)) {
                            if (enabled && autoKick.value && generation == expectedGeneration) {
                                val reason = when {
                                    PartyUtils.partyLeader == null -> "party leader not identified; use /p list to refresh"
                                    !PartyUtils.isLeader() -> "party leader is ${PartyUtils.partyLeader}"
                                    PartyUtils.members.none { it.equals(name, true) } -> "player is no longer tracked in this party"
                                    else -> "game or party state changed"
                                }
                                ChatUtils.modMessage("&eKuudraParty > $name: AutoKick skipped ($reason).")
                            }
                            return@onClient null
                        }
                        requirements()
                    } ?: return@withLock
                    if (!KuudraPartyRules.isFreshForKick(data.fetchedAt, System.currentTimeMillis())) {
                        ChatUtils.modMessage("&eKuudraParty > $name: cached data is too old for AutoKick.")
                        return@withLock
                    }
                    var decision = KuudraPartyRules.evaluate(data, snapshot)
                    if (decision.apiBlocked) {
                        ChatUtils.modMessage("&eKuudraParty > $name: API data missing; rechecking.")
                        delay(1_500)
                        if (!onClient { canKick(name, expectedGeneration, expectedPartySession) && requirements() == snapshot }) return@withLock
                        // A second copy of our 30-second cache is not independent evidence.
                        val rechecked = withTimeout(45_000) {
                            WebUtils.getAs<KuudraPartyData>("$QUERY_ENDPOINT/v1/kuudra/$name?refresh=1").getOrThrow()
                        }
                        if (!KuudraPartyRules.isFreshForKick(rechecked.fetchedAt, System.currentTimeMillis())) {
                            ChatUtils.modMessage("&eKuudraParty > $name: API recheck is stale; no kick performed.")
                            return@withLock
                        }
                        decision = KuudraPartyRules.confirmApiFailure(data, rechecked, snapshot)
                    }
                    if (decision.unknown.isNotEmpty()) {
                        ChatUtils.modMessage("&eKuudraParty > $name: cannot check ${decision.unknown.joinToString(", ")} (API data unavailable).")
                    }
                    if (decision.reasons.isEmpty()) return@withLock
                    // Confirmed API failures kick first; their announcement follows the kick.
                    if (!decision.apiBlocked) delay(650)
                    for ((index, command) in decision.commands(name).withIndex()) {
                        if (index > 0) delay(650)
                        val sent = onClient {
                            // After our API kick the target may already have left the member list.
                            val allowed = if (decision.apiBlocked && index > 0) canManageParty(expectedGeneration, expectedPartySession)
                            else canKick(name, expectedGeneration, expectedPartySession)
                            if (!allowed || requirements() != snapshot) false
                            else {
                                mc.player?.connection?.sendCommand(command)
                                true
                            }
                        }
                        if (!sent) break
                    }
                }
            }
            catch (e: CancellationException) {
                if (e !is TimeoutCancellationException) throw e
                mc.execute {
                    if (resolveTarget(target.id) === target) {
                        ChatUtils.modMessage("&cKuudraParty > Query timed out for $name; no kick performed.")
                        printActions(target)
                    }
                }
            }
            catch (e: Exception) {
                mc.execute {
                    if (resolveTarget(target.id) === target) {
                        // Network errors and proxy HTML can contain the service address.
                        val status = Regex("^HTTP (\\d{3})\\b").find(e.message.orEmpty())?.groupValues?.get(1)
                        val reason = status?.let { "HTTP $it" } ?: "connection or response error"
                        ChatUtils.modMessage("&cKuudraParty > Failed to query $name: $reason; no kick performed.")
                        printActions(target)
                    }
                }
            }
            finally {
                val finished = currentCoroutineContext()[Job]
                mc.execute { if (queries[key] === finished) queries.remove(key) }
            }
        }
        queries[key] = job
        job.start()
    }

    private fun resolveTarget(id: String): KuudraPartyChatActions.Target? {
        if (!enabled || !LocationUtils.onHypixel || !LocationUtils.inSkyblock || mc.player == null) return null
        return chatActions.resolve(id, generation, PartyUtils.sessionId)
    }

    private fun expiredAction() {
        ChatUtils.modMessage("&eKuudraParty > This player action expired; use a current Party Finder message.")
    }

    private fun printActions(target: KuudraPartyChatActions.Target) {
        ChatUtils.chat(chatActions.retryComponent(target))
        ChatUtils.chat(chatActions.kickComponent(target))
    }

    fun retryPlayer(id: String) {
        val target = resolveTarget(id) ?: return expiredAction()
        if (queries[target.playerName.lowercase()]?.isActive == true) {
            ChatUtils.modMessage("&eKuudraParty > A query for ${target.playerName} is already in progress.")
            return
        }
        ChatUtils.modMessage("&6KuudraParty > Fetching fresh stats for ${target.playerName}...")
        queryPlayer(target, refresh = true, manual = true)
    }

    fun kickPlayer(id: String) {
        val target = resolveTarget(id) ?: return expiredAction()
        if (target.kickRequested || manualKicks[id]?.isActive == true) {
            ChatUtils.modMessage("&eKuudraParty > A kick request for ${target.playerName} is already in progress.")
            return
        }
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                if (onClient { PartyUtils.partyLeader == null }) {
                    onClient { if (resolveTarget(id) === target) ChatUtils.sendCommand("p list") }
                    for (attempt in 0 until 20) {
                        delay(100)
                        if (onClient { resolveTarget(id) !== target || PartyUtils.partyLeader != null }) break
                    }
                }
                onClient {
                    if (resolveTarget(id) !== target) {
                        expiredAction()
                        return@onClient
                    }
                    val command = chatActions.consumeKick(id, generation, PartyUtils.sessionId,
                        mc.user.name, PartyUtils.isLeader(), PartyUtils.members)
                    if (command == null) {
                        val reason = when {
                            PartyUtils.partyLeader == null -> "party leader could not be identified"
                            !PartyUtils.isLeader() -> "you are not the party leader"
                            else -> "the player is no longer in this party"
                        }
                        ChatUtils.modMessage("&eKuudraParty > Cannot kick ${target.playerName}: $reason.")
                    } else {
                        queries[target.playerName.lowercase()]?.cancel()
                        mc.player?.connection?.sendCommand(command)
                        ChatUtils.modMessage("&4KuudraParty > Requested kick for ${target.playerName}.")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onClient {
                    if (resolveTarget(id) === target) {
                        ChatUtils.modMessage("&cKuudraParty > Could not send the kick request for ${target.playerName}.")
                    }
                }
            } finally {
                val finished = currentCoroutineContext()[Job]
                mc.execute { if (manualKicks[id] === finished) manualKicks.remove(id) }
            }
        }
        manualKicks[id] = job
        job.start()
    }

    private fun requirements(): KuudraPartyRequirements {
        val minimum = if (checkT5.value) KuudraPartyRules.parseMinimum(minimumT5.value) else null
        if (checkT5.value && minimum == null) {
            ChatUtils.modMessage("&eKuudraParty > Invalid Kuudra T5 minimum; that check is skipped.")
        }
        return KuudraPartyRequirements(minimum, rcmTerror.value, tap.value, noHyp.value, api.value)
    }

    private fun canManageParty(expectedGeneration: Long, expectedPartySession: Long) = enabled && autoKick.value &&
        generation == expectedGeneration && PartyUtils.sessionId == expectedPartySession && LocationUtils.onHypixel && LocationUtils.inSkyblock &&
        mc.player != null && PartyUtils.isLeader()

    private fun canKick(name: String, expectedGeneration: Long, expectedPartySession: Long) = canManageParty(expectedGeneration, expectedPartySession) &&
        PartyUtils.members.any { it.equals(name, true) }

    private fun printStats(data: KuudraPartyData) {
        fun state(value: Boolean?) = when (value) { true -> "&aYes"; false -> "&cNo"; null -> "&eUnknown" }
        val tier = KuudraPartyTier.entries.getOrElse(currentTier.value) { KuudraPartyTier.INFERNAL }
        ChatUtils.chat(Component.literal("§bKuudraParty §7> §f${data.playerName} §7[${data.profileName}]").withStyle {
            it.withHoverEvent(HoverEvent.ShowText(Component.literal(
                "Equipment\n" + data.equipment.joinToString("\n").ifEmpty { "None returned" }
            )))
        })
        ChatUtils.chat("  &6${tier.label}: &f${KuudraPartyRules.completionCount(data, tier)}")
        ChatUtils.chat("  &6Golden Dragon: ${if (KuudraPartyRules.hasGoldenDragon(data)) "Yes" else "No"}")
        ChatUtils.chat("  &bRCM: ${state(data.hasNecroticInfernalTerror)} &7| &bTAP: ${state(data.hasToxicArrowPoison)}")
        ChatUtils.chat("  &bHyp: ${state(data.hasHyperion)} &7| &bTerm: ${state(data.hasTerminator)}")
        ChatUtils.chat("  &bOpen Shop: ${state(data.hasOpenShop)}")
    }

    private suspend fun <T> onClient(block: () -> T): T = suspendCancellableCoroutine { cont ->
        mc.execute {
            if (!cont.isActive) return@execute
            try { cont.resume(block()) }
            catch (e: Exception) { cont.resumeWith(Result.failure(e)) }
        }
    }

    private fun cancelQueries() {
        generation++
        queries.values.forEach { it.cancel() }
        queries.clear()
        manualKicks.values.forEach { it.cancel() }
        manualKicks.clear()
        chatActions.clear()
        recentJoins.clear()
        lastPartyLookup = 0L
    }

    override fun onDisable() {
        super.onDisable()
        cancelQueries()
    }
}
