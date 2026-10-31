package com.github.ikun.utils.kuudra

class CannonCloseWindow {
    private var closeUntil = 0L

    fun chat(message: String, now: Long) {
        if (message.contains("You purchased Human Cannonball!")) closeUntil = now + 500
    }

    fun shouldClose(title: String, now: Long) = now < closeUntil && title.contains("Perk Menu")
    fun reset() { closeUntil = 0 }
}
