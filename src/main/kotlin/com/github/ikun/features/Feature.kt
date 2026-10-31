package com.github.ikun.features

import com.github.ikun.config.ConfigHolder
import com.github.ikun.config.SettingProvider
import com.github.ikun.event.*
import com.github.ikun.event.priority.EventPriority
import com.github.ikun.ui.clickgui.enums.CategoryType
import net.minecraft.client.Minecraft

open class Feature(
    val description: String? = null, val name: String, toggled: Boolean = false,
    val category: CategoryType = CategoryType.GENERAL
): SettingProvider {
    val jsonName = name
    @JvmField var enabled = toggled
    override val configSettings = mutableSetOf<ConfigHolder<*>>()
    private val listeners = mutableSetOf<EventListener<*>>()

    fun initialize() { init(); if (enabled) onEnable() else onDisable() }
    open fun init() = Unit
    open fun tick(client: Minecraft) = Unit
    open fun onEnable() { listeners.forEach { it.register() } }
    open fun onDisable() { listeners.forEach { it.unregister() } }
    fun toggle() { enabled = !enabled; if (enabled) onEnable() else onDisable() }

    protected inline fun <reified T: Event> register(
        priority: EventPriority = EventPriority.NORMAL,
        receiveCancelled: Boolean = false,
        noinline block: EventContext<T>.() -> Unit
    ): EventListener<T> = addListener(EventBus.listener<T>(priority, receiveCancelled, block))

    @PublishedApi internal fun <T: Event> addListener(listener: EventListener<T>): EventListener<T> {
        listeners.add(listener)
        return listener
    }
}
