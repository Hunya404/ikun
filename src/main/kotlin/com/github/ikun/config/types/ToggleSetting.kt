package com.github.ikun.config.types

import com.github.ikun.config.ConfigHolder
import com.github.ikun.config.Savable
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

class ToggleSetting(name: String, defaultValue: Boolean = false): ConfigHolder<Boolean>(name, defaultValue), Savable {
    override fun write() = JsonPrimitive(value)
    override fun read(element: JsonElement) {
        value = element.asBoolean
    }
}
