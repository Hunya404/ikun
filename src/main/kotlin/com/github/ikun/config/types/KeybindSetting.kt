package com.github.ikun.config.types

import com.github.ikun.config.ConfigHolder
import com.github.ikun.config.Savable
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.lwjgl.glfw.GLFW

class KeybindSetting(name: String, defaultValue: Int): ConfigHolder<Int>(name, defaultValue), Savable {
    /** Runtime conflicts apply to editing; saved keys are checked by the consumer. */
    var validator: (Int) -> Boolean = { true }

    init {
        require(isValidKey(defaultValue)) { "Invalid default key code: $defaultValue" }
    }

    fun set(code: Int): Boolean {
        if (!isValidKey(code) || code != UNBOUND && !validator(code)) return false
        if (value != code) value = code
        return true
    }

    override fun reset() { set(defaultValue) }

    override fun write() = JsonPrimitive(value)

    override fun read(element: JsonElement) {
        if (!element.isJsonPrimitive || !element.asJsonPrimitive.isNumber) return
        // Gson's asInt truncates fractions and overflow; accept only exact integers.
        val code = runCatching { element.asBigDecimal.intValueExact() }.getOrNull() ?: return
        if (isValidKey(code) && value != code) value = code
    }

    companion object {
        const val UNBOUND = -1

        fun isValidKey(code: Int): Boolean = code == UNBOUND || when (code) {
            GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_APOSTROPHE, GLFW.GLFW_KEY_COMMA,
            GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_PERIOD, GLFW.GLFW_KEY_SLASH,
            GLFW.GLFW_KEY_SEMICOLON, GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_GRAVE_ACCENT,
            GLFW.GLFW_KEY_WORLD_1, GLFW.GLFW_KEY_WORLD_2 -> true
            in GLFW.GLFW_KEY_0..GLFW.GLFW_KEY_9,
            in GLFW.GLFW_KEY_A..GLFW.GLFW_KEY_Z,
            in GLFW.GLFW_KEY_LEFT_BRACKET..GLFW.GLFW_KEY_RIGHT_BRACKET,
            in GLFW.GLFW_KEY_ENTER..GLFW.GLFW_KEY_END,
            in GLFW.GLFW_KEY_CAPS_LOCK..GLFW.GLFW_KEY_PAUSE,
            in GLFW.GLFW_KEY_F1..GLFW.GLFW_KEY_F25,
            in GLFW.GLFW_KEY_KP_0..GLFW.GLFW_KEY_KP_EQUAL,
            in GLFW.GLFW_KEY_LEFT_SHIFT..GLFW.GLFW_KEY_MENU -> true
            else -> false
        }

        /** Shared by settings and shop cards, without requiring a GLFW window. */
        fun keyName(code: Int): String = when (code) {
            UNBOUND -> "Unbound"
            in GLFW.GLFW_KEY_0..GLFW.GLFW_KEY_9, in GLFW.GLFW_KEY_A..GLFW.GLFW_KEY_Z -> code.toChar().toString()
            in GLFW.GLFW_KEY_F1..GLFW.GLFW_KEY_F25 -> "F${code - GLFW.GLFW_KEY_F1 + 1}"
            in GLFW.GLFW_KEY_KP_0..GLFW.GLFW_KEY_KP_9 -> "Num ${code - GLFW.GLFW_KEY_KP_0}"
            GLFW.GLFW_KEY_SPACE -> "Space"
            GLFW.GLFW_KEY_APOSTROPHE -> "'"
            GLFW.GLFW_KEY_COMMA -> ","
            GLFW.GLFW_KEY_MINUS -> "-"
            GLFW.GLFW_KEY_PERIOD -> "."
            GLFW.GLFW_KEY_SLASH -> "/"
            GLFW.GLFW_KEY_SEMICOLON -> ";"
            GLFW.GLFW_KEY_EQUAL -> "="
            GLFW.GLFW_KEY_LEFT_BRACKET -> "["
            GLFW.GLFW_KEY_BACKSLASH -> "\\"
            GLFW.GLFW_KEY_RIGHT_BRACKET -> "]"
            GLFW.GLFW_KEY_GRAVE_ACCENT -> "`"
            GLFW.GLFW_KEY_WORLD_1 -> "World 1"
            GLFW.GLFW_KEY_WORLD_2 -> "World 2"
            GLFW.GLFW_KEY_ESCAPE -> "Esc"
            GLFW.GLFW_KEY_ENTER -> "Enter"
            GLFW.GLFW_KEY_TAB -> "Tab"
            GLFW.GLFW_KEY_BACKSPACE -> "Backspace"
            GLFW.GLFW_KEY_INSERT -> "Insert"
            GLFW.GLFW_KEY_DELETE -> "Delete"
            GLFW.GLFW_KEY_RIGHT -> "Right"
            GLFW.GLFW_KEY_LEFT -> "Left"
            GLFW.GLFW_KEY_DOWN -> "Down"
            GLFW.GLFW_KEY_UP -> "Up"
            GLFW.GLFW_KEY_PAGE_UP -> "Page Up"
            GLFW.GLFW_KEY_PAGE_DOWN -> "Page Down"
            GLFW.GLFW_KEY_HOME -> "Home"
            GLFW.GLFW_KEY_END -> "End"
            GLFW.GLFW_KEY_CAPS_LOCK -> "Caps Lock"
            GLFW.GLFW_KEY_SCROLL_LOCK -> "Scroll Lock"
            GLFW.GLFW_KEY_NUM_LOCK -> "Num Lock"
            GLFW.GLFW_KEY_PRINT_SCREEN -> "Print Screen"
            GLFW.GLFW_KEY_PAUSE -> "Pause"
            GLFW.GLFW_KEY_KP_DECIMAL -> "Num ."
            GLFW.GLFW_KEY_KP_DIVIDE -> "Num /"
            GLFW.GLFW_KEY_KP_MULTIPLY -> "Num *"
            GLFW.GLFW_KEY_KP_SUBTRACT -> "Num -"
            GLFW.GLFW_KEY_KP_ADD -> "Num +"
            GLFW.GLFW_KEY_KP_ENTER -> "Num Enter"
            GLFW.GLFW_KEY_KP_EQUAL -> "Num ="
            GLFW.GLFW_KEY_LEFT_SHIFT -> "Left Shift"
            GLFW.GLFW_KEY_LEFT_CONTROL -> "Left Ctrl"
            GLFW.GLFW_KEY_LEFT_ALT -> "Left Alt"
            GLFW.GLFW_KEY_LEFT_SUPER -> "Left Super"
            GLFW.GLFW_KEY_RIGHT_SHIFT -> "Right Shift"
            GLFW.GLFW_KEY_RIGHT_CONTROL -> "Right Ctrl"
            GLFW.GLFW_KEY_RIGHT_ALT -> "Right Alt"
            GLFW.GLFW_KEY_RIGHT_SUPER -> "Right Super"
            GLFW.GLFW_KEY_MENU -> "Menu"
            else -> "Unknown"
        }
    }
}
