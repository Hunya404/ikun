package com.github.ikun.ui.kuudra

import com.github.ikun.Ikun
import com.github.ikun.config.ConfigManager
import com.github.ikun.features.impl.kuudra.KuudraShop
import com.github.ikun.utils.kuudra.ShopCardId
import com.github.ikun.utils.kuudra.ShopLayout
import com.github.ikun.utils.kuudra.ShopRect
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW
import kotlin.math.roundToInt
import kotlin.math.sign

class KuudraShopEditorScreen(private val parent: Screen? = null): Screen(Component.literal("Kuudra Shop HUD Edit")) {
    private var group = 1
    private var dragging: ShopCardId? = null
    private var offsetX = 0.0
    private var offsetY = 0.0
    private val groups = listOf(listOf(ShopCardId.CLASS_ROUTE, ShopCardId.OPEN_SHOP),
        listOf(ShopCardId.BALLISTA, ShopCardId.CANNON, ShopCardId.OPEN_SHOP),
        listOf(ShopCardId.CONFIRM, ShopCardId.CANCEL))
    private val labels = listOf("Class", "Perks", "Confirm")
    private val previews by lazy {
        mapOf(ShopCardId.CLASS_ROUTE to preview(Items.DIAMOND_SWORD, "Specialist Route"),
            ShopCardId.BALLISTA to preview(Items.CRAFTING_TABLE, "Ballista Mechanic II"),
            ShopCardId.CANNON to preview(Items.PLAYER_HEAD, "Human Cannonball"),
            ShopCardId.OPEN_SHOP to preview(Items.PLAYER_HEAD, "Fill your Quiver"),
            ShopCardId.CONFIRM to preview(Items.GREEN_TERRACOTTA, "Confirm"),
            ShopCardId.CANCEL to preview(Items.RED_TERRACOTTA, "Cancel"))
    }
    private fun preview(item: net.minecraft.world.item.Item, name: String) = ItemStack(item).apply {
        set(DataComponents.CUSTOM_NAME, Component.literal(name).withColor(0x55FF55))
    }
    private fun layout() = ShopLayout(width, height, KuudraShop.renderScale.value)
    private fun tabs(index: Int) = ShopRect(12 + index * 70, 32, 64, 24)
    private fun done() = ShopRect((width - 84).coerceAtLeast(1), 8, 72, 24)
    private fun reset() = ShopRect((width - 166).coerceAtLeast(1), 8, 74, 24)
    private fun overToolbar(x: Double, y: Double) = done().contains(x, y) || reset().contains(x, y) ||
        labels.indices.any { tabs(it).contains(x, y) }
    private fun cardAt(x: Double, y: Double, l: ShopLayout = layout()): ShopCardId? {
        if (overToolbar(x, y)) return null
        return groups[group].asReversed().firstOrNull {
            KuudraShop.cardPositions.placement(it, l, width, height).contains(l.localX(x), l.localY(y))
        }
    }

    override fun extractBackground(g: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) = Unit

    override fun extractRenderState(g: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val l = layout()
        for (x in 0..width step 20) g.fill(x, 60, x + 1, height, 0x20FFFFFF)
        for (y in 60..height step 20) g.fill(0, y, width, y + 1, 0x20FFFFFF)
        g.pose().pushMatrix()
        g.pose().translate(l.left.toFloat(), l.top.toFloat())
        g.pose().scale(l.scale.toFloat(), l.scale.toFloat())
        groups[group].forEach { id ->
            g.nextStratum()
            val rect = KuudraShop.cardPositions.placement(id, l, width, height)
            val hovered = cardAt(mouseX.toDouble(), mouseY.toDouble(), l) == id
            if (group == 2) ShopCardRenderer.confirmationButton(g, rect, id, hovered, KuudraShop.keyLabel(id))
            else ShopCardRenderer.draw(g, rect, id, previews.getValue(id),
                listOf("Scroll: ${(KuudraShop.cardPositions.scale(id) * 100).roundToInt()}%", "Right-click to reset"),
                KuudraShop.keyLabel(id), if (hovered) 1f else 0f, if (dragging == id) 0xFF73DEEB.toInt() else null)
        }
        g.pose().popMatrix()
        // Toolbar draws after cards; editor input has the same priority.
        g.nextStratum()
        g.text(font, "HUD Edit", 12, 13, 0xFF73DEEB.toInt(), true)
        g.centeredText(font, font.plainSubstrByWidth("Drag: move | Scroll over card: resize | Right-click: reset | R: reset group", (width - 24).coerceAtLeast(1)),
            width / 2, height - 14, 0xFF73DEEB.toInt())
        labels.forEachIndexed { index, label ->
            val rect = tabs(index)
            ShopCardRenderer.button(g, rect, label, group == index || rect.contains(mouseX.toDouble(), mouseY.toDouble()))
        }
        ShopCardRenderer.button(g, reset(), "Reset", reset().contains(mouseX.toDouble(), mouseY.toDouble()))
        ShopCardRenderer.button(g, done(), "Done", done().contains(mouseX.toDouble(), mouseY.toDouble()))
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (done().contains(event.x, event.y)) { onClose(); return true }
            if (reset().contains(event.x, event.y)) { resetGroup(); return true }
            labels.indices.firstOrNull { tabs(it).contains(event.x, event.y) }?.let { group = it; dragging = null; return true }
        }
        val l = layout()
        val id = cardAt(event.x, event.y, l) ?: return true
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            dragging = null
            KuudraShop.cardPositions.reset(id)
        }
        else if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            val rect = KuudraShop.cardPositions.placement(id, l, width, height)
            dragging = id
            offsetX = event.x - (l.left + rect.x * l.scale)
            offsetY = event.y - (l.top + rect.y * l.scale)
        }
        return true
    }

    override fun mouseDragged(event: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        dragging?.let { KuudraShop.cardPositions.move(it, event.x - offsetX, event.y - offsetY, layout(), width, height) }
        return true
    }
    override fun mouseReleased(event: MouseButtonEvent): Boolean { dragging = null; return true }
    override fun mouseScrolled(x: Double, y: Double, horizontal: Double, vertical: Double): Boolean {
        if (vertical.isFinite() && vertical != 0.0) {
            val id = cardAt(x, y) ?: return true
            dragging = null
            val steps = (KuudraShop.cardPositions.scale(id) * 10).roundToInt() + sign(vertical).toInt()
            KuudraShop.cardPositions.setScale(id, steps / 10.0)
        }
        return true
    }
    override fun keyPressed(event: KeyEvent): Boolean {
        when (event.key) {
            GLFW.GLFW_KEY_ESCAPE -> onClose()
            GLFW.GLFW_KEY_R -> resetGroup()
            else -> return super.keyPressed(event)
        }
        return true
    }
    private fun resetGroup() { dragging = null; groups[group].forEach { KuudraShop.cardPositions.reset(it) } }
    override fun onClose() {
        dragging = null
        minecraft.setScreen(parent)
    }
    override fun removed() {
        dragging = null
        runCatching { ConfigManager.save() }.onFailure { Ikun.logger.warn("Could not save Kuudra Shop positions.", it) }
        super.removed()
    }
    override fun isPauseScreen() = false
}
