package com.github.ikun.ui.clickgui.components.settings

import com.github.ikun.utils.ColorUtils.withAlpha
import com.github.ikun.utils.NumbersUtils.div
import com.github.ikun.utils.NumbersUtils.minus
import com.github.ikun.utils.NumbersUtils.plus
import com.github.ikun.utils.render.Render2D.drawRect
import com.github.ikun.utils.render.Render2D.drawString
import com.github.ikun.Ikun.mc
import com.github.ikun.ui.clickgui.ClickGuiTheme
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents
import java.awt.Color

object Style {
    private val palette get() = ClickGuiTheme.palette
    val accentColor get() = palette.accentColor
    val accentColorTrans get() = accentColor.withAlpha(120)
    val textColor get() = palette.textColor
    val mutedTextColor get() = palette.mutedTextColor
    val panelBackgroundColor get() = palette.surface()
    val headerBackgroundColor get() = palette.surface(245f / 235f, 8)
    val categoryHeaderColor get() = palette.surface(230f / 235f, 4)
    val settingBackgroundColor get() = palette.surface(100f / 235f, -6)
    val featureBackgroundColor get() = palette.surface(180f / 235f, -1)
    val sliderBackgroundColor get() = palette.surface(200f / 235f, 24)
    val searchBackgroundColor get() = palette.surface(200f / 235f, -1)
    val inputBackgroundColor get() = palette.surface(180f / 235f, -6)
    val dropdownBackgroundColor get() = palette.surface(150f / 235f, -11)
    val tooltipBackgroundColor get() = palette.surface(240f / 235f, -6)
    val inactiveToggleColor get() = palette.surface(120f / 235f, 24)
    val overlayTopColor get() = palette.overlay(true)
    val overlayBottomColor get() = palette.overlay(false)
    val shadowColor get() = overlayBottomColor.withAlpha((panelBackgroundColor.alpha * 70f / 235f).toInt())

    fun drawBackground(ctx: GuiGraphicsExtractor, x: Number, y: Number, w: Number, h: Number) {
        ctx.drawRect(x, y, w, h, settingBackgroundColor)
    }

    fun drawHoverBar(ctx: GuiGraphicsExtractor, x: Number, y: Number, height: Number, anim: Float) {
        if (anim <= 0.01f) return
        val barH = (height - 6f) * anim
        val barY = y + (height / 2f) - (barH / 2f)
        ctx.drawRect(x, barY, 1.5f, barH, accentColor.withAlpha((200 * anim).toInt()))
    }

    fun drawNudgedText(ctx: GuiGraphicsExtractor, text: String, x: Float, y: Float, anim: Float, color: Color = textColor) {
        val xOffset = 2f * anim
        ctx.drawString(text, x + xOffset, y, color)
    }

    fun drawSlider(ctx: GuiGraphicsExtractor, x: Float, y: Float, w: Float, progress: Float, color: Color) {
        val kSize = 4f
        val h = 2.5f
        ctx.drawRect(x, y, w, h, sliderBackgroundColor)
        ctx.drawRect(x, y, w * progress, h, color)
        ctx.drawRect(x + (w * progress) - (kSize / 2f), y + (h / 2f) - (kSize / 2f), kSize, kSize, textColor)
    }

    fun playClickSound(pitch: Float) = mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.25f))
}
