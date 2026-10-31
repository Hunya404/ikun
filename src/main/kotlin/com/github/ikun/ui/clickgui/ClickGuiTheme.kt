package com.github.ikun.ui.clickgui

import com.github.ikun.config.Savable
import com.github.ikun.config.SettingProvider
import com.github.ikun.config.ConfigHolder
import com.github.ikun.config.types.ActionSetting
import com.github.ikun.config.types.ColorSetting
import com.github.ikun.config.types.NumberSetting
import com.github.ikun.features.Feature
import com.google.gson.JsonObject
import com.google.gson.JsonElement
import java.awt.Color
import kotlin.math.roundToInt

/** One palette shared by all ClickGUI windows; no runtime state is persisted here. */
class ClickGuiPalette : SettingProvider {
    override val configSettings = linkedSetOf<ConfigHolder<*>>()
    val accent by ColorSetting("Theme Color", 0x63B0D9).section("Colors")
    val panel by ColorSetting("Panel Color", 0x101010).section("Colors")
    val background by ColorSetting("Background Color", 0x000000).section("Colors")
    val text by ColorSetting("Text Color", 0xFFFFFF).section("Colors")
    val mutedText by ColorSetting("Muted Text Color", 0xA0A0A0).section("Colors")
    val panelOpacity by NumberSetting("Panel Opacity", 92.0, 0.0, 100.0, 1.0, "%").section("Opacity")
    val backgroundOpacity by NumberSetting("Background Opacity", 59.0, 0.0, 100.0, 1.0, "%").section("Opacity")

    val accentColor get() = Color(accent.value)
    val textColor get() = Color(text.value)
    val mutedTextColor get() = Color(mutedText.value)

    fun surface(strength: Float = 1f, shade: Int = 0): Color {
        val base = Color(panel.value)
        val alpha = (panelOpacity.value * 255.0 / 100.0).roundToInt()
        return Color((base.red + shade).coerceIn(0, 255), (base.green + shade).coerceIn(0, 255),
            (base.blue + shade).coerceIn(0, 255), (alpha * strength).roundToInt().coerceIn(0, 255))
    }

    fun overlay(top: Boolean): Color {
        val rgb = Color(background.value)
        val alpha = (backgroundOpacity.value * 255.0 / 100.0).roundToInt()
        return Color(rgb.red, rgb.green, rgb.blue, if (top) (alpha * (2f / 3f)).roundToInt() else alpha)
    }

    fun reset() = configSettings.forEach { it.reset() }

    fun write() = JsonObject().apply {
        configSettings.forEach { setting -> if (setting is Savable) add(setting.jsonName, setting.write()) }
    }

    /** Ignore individual malformed fields while retaining the rest of the theme. */
    fun read(json: JsonElement?): List<String> {
        if (json == null || json.isJsonNull) return emptyList()
        if (!json.isJsonObject) return listOf("clickGuiTheme")
        return buildList {
            json.asJsonObject.entrySet().forEach { (name, value) ->
                val setting = getSettingByName(name) as? Savable ?: return@forEach
                runCatching { setting.read(value) }.onFailure { add(name) }
            }
        }
    }
}

/** Settings-window adapter, opened by the Colors button instead of a gameplay toggle. */
object ClickGuiTheme : Feature("Customize ClickGUI colors and opacity.", name = "ClickGUI Colors") {
    val palette = ClickGuiPalette()
    init {
        configSettings.add(ActionSetting("Reset Theme") { palette.reset() }
            .withDescription("Restore all ClickGUI colors and opacity to their defaults"))
        configSettings.addAll(palette.configSettings)
    }
}
