package com.github.ikun.ui.clickgui.components.settings.impl

import com.github.ikun.config.types.KeybindSetting
import com.github.ikun.ui.clickgui.components.settings.Style
import com.github.ikun.ui.clickgui.components.settings.Widget
import com.github.ikun.utils.render.Render2D.drawRect
import com.github.ikun.utils.render.Render2D.drawString
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.lwjgl.glfw.GLFW
import java.awt.Color

class KeybindWidget(private val setting: KeybindSetting): Widget<Int>(setting) {
    private var captureMessage: String? = null
    override val height = 34

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        Style.drawBackground(ctx, x, y, width, height)
        if (active === this || inside(mouseX.toDouble(), mouseY.toDouble()))
            ctx.drawRect(x, y, width, height, Style.accentColorTrans)
        ctx.drawString(name, x + 8, y + 6, Style.textColor)
        val text = if (active === this) captureMessage ?: "Press a key..." else KeybindSetting.keyName(value)
        val color = if (active === this && captureMessage != null) Color(235, 130, 130) else Style.accentColor
        ctx.drawString(text, x + 8, y + 20, color)
    }

    private fun inside(mx: Double, my: Double) = mx >= x && mx < x + width && my >= y && my < y + height

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!inside(mouseX, mouseY)) return false
        when (button) {
            GLFW.GLFW_MOUSE_BUTTON_LEFT -> {
                cancelCapture()
                active = this
            }
            GLFW.GLFW_MOUSE_BUTTON_RIGHT -> {
                cancelCapture()
                if (!setting.set(setting.defaultValue)) {
                    active = this
                    captureMessage = "Key already used"
                }
            }
            else -> return false
        }
        Style.playClickSound(1f)
        return true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean =
        active === this && captureKey(keyCode, scanCode, modifiers)

    companion object {
        private var active: KeybindWidget? = null
        val capturing: Boolean get() = active != null

        @Suppress("UNUSED_PARAMETER")
        fun captureKey(key: Int, scan: Int, mods: Int): Boolean {
            val widget = active ?: return false
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                cancelCapture()
                return true
            }
            if (key == GLFW.GLFW_KEY_UNKNOWN) {
                widget.captureMessage = "Unsupported key"
                return true
            }
            val code = if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE)
                KeybindSetting.UNBOUND else key
            if (widget.setting.set(code)) cancelCapture()
            else widget.captureMessage = if (KeybindSetting.isValidKey(code)) "Key already used" else "Unsupported key"
            return true
        }

        fun cancelCapture() {
            active?.captureMessage = null
            active = null
        }
    }
}
