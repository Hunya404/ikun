package com.github.ikun.utils.render

import com.github.ikun.Ikun.mc
import com.github.ikun.utils.ChatUtils.addColor

object RenderHelper {
    fun String.width() = addColor().lineSequence().maxOf { mc.font.width(it) }.toFloat()
    fun String.wrap(maxWidth: Int) = buildList {
        for (raw in addColor().lineSequence()) {
            var line = ""
            for (word in raw.split(" ")) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (candidate.width() > maxWidth && line.isNotEmpty()) {
                    add(line)
                    line = word
                }
                else line = candidate
            }
            add(line)
        }
    }
}
