package com.github.ikun.utils.kuudra

/** One gate is shared across shop screens so a held purchase key cannot confirm the next page. */
class ShopKeyPressGate {
    private val held = mutableSetOf<Int>()

    fun press(key: Int): Boolean = key >= 0 && held.add(key)

    fun release(key: Int) { held.remove(key) }

    /** Newly opened pages must also block keys held before their input listeners existed. */
    fun blockHeld(keys: Iterable<Int>, isDown: (Int) -> Boolean) {
        keys.filter { it >= 0 }.forEach { if (isDown(it)) held.add(it) }
    }

    /** Releases may go to a different screen; the physical state recovers those missed events. */
    fun releaseNotHeld(isDown: (Int) -> Boolean) {
        held.removeAll { !isDown(it) }
    }

    fun reset() { held.clear() }
}
