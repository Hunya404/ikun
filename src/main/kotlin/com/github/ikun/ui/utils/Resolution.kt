package com.github.ikun.ui.utils

import com.github.ikun.utils.NumbersUtils.div
import com.github.ikun.Ikun.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.min

object Resolution {
    private const val REFERENCE_WIDTH = 960f
    private const val REFERENCE_HEIGHT = 540f

    var scale = 1f
        private set

    var width = 960f
        private set

    var height = 540f
        private set

    private fun refresh() {
        val guiWidth = mc.window.guiScaledWidth.toFloat()
        val guiHeight = mc.window.guiScaledHeight.toFloat()

        scale = min(guiWidth / REFERENCE_WIDTH, guiHeight / REFERENCE_HEIGHT)

        width = guiWidth / scale
        height = guiHeight / scale
    }

    fun push(ctx: GuiGraphicsExtractor) {
        refresh()
        ctx.pose().pushMatrix()
        ctx.pose().scale(scale, scale)
    }

    fun pop(ctx: GuiGraphicsExtractor) {
        ctx.pose().popMatrix()
    }

    fun getMouseX(vanillaX: Number) = (vanillaX / scale).toInt()
    fun getMouseY(vanillaY: Number) = (vanillaY / scale).toInt()

}
