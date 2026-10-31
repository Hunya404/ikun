package com.github.ikun.features.impl.kuudra

import com.github.ikun.event.impl.ChatMessageEvent
import com.github.ikun.event.impl.WorldChangeEvent
import com.github.ikun.features.Feature
import com.github.ikun.utils.kuudra.CannonCloseWindow
import com.github.ikun.utils.location.LocationUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

object AutoCannonClose: Feature(
    "Close Perk Menu within 500 ms of the Human Cannonball purchase confirmation.", name = "Auto Cannon Close"
) {
    private val window = CannonCloseWindow()
    override fun init() {
        register<ChatMessageEvent> {
            if (LocationUtils.onHypixel) window.chat(event.unformattedText, System.currentTimeMillis())
        }
        register<WorldChangeEvent> { window.reset() }
    }

    override fun tick(client: Minecraft) {
        val screen = client.screen as? AbstractContainerScreen<*> ?: return
        if (window.shouldClose(screen.title.string, System.currentTimeMillis())) {
            val player = client.player ?: return
            player.closeContainer()
            window.reset()
        }
    }

    override fun onDisable() { super.onDisable(); window.reset() }
}
