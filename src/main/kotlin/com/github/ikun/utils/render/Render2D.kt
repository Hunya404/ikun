package com.github.ikun.utils.render

import com.github.ikun.Ikun.mc
import com.github.ikun.utils.ChatUtils.addColor
import com.github.ikun.utils.NumbersUtils.minus
import com.github.ikun.utils.NumbersUtils.plus
import com.github.ikun.utils.NumbersUtils.times
import com.github.ikun.utils.render.RenderHelper.width
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

object Render2D {

    fun GuiGraphicsExtractor.scissor(x: Number, y: Number, width: Number, height: Number) {
        enableScissor(x.toInt(), y.toInt(), x.toInt() + width.toInt(), y.toInt() + height.toInt())
    }

    fun GuiGraphicsExtractor.drawRect(x: Number, y: Number, width: Number, height: Number, color: Color = Color.WHITE) {
        val fx = x.toFloat()
        val fy = y.toFloat()
        val fw = width.toFloat()
        val fh = height.toFloat()

        pose().pushMatrix()
        pose().translate(fx, fy)
        pose().scale(fw, fh)
        fill(0, 0, 1, 1, color.rgb)
        pose().popMatrix()
    }

    fun GuiGraphicsExtractor.drawBorder(x: Number, y: Number, width: Number, height: Number, color: Color = Color.WHITE, thickness: Number = 1) {
        drawRect(x, y, width, thickness, color)
        drawRect(x, y + height - thickness, width, thickness, color)
        drawRect(x, y + thickness, thickness, height - thickness * 2, color)
        drawRect(x + width - thickness, y + thickness, thickness, height - thickness * 2, color)
    }

    fun GuiGraphicsExtractor.drawString(str: String, x: Number, y: Number, color: Color = Color.WHITE, scale: Number = 1, shadow: Boolean = true) {
        val fx = x.toFloat()
        val fy = y.toFloat()
        val fScale = scale.toFloat()

        pose().pushMatrix()
        pose().translate(fx, fy)
        if (fScale != 1f) pose().scale(fScale, fScale)
        text(mc.font, str.addColor(), 0, 0, color.rgb, shadow)
        pose().popMatrix()
    }

    fun GuiGraphicsExtractor.drawCenteredString(str: String, x: Number, y: Number, color: Color = Color.WHITE, scale: Number = 1, shadow: Boolean = true) {
        val fScale = scale.toFloat()
        val totalScaledWidth = str.width() * fScale
        val centerX = x.toFloat() - (totalScaledWidth / 2f)
        drawString(str, centerX, y, color, scale, shadow)
    }

    /**
     * Draws a gradient from Color1 (Top) to Color2 (Bottom)
     */
    fun GuiGraphicsExtractor.drawVerticalGradient(x: Number, y: Number, width: Number, height: Number, color1: Color, color2: Color) {
        val fx = x.toFloat()
        val fy = y.toFloat()
        val fw = width.toFloat()
        val fh = height.toFloat()

        pose().pushMatrix()
        pose().translate(fx, fy)
        pose().scale(fw, fh)
        fillGradient(0, 0, 1, 1, color1.rgb, color2.rgb)
        pose().popMatrix()
    }

}
