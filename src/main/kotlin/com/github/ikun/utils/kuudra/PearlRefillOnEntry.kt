package com.github.ikun.utils.kuudra

class PearlRefillOnEntry {
    var status = Status.OUTSIDE
        private set
    var pearlCount: Int? = null
        private set
    private var lastWorld: Any? = null
    private var enteredAt = 0L
    private var lastCheckedAt: Long? = null

    fun command(world: Any, inKuudra: Boolean, pearls: () -> Int, canSend: Boolean, now: Long): String? {
        if (!inKuudra) {
            status = Status.OUTSIDE
            return null
        }
        if (lastWorld !== world) {
            lastWorld = world
            enteredAt = now
            pearlCount = null
            lastCheckedAt = null
            status = Status.WAITING_SYNC
        }

        val previousCheck = lastCheckedAt
        val delay = if (previousCheck == null) 10_000L else 20_000L
        if (now - (previousCheck ?: enteredAt) < delay) {
            status = if (previousCheck == null) Status.WAITING_SYNC else Status.WAITING_INTERVAL
            return null
        }
        if (!canSend) {
            status = Status.WAITING_INPUT
            return null
        }

        val count = pearls()
        if (count < 0) {
            status = Status.WAITING_SYNC
            return null
        }
        pearlCount = count
        lastCheckedAt = now
        status = if (count < 16) Status.REQUESTED else Status.FULL
        return if (count < 16) "getfromsacks ENDER_PEARL ${16 - count}" else null
    }

    fun reset() {
        lastWorld = null
        pearlCount = null
        lastCheckedAt = null
        status = Status.OUTSIDE
    }

    enum class Status { OUTSIDE, WAITING_SYNC, WAITING_INTERVAL, WAITING_INPUT, FULL, REQUESTED }
}
