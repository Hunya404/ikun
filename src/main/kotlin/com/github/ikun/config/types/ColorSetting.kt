package com.github.ikun.config.types

import com.github.ikun.config.ConfigHolder
import com.github.ikun.config.Savable
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

class ColorSetting(name: String, defaultValue: Int): ConfigHolder<Int>(name, defaultValue), Savable {
    init { require(defaultValue in 0..0xFFFFFF) }

    fun set(rgb: Int) {
        require(rgb in 0..0xFFFFFF)
        value = rgb
    }

    override fun write() = JsonPrimitive("#%06X".format(value))
    override fun read(element: JsonElement) {
        val hex = element.asString
        require(hex.matches(Regex("#[0-9a-fA-F]{6}"))) { "Color must use #RRGGBB" }
        set(hex.substring(1).toInt(16))
    }
}
