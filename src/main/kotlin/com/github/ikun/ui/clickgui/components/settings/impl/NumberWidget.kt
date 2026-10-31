package com.github.ikun.ui.clickgui.components.settings.impl

import com.github.ikun.config.types.NumberSetting
import com.github.ikun.ui.clickgui.components.settings.Style
import com.github.ikun.ui.clickgui.components.settings.Widget
import com.github.ikun.utils.render.Render2D.drawString
import com.github.ikun.utils.render.RenderHelper.width
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.util.Locale
import kotlin.math.sign

class NumberWidget(private val setting: NumberSetting): Widget<Double>(setting) {
    private var dragging = false
    override val height = 34

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (dragging) update(mouseX.toDouble())
        Style.drawBackground(ctx, x, y, width, height)
        ctx.drawString(name, x + 8, y + 6, Style.textColor)
        val label = String.format(Locale.ROOT, "%.1f%s", value, setting.suffix)
        ctx.drawString(label, x + width - label.width() - 8, y + 6, Style.accentColor)
        val progress = ((value - setting.minimum) / (setting.maximum - setting.minimum)).toFloat().coerceIn(0f, 1f)
        Style.drawSlider(ctx, x + 10f, y + 25f, (width - 20).coerceAtLeast(1).toFloat(), progress, Style.accentColor)
    }

    private fun inside(mx: Double, my: Double) = mx >= x && mx < x + width && my >= y && my < y + height

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!inside(mouseX, mouseY)) return false
        if (button == 1) {
            dragging = false
            setting.reset()
            Style.playClickSound(1f)
            return true
        }
        if (button != 0 || mouseY < y + 18) return false
        dragging = true
        update(mouseX)
        Style.playClickSound(1f)
        return true
    }

    private fun update(mouseX: Double) {
        val ratio = ((mouseX - x - 10) / (width - 20).coerceAtLeast(1)).coerceIn(0.0, 1.0)
        setting.set(setting.minimum + ratio * (setting.maximum - setting.minimum))
    }

    override fun mouseReleased(button: Int) { if (button == 0) dragging = false }
    override fun mouseScrolled(mouseX: Int, mouseY: Int, delta: Double): Boolean {
        if (!inside(mouseX.toDouble(), mouseY.toDouble()) || delta == 0.0) return false
        setting.set(value + sign(delta) * setting.step)
        return true
    }
}
