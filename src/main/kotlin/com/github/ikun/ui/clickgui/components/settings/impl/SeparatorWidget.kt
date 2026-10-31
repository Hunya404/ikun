package com.github.ikun.ui.clickgui.components.settings.impl

import com.github.ikun.config.types.UnitSetting
import com.github.ikun.ui.clickgui.components.settings.Style
import com.github.ikun.ui.clickgui.components.settings.Widget
import com.github.ikun.utils.render.Render2D.drawRect
import com.github.ikun.utils.ColorUtils.withAlpha
import net.minecraft.client.gui.GuiGraphicsExtractor

class SeparatorWidget(name: String = ""): Widget<Unit>(UnitSetting(name)) {
    override val height = 10
    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        ctx.drawRect(x + 10f, y + 5f, width - 20f, 0.5f, Style.textColor.withAlpha(30))
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int) = false
}
