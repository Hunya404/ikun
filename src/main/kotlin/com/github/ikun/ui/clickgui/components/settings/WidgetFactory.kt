package com.github.ikun.ui.clickgui.components.settings
import com.github.ikun.config.ConfigHolder
import com.github.ikun.config.types.*
import com.github.ikun.ui.clickgui.components.settings.impl.*
object WidgetFactory {
    fun fromSetting(configHolder: ConfigHolder<*>): Widget<*>? = when (configHolder) {
        is ToggleSetting -> ToggleWidget(configHolder)
        is DropdownSetting -> DropdownWidget(configHolder)
        is TextInputSetting -> TextInputWidget(configHolder)
        is NumberSetting -> NumberWidget(configHolder)
        is ColorSetting -> ColorWidget(configHolder)
        is KeybindSetting -> KeybindWidget(configHolder)
        is ActionSetting -> ActionWidget(configHolder)
        is UnitSetting -> null
        else -> error("Unknown setting type")
    }
}
