package com.github.ikun.ui.clickgui

import com.github.ikun.Ikun
import com.github.ikun.config.ConfigManager
import com.github.ikun.features.Feature
import com.github.ikun.ui.clickgui.components.CategoryPanel
import com.github.ikun.ui.clickgui.components.FeatureConfigWindow
import com.github.ikun.ui.clickgui.components.settings.Style
import com.github.ikun.ui.clickgui.components.settings.impl.KeybindWidget
import com.github.ikun.ui.clickgui.enums.CategoryType
import com.github.ikun.ui.clickgui.enums.WindowClickAction
import com.github.ikun.ui.utils.MouseHelper
import com.github.ikun.ui.utils.Resolution
import com.github.ikun.ui.utils.TextInputHandler
import com.github.ikun.utils.render.Render2D.drawCenteredString
import com.github.ikun.utils.render.Render2D.drawRect
import com.github.ikun.utils.render.Render2D.drawVerticalGradient
import com.github.ikun.utils.ColorUtils.withAlpha
import org.lwjgl.glfw.GLFW
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import java.awt.Color

class ClickGuiScreen: Screen(Component.literal("ikun")) {
    private companion object {
        const val defaultWindowWidth = 220f
        const val defaultWindowHeight = 260f
        const val windowCascadeOffset = 18f
    }

    private val categoryPanels = mutableListOf<CategoryPanel>()
    private val configWindows = mutableListOf<FeatureConfigWindow>()
    private val capturedKeys = mutableSetOf<Int>()
    var searchQuery = ""

    private val searchHandler = TextInputHandler(
        textProvider = { searchQuery },
        textSetter = { searchQuery = it }
    )

    init {
        CategoryType.entries.forEachIndexed { index, category ->
            categoryPanels.add(CategoryPanel(category, 20 + (index * 120), 20, this))
        }
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        val mX = Resolution.getMouseX(mouseX.toDouble())
        val mY = Resolution.getMouseY(mouseY.toDouble())
        Resolution.push(context)

        context.drawVerticalGradient(0, 0, Resolution.width, Resolution.height, Style.overlayTopColor, Style.overlayBottomColor)
        context.drawCenteredString("§likun", Resolution.width / 2, 12, Style.accentColor)
        context.drawSearchBar(mX.toFloat(), mY.toFloat())

        val draggingPanel = categoryPanels.find { it.dragging }
        categoryPanels.forEach { if (it != draggingPanel) it.render(context, mX, mY) }
        draggingPanel?.render(context, mX, mY)
        drawThemeButton(context, mX, mY)

        configWindows.forEachIndexed { index, window -> window.render(context, mX, mY, index == configWindows.lastIndex) }

        val cursor = configWindows.asReversed().firstNotNullOfOrNull { it.cursorAt(mX.toFloat(), mY.toFloat()) }
        if (cursor == null) MouseHelper.resetCursor()
        else MouseHelper.setCursor(cursor)

        TooltipManager.draw(context, Resolution.width, Resolution.height)
        Resolution.pop(context)
    }

    private fun GuiGraphicsExtractor.drawSearchBar(mX: Float, mY: Float) {
        val bw = 150f
        val bh = 22f
        val bx = (Resolution.width / 2) - (bw / 2)
        val by = Resolution.height - 40

        drawRect(bx, by, bw, bh, Style.searchBackgroundColor)

        val borderColor = if (searchHandler.listening) Style.accentColor else Style.textColor.withAlpha(30)
        drawRect(bx, by + bh - 2, bw, 2f, borderColor)

        searchHandler.x = bx
        searchHandler.y = by
        searchHandler.width = bw
        searchHandler.height = bh

        if (searchQuery.isEmpty() && ! searchHandler.listening) {
            drawCenteredString("Search...", Resolution.width / 2, by + 7, Style.mutedTextColor, shadow = false)
        }
        else searchHandler.draw(this, mX, mY)
    }

    private fun themeButtonX() = (Resolution.width - 90f).coerceAtLeast(8f)
    private fun overThemeButton(mx: Int, my: Int) = mx >= themeButtonX() && mx < themeButtonX() + 78f && my >= 8 && my < 28

    private fun drawThemeButton(context: GuiGraphicsExtractor, mx: Int, my: Int) {
        val hovered = overThemeButton(mx, my)
        context.drawRect(themeButtonX(), 8, 78, 20, Style.headerBackgroundColor)
        if (hovered) context.drawRect(themeButtonX(), 8, 78, 20, Style.accentColorTrans)
        context.drawRect(themeButtonX(), 26, 78, 2, Style.accentColor)
        context.drawCenteredString("Colors", themeButtonX() + 39f, 14, Style.textColor)
        if (hovered && !isMouseOverConfigWindow(mx, my)) {
            TooltipManager.hover("Customize ClickGUI colors and opacity", mx, my)
        }
    }





    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        KeybindWidget.cancelCapture()
        val mx = Resolution.getMouseX(mouseButtonEvent.x)
        val my = Resolution.getMouseY(mouseButtonEvent.y)
        val button = mouseButtonEvent.button()

        configWindows.asReversed().find { it.contains(mx.toFloat(), my.toFloat()) }?.let { window ->
            focusWindow(window)
            searchHandler.listening = false

            when (window.mouseClicked(mx, my, button)) {
                WindowClickAction.CLOSE -> configWindows.remove(window)
                WindowClickAction.CONSUMED -> {}
            }
            return true
        }

        if (overThemeButton(mx, my) && button in 0..1) {
            openFeatureWindow(ClickGuiTheme)
            return true
        }

        categoryPanels.find { it.isMouseOverHeader(mx.toDouble(), my.toDouble()) }?.let { clickedPanel ->
            clickedPanel.mouseClicked(mx.toDouble(), my.toDouble(), button)
            searchHandler.listening = false
            return true
        }

        if (searchHandler.mouseClicked(mx.toFloat(), my.toFloat(), mouseButtonEvent)) return true

        categoryPanels.asReversed().find { it.isMouseOver(mx, my) }?.let { panel ->
            panel.mouseClicked(mx.toDouble(), my.toDouble(), button)
            return true
        }

        return super.mouseClicked(mouseButtonEvent, bl)
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        val button = mouseButtonEvent.button()

        searchHandler.mouseReleased()
        configWindows.forEach { it.mouseReleased(button) }
        categoryPanels.forEach { it.mouseReleased(button) }
        return super.mouseReleased(mouseButtonEvent)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        KeybindWidget.cancelCapture()
        val mx = Resolution.getMouseX(mouseX)
        val my = Resolution.getMouseY(mouseY)

        configWindows.asReversed().find { it.contains(mx.toFloat(), my.toFloat()) }?.let { window ->
            focusWindow(window)
            window.mouseScrolled(mx, my, vertical)
            return true
        }

        categoryPanels.asReversed().find { it.isMouseOver(mx, my) }?.let { panel ->
            panel.handleScroll(vertical)
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        if (KeybindWidget.capturing) return true
        configWindows.lastOrNull()?.let { window ->
            if (window.charTyped(characterEvent.codepoint.toChar())) {
                return true
            }
        }

        if (searchHandler.keyTyped(characterEvent)) return true
        return super.charTyped(characterEvent)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (keyEvent.key in capturedKeys) return true
        if (KeybindWidget.captureKey(keyEvent.key, keyEvent.scancode, keyEvent.modifiers)) {
            if (keyEvent.key >= 0) capturedKeys.add(keyEvent.key)
            return true
        }
        // Outside key capture, exit keys take priority over feature subwindows.
        if (keyEvent.key == GLFW.GLFW_KEY_ESCAPE ||
            KeyMapping.get("key.ikun.open_gui")?.matches(keyEvent) == true) {
            onClose()
            return true
        }

        configWindows.lastOrNull()?.let { window ->
            if (window.keyPressed(keyEvent.key, keyEvent.scancode, keyEvent.modifiers)) return true
        }

        if (searchHandler.keyPressed(keyEvent)) return true
        if (keyEvent.hasControlDown() && keyEvent.input() == GLFW.GLFW_KEY_F) {
            searchHandler.listening = ! searchHandler.listening
            return true
        }

        return super.keyPressed(keyEvent)
    }

    override fun keyReleased(keyEvent: KeyEvent): Boolean {
        if (capturedKeys.remove(keyEvent.key)) return true
        return super.keyReleased(keyEvent)
    }

    fun openFeatureWindow(feature: Feature, preferredX: Float? = null, preferredY: Float? = null) {
        configWindows.find { it.feature == feature }?.let {
            focusWindow(it)
            searchHandler.listening = false
            return
        }

        searchHandler.listening = false

        val offset = configWindows.size * windowCascadeOffset
        val windowWidth = if (feature === ClickGuiTheme) 280f else defaultWindowWidth
        val windowHeight = if (feature === ClickGuiTheme) 360f else defaultWindowHeight
        val startX = preferredX ?: (((Resolution.width - windowWidth) / 2f) + offset)
        val startY = preferredY ?: (((Resolution.height - windowHeight) / 2f) + offset)

        configWindows.add(FeatureConfigWindow(
            feature = feature,
            startX = startX,
            startY = startY,
            startWidth = windowWidth,
            startHeight = windowHeight
        ).also { it.clampToScreen() })
    }

    fun isMouseOverConfigWindow(mouseX: Int, mouseY: Int) = configWindows.asReversed().any { it.contains(mouseX.toFloat(), mouseY.toFloat()) }





    private fun focusWindow(window: FeatureConfigWindow) {
        configWindows.remove(window)
        configWindows.add(window)
    }

    override fun onClose() {
        KeybindWidget.cancelCapture()
        capturedKeys.clear()
        configWindows.clear()
        searchHandler.listening = false
        super.onClose()
        runCatching { MouseHelper.resetCursor() }
            .onFailure { Ikun.logger.warn("Could not reset the GUI cursor.", it) }
    }

    override fun removed() {
        KeybindWidget.cancelCapture()
        capturedKeys.clear()
        runCatching { ConfigManager.save() }
            .onFailure { Ikun.logger.warn("Could not save Ikun settings when leaving the GUI.", it) }
        super.removed()
    }
}
