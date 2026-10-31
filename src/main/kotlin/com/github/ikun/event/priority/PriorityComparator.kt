package com.github.ikun.event.priority

import com.github.ikun.event.EventListener

object PriorityComparator: Comparator<EventListener<*>> {
    override fun compare(o1: EventListener<*>, o2: EventListener<*>): Int =
        o1.priority.ordinal - o2.priority.ordinal
}
