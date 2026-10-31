package com.github.ikun.ui.clickgui.components.settings.impl

import com.github.ikun.config.types.UnitSetting
import com.github.ikun.ui.clickgui.components.settings.Style
import com.github.ikun.ui.clickgui.components.settings.Widget
import com.github.ikun.utils.render.Render2D.drawCenteredString
import com.github.ikun.utils.render.Render2D.drawRect
import com.github.ikun.utils.ColorUtils.withAlpha
import net.minecraft.client.gui.GuiGraphicsExtractor

class CategoryWidget(name: String): Widget<Unit>(UnitSetting(name)) {
    override val height = 22

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        ctx.drawRect(x, y, width, height, Style.textColor.withAlpha(10))
        ctx.drawCenteredString("§l$name", x + width / 2, y + 7, Style.accentColor)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int) = false
}
