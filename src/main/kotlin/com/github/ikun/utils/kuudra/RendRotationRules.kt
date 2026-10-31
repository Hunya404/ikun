// SPDX-License-Identifier: GPL-3.0-only
// Rotation-only adaptation of RatTokens' RatAddons 2.0.1 RendMacro and KuudraState.
package com.github.ikun.utils.kuudra

import net.minecraft.world.phys.Vec3

object RendRotationRules {
    enum class Direction(val target: Vec3, val minimum: Double, val maximum: Double) {
        RIGHT(Vec3(-123.5, 16.0, -105.5), 235.0, 295.0),
        FRONT(Vec3(-104.5, 16.0, -82.5), 85.0, 145.0),
        LEFT(Vec3(-80.5, 16.0, -104.5), 235.0, 295.0),
        BACK(Vec3(-98.5, 16.0, -129.5), 285.0, 345.0)
    }

    fun isEntry(position: Vec3): Boolean = position.x in -102.0..-101.0 &&
        position.y in 5.0..7.0 && position.z in -106.0..-105.0

    // Preserve RatAddons' strict boundaries and X-before-Z priority at corners.
    fun direction(position: Vec3): Direction? = when {
        position.x < -128.0 -> Direction.RIGHT
        position.z > -84.0 -> Direction.FRONT
        position.x > -72.0 -> Direction.LEFT
        position.z < -132.0 -> Direction.BACK
        else -> null
    }

    fun isBoss(alive: Boolean, size: Int, baseMaxHealth: Double): Boolean =
        alive && size == 30 && baseMaxHealth == 100_000.0
}

class RendPhaseState {
    enum class Phase { NONE, SUPPLIES, BUILD, STUN, DPS }
    var phase = Phase.NONE
        private set
    var isDead = false
        private set
    var inKuudra = false
        private set

    fun observeChat(message: String, ownName: String?) {
        when {
            message.contains(SUPPLIES_START) -> phase = Phase.SUPPLIES
            message == BUILD_START -> phase = Phase.BUILD
            message == STUN_START -> phase = Phase.STUN
        }
        if (ownName != null && message.contains("$ownName was FINAL KILLED by Kuudra!")) isDead = true
    }

    fun observeHeight(y: Double) {
        if (y < 20.0 && phase == Phase.STUN) phase = Phase.DPS
    }

    fun observeTab(text: String): Boolean {
        var marker = text.indexOf("Area:")
        var length = "Area:".length
        if (marker < 0) {
            marker = text.indexOf("Dungeon:")
            length = "Dungeon:".length
        }
        if (marker < 0) return false
        val area = text.substring(marker + length).replace(Regex("§."), "").trim()
        inKuudra = area.contains("Kuudra", ignoreCase = true)
        return true
    }

    fun canTrigger(enabled: Boolean, position: Vec3, singleplayer: Boolean = false): Boolean =
        enabled && (inKuudra || singleplayer) && !isDead &&
            (phase == Phase.STUN || phase == Phase.DPS) && RendRotationRules.isEntry(position)

    fun reset() {
        phase = Phase.NONE
        isDead = false
        inKuudra = false
    }

    companion object {
        const val SUPPLIES_START = "[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!"
        const val BUILD_START = "[NPC] Elle: OMG! Great work collecting my supplies!"
        const val STUN_START = "[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!"
    }
}

/** RatAddons checks ticks-- <= 0 at START_CLIENT_TICK; zero runs on the next tick. */
class RendRotationQueue {
    private val tasks = mutableListOf<Int>()

    fun schedule(ticks: Int) {
        if (ticks >= 0) tasks.add(ticks)
    }

    fun tick(): Int {
        var due = 0
        val iterator = tasks.listIterator()
        while (iterator.hasNext()) {
            val ticks = iterator.next()
            if (ticks <= 0) {
                iterator.remove()
                due++
            } else iterator.set(ticks - 1)
        }
        return due
    }

    fun reset() = tasks.clear()
}
