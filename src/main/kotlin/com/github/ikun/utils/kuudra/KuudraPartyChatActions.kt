package com.github.ikun.utils.kuudra

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import java.util.UUID

class KuudraPartyChatActions {
    private val targets = linkedMapOf<String, Target>()

    data class Target(val id: String, val playerName: String, val generation: Long, val partySession: Long) {
        var kickRequested = false
            internal set
    }

    fun track(playerName: String, generation: Long, partySession: Long): Target {
        require(playerName.matches(Regex("[A-Za-z0-9_]{1,16}")))
        targets.entries.removeIf { it.value.playerName.equals(playerName, ignoreCase = true) }
        val target = Target(UUID.randomUUID().toString().replace("-", ""), playerName, generation, partySession)
        targets[target.id] = target
        if (targets.size > 100) targets.remove(targets.keys.first())
        return target
    }

    fun resolve(id: String, generation: Long, partySession: Long): Target? =
        targets[id]?.takeIf { it.generation == generation && it.partySession == partySession }

    fun consumeKick(
        id: String, generation: Long, partySession: Long, ownName: String,
        isLeader: Boolean, members: Collection<String>
    ): String? {
        val target = resolve(id, generation, partySession) ?: return null
        if (target.kickRequested || !isLeader || target.playerName.equals(ownName, ignoreCase = true) ||
            members.none { it.equals(ownName, ignoreCase = true) } ||
            members.none { it.equals(target.playerName, ignoreCase = true) }) return null
        target.kickRequested = true
        return "p kick ${target.playerName}"
    }

    fun retryComponent(target: Target): Component = Component.literal("Click here to retry querying ${target.playerName}").withStyle {
        it.withColor(ChatFormatting.GOLD)
            .withClickEvent(ClickEvent.RunCommand("/ikun retry ${target.id}"))
            .withHoverEvent(HoverEvent.ShowText(Component.literal("Fetch fresh stats for ${target.playerName}.")))
    }

    fun kickComponent(target: Target): Component = Component.literal("[ikun]Kick This Player From HERE!").withStyle {
        it.withColor(ChatFormatting.DARK_RED)
            .withClickEvent(ClickEvent.RunCommand("/ikun kick ${target.id}"))
            .withHoverEvent(HoverEvent.ShowText(Component.literal("Kick ${target.playerName} from this party. Party leader only.")))
    }

    fun clear() = targets.clear()
}
