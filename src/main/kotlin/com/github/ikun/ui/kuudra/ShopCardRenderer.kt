package com.github.ikun.ui.kuudra

import com.github.ikun.utils.kuudra.ShopCardId
import com.github.ikun.utils.kuudra.ShopCardPlacement
import com.github.ikun.utils.kuudra.ShopRect
import com.github.ikun.features.impl.kuudra.KuudraShop
import com.github.ikun.ui.clickgui.components.settings.Style
import com.github.ikun.utils.ColorUtils.lerp
import com.github.ikun.utils.ColorUtils.withAlpha
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack
import java.awt.Color
import kotlin.math.roundToInt

/** Shared by the live server menu and HUD Edit so the preview uses identical card sizes. */
object ShopCardRenderer {
    fun draw(g: GuiGraphicsExtractor, placement: ShopCardPlacement, id: ShopCardId, stack: ItemStack,
             summary: List<String>, keyLabel: String? = null, hover: Float = 0f, border: Int? = null) {
        withPlacement(g, placement) {
            draw(g, id.defaultRect.copy(x = 0, y = 0), stack, summary, keyLabel, hover, border)
        }
    }

    fun confirmationButton(g: GuiGraphicsExtractor, placement: ShopCardPlacement, id: ShopCardId, hovered: Boolean,
                           keyLabel: String? = null) {
        withPlacement(g, placement) {
            confirmationButton(g, id.defaultRect.copy(x = 0, y = 0), id == ShopCardId.CONFIRM, hovered, keyLabel)
        }
    }

    private inline fun withPlacement(g: GuiGraphicsExtractor, placement: ShopCardPlacement, draw: () -> Unit) {
        g.pose().pushMatrix()
        try {
            g.pose().translate(placement.x.toFloat(), placement.y.toFloat())
            g.pose().scale(placement.scale.toFloat(), placement.scale.toFloat())
            draw()
        } finally {
            g.pose().popMatrix()
        }
    }

    fun draw(g: GuiGraphicsExtractor, rect: ShopRect, stack: ItemStack, summary: List<String>,
             keyLabel: String? = null, hover: Float = 0f, border: Int? = null) {
        val font = Minecraft.getInstance().font
        panel(g, rect, hover, border)
        g.pose().pushMatrix()
        g.pose().translate((rect.x + 10).toFloat(), (rect.y + 24).toFloat())
        g.pose().scale(1.5f, 1.5f)
        g.fakeItem(stack, 0, 0)
        g.pose().popMatrix()
        keyLabel?.let { keyHint(g, it, rect.x + 10, rect.y + 8, 28) }
        g.pose().pushMatrix()
        g.pose().translate((rect.x + 44).toFloat(), (rect.y + 12).toFloat())
        val textScale = minOf(1f, 150f / font.width(stack.hoverName).coerceAtLeast(1))
        g.pose().scale(textScale, textScale)
        g.text(font, stack.hoverName, 0, 0, 0xFFF3F7FF.toInt(), false)
        g.pose().popMatrix()
        summary.forEachIndexed { line, text ->
            g.text(font, font.plainSubstrByWidth(text, 150), rect.x + 44, rect.y + 30 + line * 13,
                if (line == 0) 0xFFB9CCD9.toInt() else 0xFF79D7C3.toInt(), false)
        }
    }

    fun button(g: GuiGraphicsExtractor, rect: ShopRect, label: String, hovered: Boolean, enabled: Boolean = true) {
        panel(g, rect, if (enabled && hovered) 1f else 0f)
        g.centeredText(Minecraft.getInstance().font, label, rect.x + rect.width / 2, rect.y + 9,
            if (enabled) 0xFFDBEAF2.toInt() else 0xFF667787.toInt())
    }

    fun confirmationButton(g: GuiGraphicsExtractor, rect: ShopRect, confirm: Boolean, hovered: Boolean,
                           keyLabel: String? = null) {
        val edge = if (confirm) 0xFF52BF80.toInt() else 0xFFCE6464.toInt()
        panel(g, rect, if (hovered) 1f else 0f)
        g.centeredText(Minecraft.getInstance().font, if (confirm) "Confirm" else "Cancel",
            rect.x + rect.width / 2, rect.y + (rect.height - 9) / 2, edge)
        keyLabel?.let { keyHint(g, it, rect.x + 6, rect.y + 6, 22) }
    }

    private fun keyHint(g: GuiGraphicsExtractor, label: String, x: Int, y: Int, maxWidth: Int) {
        val font = Minecraft.getInstance().font
        val scale = minOf(1f, maxWidth.toFloat() / font.width(label).coerceAtLeast(1))
        g.pose().pushMatrix()
        g.pose().translate(x.toFloat(), y.toFloat())
        g.pose().scale(scale, scale)
        g.text(font, label, 0, 0, Style.accentColor.rgb, false)
        g.pose().popMatrix()
    }

    private fun panel(g: GuiGraphicsExtractor, rect: ShopRect, hover: Float, border: Int? = null) {
        val base = KuudraShop.buttonBackground()
        if (base.alpha == 0) return
        val progress = hover.coerceIn(0f, 1f)
        val background = base.lerp(Color.WHITE, progress * 0.1f).withAlpha(base.alpha)
        g.fill(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, background.rgb)
        val edge = if (border != null) Color(border, true).withAlpha(base.alpha)
            else Color.WHITE.withAlpha(((40 + 40 * progress) * base.alpha / 255).roundToInt())
        g.fill(rect.x, rect.y, rect.x + rect.width, rect.y + 1, edge.rgb)
        g.fill(rect.x, rect.y + rect.height - 1, rect.x + rect.width, rect.y + rect.height, edge.rgb)
        g.fill(rect.x, rect.y + 1, rect.x + 1, rect.y + rect.height - 1, edge.rgb)
        g.fill(rect.x + rect.width - 1, rect.y + 1, rect.x + rect.width, rect.y + rect.height - 1, edge.rgb)
        if (progress > 0f) {
            val accent = Style.accentColor.withAlpha((base.alpha * progress).roundToInt())
            g.fill(rect.x, rect.y + 1, rect.x + 2, rect.y + rect.height - 1, accent.rgb)
        }
    }
}
