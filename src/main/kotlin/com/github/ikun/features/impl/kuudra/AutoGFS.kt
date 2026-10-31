package com.github.ikun.features.impl.kuudra

import com.github.ikun.Ikun
import com.github.ikun.config.types.NumberSetting
import com.github.ikun.config.types.ToggleSetting
import com.github.ikun.event.impl.ChatMessageEvent
import com.github.ikun.event.impl.WorldChangeEvent
import com.github.ikun.features.Feature
import com.github.ikun.utils.ChatUtils
import com.github.ikun.utils.kuudra.AutoGfsTrigger
import com.github.ikun.utils.kuudra.KuudraEntryNotice
import com.github.ikun.utils.kuudra.PearlInventory
import com.github.ikun.utils.kuudra.PearlRefillOnEntry
import com.github.ikun.utils.location.AutoGfsLocation
import com.github.ikun.utils.location.LocationUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel

object AutoGFS: Feature(
    "In Kuudra, check pearls ten seconds after entry and every twenty seconds afterward, refilling to 16. Withdraw Toxic Arrow Poison when chat contains 'The Ballista is finally ready!', following RatAddons.",
    name = "AutoGFS"
) {
    private val trigger = AutoGfsTrigger()
    private val pearlRefill = PearlRefillOnEntry()
    private val entryNotice = KuudraEntryNotice()
    private var diagnosedWorld: Any? = null
    private var lastPearlStatus: PearlRefillOnEntry.Status? = null
    private val refillPearl by ToggleSetting("Refill Pearl", true)
        .withDescription("Check carried Ender Pearls ten seconds after Kuudra entry, then every twenty seconds. Withdraw only enough from sacks to reach 16")
    private val toxicAmount by NumberSetting("Toxic Amount", 16.0, 1.0, 32.0, 1.0)
        .withDescription("Withdraw 1-32 Toxic Arrow Poison whenever Kuudra chat contains 'The Ballista is finally ready!'. Requests the selected amount without checking inventory")

    override fun init() {
        register<ChatMessageEvent> {
            val client = Ikun.mc
            val world = client.level ?: return@register
            val player = client.player ?: return@register
            val message = event.unformattedText
            if (entryNotice.observe(message, player.name.string, world)) {
                Ikun.logger.info("AutoGFS recognized own Kuudra entry banner.")
            }
            if (!AutoGfsTrigger.isCue(message)) return@register
            if (!inKuudra(client, world)) {
                Ikun.logger.info("AutoGFS skipped Ballista cue: current area was not recognized as Kuudra.")
                return@register
            }
            val command = trigger.command(message, true, toxicAmount.value.toInt()) ?: return@register
            client.execute {
                if (enabled && client.level === world && inKuudra(client, world)) {
                    val currentPlayer = client.player ?: return@execute
                    currentPlayer.connection.sendCommand(command)
                    Ikun.logger.info("AutoGFS requested: {}", command)
                }
            }
        }
        register<WorldChangeEvent> {
            pearlRefill.reset()
            entryNotice.reset()
            diagnosedWorld = null
            lastPearlStatus = null
        }
    }

    override fun tick(client: Minecraft) {
        if (!refillPearl.value) return
        val world = client.level ?: return
        val player = client.player ?: return
        if (diagnosedWorld !== world) {
            diagnosedWorld = world
            lastPearlStatus = null
            Ikun.logger.info(
                "AutoGFS pearl check active: brandHypixel={}, skyblockObjective={}, areaRecognized={}, screenOpen={}, dead={}",
                LocationUtils.onHypixel, AutoGfsLocation.hasSkyBlockObjective(world.scoreboard),
                inKuudra(client, world), client.screen != null, player.isDeadOrDying
            )
        }
        if (!inKuudra(client, world)) {
            if (lastPearlStatus != PearlRefillOnEntry.Status.OUTSIDE) {
                lastPearlStatus = PearlRefillOnEntry.Status.OUTSIDE
                Ikun.logger.info("AutoGFS pearl check: Kuudra area not recognized; waiting for scoreboard, TAB or own entry banner.")
            }
            return
        }
        val command = pearlRefill.command(
            world, true,
            { PearlInventory.count(player.inventory.nonEquipmentItems + player.offhandItem) },
            client.screen == null && !player.isDeadOrDying,
            System.nanoTime() / 1_000_000
        )
        if (lastPearlStatus != pearlRefill.status) {
            lastPearlStatus = pearlRefill.status
            Ikun.logger.info("AutoGFS pearl check: status={}, pearls={}, screenOpen={}, dead={}",
                pearlRefill.status, pearlRefill.pearlCount, client.screen != null, player.isDeadOrDying)
        }
        if (command == null) return
        val pearls = pearlRefill.pearlCount ?: return
        player.connection.sendCommand(command)
        Ikun.logger.info("AutoGFS requested: {}", command)
        ChatUtils.modMessage("&bAutoGFS &7> &aPearls: $pearls/16; requested ${16 - pearls}.")
    }

    private fun inKuudra(client: Minecraft, world: ClientLevel) =
        AutoGfsLocation.inKuudra(client) ||
            (entryNotice.matches(world) && AutoGfsLocation.hasSkyBlockObjective(world.scoreboard))
}
