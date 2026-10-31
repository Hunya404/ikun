package com.github.ikun.utils.location

import com.github.ikun.utils.ChatUtils.removeFormatting
import com.github.ikun.utils.kuudra.AutoGfsTrigger
import net.minecraft.client.Minecraft
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard

object AutoGfsLocation {
    fun inKuudra(client: Minecraft): Boolean {
        val scoreboard = client.level?.scoreboard ?: return false
        if (!hasSkyBlockObjective(scoreboard)) return false
        return hasKuudraSidebar(scoreboard) || client.player?.connection?.onlinePlayers?.any {
            AutoGfsTrigger.isKuudraTabLine(it.tabListDisplayName?.string ?: it.profile.name)
        } == true
    }

    fun hasSkyBlockObjective(scoreboard: Scoreboard): Boolean =
        scoreboard.getObjective("SBScoreboard")?.displayName?.string?.removeFormatting()
            ?.contains("SKYBLOCK", ignoreCase = true) == true

    internal fun hasKuudraSidebar(scoreboard: Scoreboard): Boolean {
        val objectives = listOfNotNull(
            scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR),
            scoreboard.getObjective("SBScoreboard")
        ).distinct()
        return objectives.any { objective ->
            scoreboard.listPlayerScores(objective).filterNot { it.isHidden }.sortedByDescending { it.value() }
                .take(15).any { score ->
                    val text = PlayerTeam.formatNameForTeam(
                        scoreboard.getPlayersTeam(score.owner()), score.ownerName()
                    ).string
                    AutoGfsTrigger.isKuudraSidebarLine(text)
                }
        }
    }
}
