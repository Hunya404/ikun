package com.github.ikun.ui.clickgui.components.settings.impl

import com.github.ikun.config.types.ToggleSetting
import com.github.ikun.ui.clickgui.components.settings.Style
import com.github.ikun.ui.clickgui.components.settings.Widget
import com.github.ikun.ui.utils.Animation
import com.github.ikun.utils.ColorUtils.lerp
import com.github.ikun.utils.render.Render2D.drawRect
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

class ToggleWidget(config: ToggleSetting): Widget<Boolean>(config) {
    private val toggleAnim = Animation(200, if (config.value) 1f else 0f)
    private val hoverAnim = Animation(200, 0f)

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        toggleAnim.update(if (value) 1f else 0f)
        hoverAnim.update(if (isHovered) 1f else 0f)

        Style.drawBackground(ctx, x, y, width, height)
        Style.drawHoverBar(ctx, x, y, height, hoverAnim.value)
        Style.drawNudgedText(ctx, name, x + 8f, y + 6f, hoverAnim.value)

        val sw = 18f
        val sh = 6f
        val sx = x + width - sw - 10f
        val sy = y + (height / 2f) - (sh / 2f)
        ctx.drawRect(sx, sy, sw, sh, Style.inactiveToggleColor.lerp(Style.accentColorTrans, toggleAnim.value))
        val tx = sx + (toggleAnim.value * (sw - 8f))
        ctx.drawRect(tx, sy - 1f, 8f, 8f, Style.mutedTextColor.lerp(Style.accentColor, toggleAnim.value))
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            value = ! value
            Style.playClickSound(1f)
            return true
        }
        return false
    }
}
