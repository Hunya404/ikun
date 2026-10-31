package com.github.ikun.utils.location
import com.github.ikun.Ikun.mc
import net.minecraft.ChatFormatting
object LocationUtils {
    val onHypixel get() = mc.player?.connection?.serverBrand()?.lowercase()?.contains("hypixel") == true
    val inSkyblock get() = onHypixel && mc.level?.scoreboard?.getObjective("SBScoreboard") != null
    val inKuudra: Boolean get() {
        if (!onHypixel) return false
        val connection = mc.connection ?: return false
        // RatAddons identifies the island from the tab list's Area:/Dungeon: entry.
        for (info in connection.listedOnlinePlayers) {
            val text = ChatFormatting.stripFormatting(info.tabListDisplayName?.string ?: continue) ?: continue
            val marker = when {
                text.contains("Area:") -> "Area:"
                text.contains("Dungeon:") -> "Dungeon:"
                else -> continue
            }
            return text.substringAfter(marker).trim().contains("Kuudra", ignoreCase = true)
        }
        return false
    }
}
