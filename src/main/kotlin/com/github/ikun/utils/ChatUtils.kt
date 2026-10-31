package com.github.ikun.utils

import com.github.ikun.Ikun
import net.minecraft.network.chat.Component

object ChatUtils {
    fun String.addColor() = replace('&', '§')
    fun String.removeFormatting() = replace(Regex("[§&][0-9a-fk-or]", RegexOption.IGNORE_CASE), "")
    fun chat(message: Any?) = chat(Component.literal(message.toString().addColor()))
    fun chat(message: Component) = Ikun.mc.execute { Ikun.mc.gui.chat.addClientSystemMessage(message) }
    fun modMessage(message: Any?) = chat(Ikun.PREFIX.copy().append(Component.literal(" $message".addColor())))
    fun sendCommand(command: String) = Ikun.mc.execute {
        Ikun.mc.player?.connection?.sendCommand(command.removePrefix("/"))
    }
}
