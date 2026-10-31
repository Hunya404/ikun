package com.github.ikun.features.impl.kuudra

import com.github.ikun.event.impl.WorldChangeEvent
import com.github.ikun.features.Feature
import com.github.ikun.utils.location.LocationUtils
import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items

object SupplySwap: Feature(
    "Right-click Elle's Supplies to swap to a hotbar pearl and throw it at the next tick end. Does not swap back.",
    name = "Supply Swap"
) {
    private var pendingPearlSlot = -1

    override fun init() {
        register<WorldChangeEvent> { pendingPearlSlot = -1 }
    }

    @JvmStatic
    fun beforeUseItem(player: Player, hand: InteractionHand): Boolean {
        if (!enabled || hand != InteractionHand.MAIN_HAND) return false
        val client = Minecraft.getInstance()
        if (player !== client.player || !LocationUtils.inKuudra) return false
        val stack = player.getItemInHand(hand)
        if (stack.isEmpty || !stack.hoverName.string.contains("Elle's Supplies")) return false

        val pearl = (0..8).firstOrNull { player.inventory.getItem(it).`is`(Items.ENDER_PEARL) }
        if (pearl != null) pendingPearlSlot = pearl
        // RatAddons 1.2.2 consumes the supplies click even when no hotbar pearl is available.
        return true
    }

    override fun tick(client: Minecraft) {
        val slot = pendingPearlSlot
        pendingPearlSlot = -1
        if (slot !in 0..8 || !LocationUtils.inKuudra) return
        val player = client.player ?: return
        val gameMode = client.gameMode ?: return
        if (!player.inventory.getItem(slot).`is`(Items.ENDER_PEARL)) return

        player.inventory.setSelectedSlot(slot)
        gameMode.useItem(player, InteractionHand.MAIN_HAND)
    }

    override fun onDisable() {
        super.onDisable()
        pendingPearlSlot = -1
    }
}
