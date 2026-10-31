// SPDX-License-Identifier: GPL-3.0-only
// Rotation-only adaptation of RatTokens' RatAddons 2.0.1.
package com.github.ikun.features.impl.cheat

import com.github.ikun.Ikun.mc
import com.github.ikun.config.types.NumberSetting
import com.github.ikun.event.EventBus
import com.github.ikun.event.impl.ChatMessageEvent
import com.github.ikun.event.impl.WorldChangeEvent
import com.github.ikun.features.Feature
import com.github.ikun.ui.clickgui.enums.CategoryType
import com.github.ikun.utils.ChatUtils
import com.github.ikun.utils.kuudra.RendPhaseState
import com.github.ikun.utils.kuudra.RendRotationQueue
import com.github.ikun.utils.kuudra.RendRotationRules
import com.github.ikun.utils.rotation.RatHumanRotation
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.MagmaCube
import java.util.concurrent.ThreadLocalRandom

object RendRotate : Feature(
    "RatAddons Rend rotation only: turn after the Kuudra Stun/DPS entry teleport using the original smooth rotation algorithm.",
    name = "Rend Rotate", category = CategoryType.CHEAT
) {
    private val delay by NumberSetting("Rotation Delay", 5.0, 1.0, 20.0, 1.0, " t")
    private val yawMin by NumberSetting("Yaw Offset Min", 0.0, -10.0, 10.0, 0.1, "°")
    private val yawMax by NumberSetting("Yaw Offset Max", 0.0, -10.0, 10.0, 0.1, "°")
    private val pitchMin by NumberSetting("Pitch Offset Min", 0.0, -10.0, 10.0, 0.1, "°")
    private val pitchMax by NumberSetting("Pitch Offset Max", 0.0, -10.0, 10.0, 0.1, "°")
    private val frontMin by NumberSetting("Front Min", 85.0, 50.0, 500.0, 1.0)
        .withDescription("Front rotation speed minimum, in degrees per second")
    private val frontMax by NumberSetting("Front Max", 145.0, 50.0, 500.0, 1.0)
    private val rightMin by NumberSetting("Right Min", 235.0, 50.0, 500.0, 1.0)
    private val rightMax by NumberSetting("Right Max", 295.0, 50.0, 500.0, 1.0)
    private val leftMin by NumberSetting("Left Min", 235.0, 50.0, 500.0, 1.0)
    private val leftMax by NumberSetting("Left Max", 295.0, 50.0, 500.0, 1.0)
    private val backMin by NumberSetting("Back Min", 285.0, 50.0, 500.0, 1.0)
    private val backMax by NumberSetting("Back Max", 345.0, 50.0, 500.0, 1.0)
    private val phase = RendPhaseState()
    private val pending = RendRotationQueue()
    private var boss: MagmaCube? = null

    override fun init() {
        // RatAddons' KuudraState keeps tracking the encounter while the macro is off.
        EventBus.register<ChatMessageEvent> {
            phase.observeChat(event.unformattedText, mc.player?.name?.string)
        }
        EventBus.register<WorldChangeEvent> {
            phase.reset()
            boss = null
            stopRotation()
        }
        ClientTickEvents.END_CLIENT_TICK.register { client -> updateContext(client) }
        ClientTickEvents.START_CLIENT_TICK.register {
            if (enabled) repeat(pending.tick()) { startRotation() }
        }
    }

    private fun updateContext(client: Minecraft) {
        val world = client.level ?: return
        val player = client.player ?: return
        phase.observeHeight(player.y)
        client.connection?.listedOnlinePlayers?.firstOrNull { info ->
            phase.observeTab(info.tabListDisplayName?.string ?: info.profile.name)
        }
        boss = null
        if (phase.inKuudra || client.isLocalServer) {
            for (entity in world.entitiesForRendering()) {
                if (entity is MagmaCube && RendRotationRules.isBoss(entity.isAlive, entity.size,
                        entity.getAttributeBaseValue(Attributes.MAX_HEALTH))) {
                    boss = entity
                    break
                }
            }
        }
    }

    fun onPlayerInfo(packet: ClientboundPlayerInfoUpdatePacket) {
        packet.entries().firstOrNull { entry ->
            entry.displayName()?.let { phase.observeTab(it.string) } == true
        }
    }

    fun onPositionChange(packet: ClientboundPlayerPositionPacket) {
        if (mc.player == null || mc.level == null) return
        if (phase.canTrigger(enabled, packet.change().position(), mc.isLocalServer)) {
            pending.schedule(delay.value.toInt())
        }
    }

    private fun startRotation() {
        if (!enabled || mc.player == null || mc.level == null) return
        val position = boss?.position() ?: return
        val direction = RendRotationRules.direction(position)
        if (direction == null) {
            ChatUtils.modMessage("No valid kuudra spots found!")
            return
        }
        val speeds = when (direction) {
            RendRotationRules.Direction.FRONT -> frontMin.value to frontMax.value
            RendRotationRules.Direction.RIGHT -> rightMin.value to rightMax.value
            RendRotationRules.Direction.LEFT -> leftMin.value to leftMax.value
            RendRotationRules.Direction.BACK -> backMin.value to backMax.value
        }
        RatHumanRotation.lookAt(direction.target, -30.0f,
            offset(yawMin.value, yawMax.value), offset(pitchMin.value, pitchMax.value),
            RatHumanRotation.Profile.speeds(speeds.first.toFloat(), speeds.second.toFloat()))
    }

    private fun offset(lower: Double, upper: Double): Float =
        if (lower < upper) ThreadLocalRandom.current().nextDouble(lower, upper).toFloat() else lower.toFloat()

    private fun stopRotation() {
        pending.reset()
        RatHumanRotation.cancel()
    }

    override fun onDisable() {
        super.onDisable()
        stopRotation()
    }
}
