package com.github.ikun.config

import kotlin.reflect.KProperty

interface SettingProvider {
    val configSettings: MutableSet<ConfigHolder<*>>
    fun getSettingByName(key: String?) = configSettings.find { it.jsonName == key && it is Savable }

    operator fun <T, S: ConfigHolder<T>> S.provideDelegate(thisRef: SettingProvider, prop: KProperty<*>): S {
        thisRef.configSettings.add(this)
        return this
    }
    operator fun <T, S: ConfigHolder<T>> S.getValue(thisRef: SettingProvider, prop: KProperty<*>): S = this
    fun <T: ConfigHolder<*>> T.section(name: String): T { section = name; return this }
    fun <T: ConfigHolder<*>> T.withDescription(desc: String): T {
        description = if (desc.endsWith('.')) desc else "$desc."
        return this
    }
    fun <T: ConfigHolder<*>> T.showIf(condition: () -> Boolean): T { visibility = condition; return this }
}
