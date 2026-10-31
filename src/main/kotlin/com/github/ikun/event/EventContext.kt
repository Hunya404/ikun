package com.github.ikun.event

class EventContext<T: Event>(val event: T, var listener: EventListener<T>)
