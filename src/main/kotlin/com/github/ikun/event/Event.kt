package com.github.ikun.event
abstract class Event(val cancelable: Boolean = false) {
    var isCanceled = false
        set(value) { require(cancelable || !value); field = value }
    fun cancel() { isCanceled = true }
}
