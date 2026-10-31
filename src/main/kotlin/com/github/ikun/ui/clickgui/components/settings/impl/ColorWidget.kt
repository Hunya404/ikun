package com.github.ikun.ui.clickgui.components.settings.impl

import com.github.ikun.config.types.ColorSetting
import com.github.ikun.ui.clickgui.components.settings.Style
import com.github.ikun.ui.clickgui.components.settings.Widget
import com.github.ikun.utils.render.Render2D.drawString
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color
import kotlin.math.roundToInt

/** Hue strip and saturation/brightness field, using the same square controls as ClickGUI. */
class ColorWidget(private val setting: ColorSetting): Widget<Int>(setting) {
    override val height = 132
    private var dragging = 0
    private var lastRgb = -1
    private var hue = 0f
    private var saturation = 0f
    private var brightness = 0f
    private val fieldWidth get() = (width - 20).coerceAtLeast(1)

    private fun sync() {
        if (lastRgb == value) return
        val color = Color(value)
        val hsv = Color.RGBtoHSB(color.red, color.green, color.blue, null)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        lastRgb = value
    }

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        sync()
        if (dragging != 0) update(mouseX.toDouble(), mouseY.toDouble())
        Style.drawBackground(ctx, x, y, width, height)
        ctx.drawString(name, x + 8, y + 6, Style.textColor)
        ctx.drawString("#%06X".format(value), x + 10, y + 23, Style.accentColor)
        ctx.fill(x + width - 38, y + 21, x + width - 10, y + 33, value or 0xFF000000.toInt())

        val left = x + 10
        val w = fieldWidth
        for (column in 0 until w) {
            val fraction = column.toFloat() / (w - 1).coerceAtLeast(1)
            ctx.fill(left + column, y + 40, left + column + 1, y + 50, Color.HSBtoRGB(fraction, 1f, 1f))
            ctx.fillGradient(left + column, y + 58, left + column + 1, y + 122,
                Color.HSBtoRGB(hue, fraction, 1f), 0xFF000000.toInt())
        }
        val hueX = left + (hue * (w - 1)).roundToInt()
        ctx.fill(hueX - 1, y + 38, hueX + 2, y + 52, 0xFFFFFFFF.toInt())
        val sx = left + (saturation * (w - 1)).roundToInt()
        val sy = y + 58 + ((1f - brightness) * 63).roundToInt()
        ctx.fill(sx - 3, sy - 3, sx + 4, sy + 4, 0xFF000000.toInt())
        ctx.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xFFFFFFFF.toInt())
        ctx.fill(sx - 1, sy - 1, sx + 2, sy + 2, Color.HSBtoRGB(hue, saturation, brightness))
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) return false
        if (button == 1) {
            dragging = 0
            setting.reset()
            sync()
            Style.playClickSound(1f)
            return true
        }
        if (button != 0) return false
        sync()
        dragging = when {
            mouseX < x + 10 || mouseX >= x + 10 + fieldWidth -> 0
            mouseY >= y + 40 && mouseY < y + 50 -> 1
            mouseY >= y + 58 && mouseY < y + 122 -> 2
            else -> 0
        }
        if (dragging != 0) update(mouseX, mouseY)
        return true
    }

    private fun update(mouseX: Double, mouseY: Double) {
        val fraction = ((mouseX - x - 10) / (fieldWidth - 1).coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
        if (dragging == 1) hue = fraction
        else {
            saturation = fraction
            brightness = (1f - ((mouseY - y - 58) / 63).toFloat()).coerceIn(0f, 1f)
        }
        setting.set(Color.HSBtoRGB(hue, saturation, brightness) and 0xFFFFFF)
        lastRgb = value
    }

    override fun mouseReleased(button: Int) { if (button == 0) dragging = 0 }
}
