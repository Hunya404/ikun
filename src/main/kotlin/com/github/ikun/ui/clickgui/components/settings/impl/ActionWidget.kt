package com.github.ikun.ui.clickgui.components.settings.impl

import com.github.ikun.config.types.ActionSetting
import com.github.ikun.ui.clickgui.components.settings.Style
import com.github.ikun.ui.clickgui.components.settings.Widget
import com.github.ikun.utils.render.Render2D.drawCenteredString
import com.github.ikun.utils.render.Render2D.drawRect
import net.minecraft.client.gui.GuiGraphicsExtractor

class ActionWidget(private val setting: ActionSetting): Widget<Unit>(setting) {
    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        Style.drawBackground(ctx, x, y, width, height)
        if (inside(mouseX.toDouble(), mouseY.toDouble())) ctx.drawRect(x, y, width, height, Style.accentColorTrans)
        ctx.drawCenteredString(name, x + width / 2f, y + 6f, Style.textColor)
    }
    private fun inside(mx: Double, my: Double) = mx >= x && mx < x + width && my >= y && my < y + height
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0 || !inside(mouseX, mouseY)) return false
        Style.playClickSound(1f)
        setting.action()
        return true
    }
}
