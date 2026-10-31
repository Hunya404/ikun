package com.github.ikun.event.impl
import com.github.ikun.event.Event
import com.github.ikun.utils.ChatUtils.removeFormatting
import net.minecraft.network.chat.Component
class ChatMessageEvent(val component: Component): Event(cancelable = true) {
    val unformattedText by lazy { component.string.removeFormatting() }
}
