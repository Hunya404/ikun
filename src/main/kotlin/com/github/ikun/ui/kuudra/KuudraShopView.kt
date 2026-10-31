package com.github.ikun.ui.kuudra

import com.github.ikun.utils.kuudra.ShopEntries
import com.github.ikun.utils.kuudra.ShopLayout
import com.github.ikun.utils.kuudra.ShopCardPlacement
import com.github.ikun.utils.kuudra.ShopConfirmActions
import com.github.ikun.utils.kuudra.ShopItem
import com.github.ikun.utils.kuudra.ShopCardId
import com.github.ikun.utils.kuudra.ShopPositions
import com.github.ikun.utils.kuudra.ShopKeyBindings
import com.github.ikun.utils.kuudra.ShopKeyPressGate
import com.github.ikun.config.types.KeybindSetting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW

class KuudraShopView(
    val screen: AbstractContainerScreen<*>, private val menu: ChestMenu,
    private val confirmation: Boolean = false,
    private val requestedScale: () -> Double = { 1.0 },
    private val positions: ShopPositions = ShopPositions(),
    private val binding: (ShopCardId) -> Int = { id -> id.shortcut?.let { GLFW.GLFW_KEY_1 + it - 1 } ?: KeybindSetting.UNBOUND },
    private val keyPressGate: ShopKeyPressGate = ShopKeyPressGate(),
    private val onAction: () -> Unit = {}, private val onExit: () -> Unit = {}
) {
    private val client get() = Minecraft.getInstance()
    private var displayed = emptyList<Entry>()
    private val hover = mutableMapOf<Int, Float>()
    private var lastFrame = System.nanoTime()
    private var lastPurchase = 0L
    private var renderedWidth = 0
    private var renderedHeight = 0
    private var renderedScale = 0.0
    private var renderedRevision = -1
    private var renderedCards = emptyList<ShopCardPlacement>()
    private var renderedIds = emptyList<ShopCardId>()
    private data class Entry(val slot: Int, val stack: ItemStack, val summary: List<String>)
    private fun layout() = ShopLayout(screen.width, screen.height, requestedScale())
    private fun keyLabel(id: ShopCardId) = binding(id).takeIf { it != KeybindSetting.UNBOUND }?.let { KeybindSetting.keyName(it) }
    private fun cardId(index: Int): ShopCardId =
        if (confirmation) { if (index == 0) ShopCardId.CONFIRM else ShopCardId.CANCEL }
        else if (ShopEntries.isOpenShop(displayed[index].slot, displayed[index].stack.hoverName.string, menu.rowCount * 9)) ShopCardId.OPEN_SHOP
        else when (val name = ShopEntries.plain(displayed[index].stack.hoverName.string).trim().lowercase()) {
            "human cannonball" -> ShopCardId.CANNON
            else -> if (name.startsWith("ballista mechanic")) ShopCardId.BALLISTA else ShopCardId.CLASS_ROUTE
        }

    fun render(g: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        var entries = (0 until menu.rowCount * 9).mapNotNull { index ->
            val stack = menu.slots.getOrNull(index)?.item ?: return@mapNotNull null
            if (stack.isEmpty) return@mapNotNull null
            val lore = stack.get(DataComponents.LORE)?.lines()?.map { it.string }.orEmpty()
            if (!ShopEntries.include(index, menu.rowCount * 9, BuiltInRegistries.ITEM.getKey(stack.item).toString(), stack.hoverName.string, lore)) return@mapNotNull null
            Entry(index, stack.copy(), ShopEntries.summary(lore))
        }
        if (confirmation) {
            val actions = ShopConfirmActions.find(entries.map { it.slot to it.stack.hoverName.string }, menu.rowCount * 9)
            entries = actions?.let { targets -> listOfNotNull(entries.find { it.slot == targets.confirm }, entries.find { it.slot == targets.cancel }) }.orEmpty()
        } else {
            val visible = ShopEntries.visibleSlots(entries.map {
                ShopItem(it.slot, BuiltInRegistries.ITEM.getKey(it.stack.item).toString(), it.stack.hoverName.string)
            }, menu.rowCount * 9)
            entries = entries.filter { it.slot in visible }
        }
        displayed = entries
        renderedWidth = screen.width
        renderedHeight = screen.height
        val l = layout()
        renderedScale = l.scale
        renderedRevision = positions.revision
        renderedIds = displayed.indices.map(::cardId)
        renderedCards = renderedIds.map { positions.placement(it, l, screen.width, screen.height) }
        val mx = l.localX(mouseX.toDouble())
        val my = l.localY(mouseY.toDouble())
        val now = System.nanoTime()
        val step = ((now - lastFrame) / 150_000_000f).coerceIn(0f, 1f)
        lastFrame = now
        g.nextStratum()
        g.pose().pushMatrix()
        g.pose().translate(l.left.toFloat(), l.top.toFloat())
        g.pose().scale(l.scale.toFloat(), l.scale.toFloat())
        var tooltip: ItemStack? = null
        displayed.forEachIndexed { index, entry ->
            g.nextStratum()
            val rect = renderedCards[index]
            val isHovered = rect.contains(mx, my)
            val previous = hover[entry.slot] ?: 0f
            val progress = (previous + (if (isHovered) step else -step)).coerceIn(0f, 1f)
            hover[entry.slot] = progress
            val id = renderedIds[index]
            val displayStack = if (id == ShopCardId.OPEN_SHOP) entry.stack.copy().apply {
                set(DataComponents.CUSTOM_NAME, Component.literal("Fill your Quiver").withStyle(entry.stack.hoverName.style))
            } else entry.stack
            if (confirmation) ShopCardRenderer.confirmationButton(g, rect, id, isHovered, keyLabel(id))
            else ShopCardRenderer.draw(g, rect, id, displayStack, entry.summary, keyLabel(id), progress)
            if (!confirmation && isHovered) tooltip = displayStack
        }
        if (!confirmation && displayed.isEmpty()) g.centeredText(client.font, "Waiting for shop items...", 220, 145, 0xFFB9CCD9.toInt())
        g.pose().popMatrix()
        tooltip?.let { g.setTooltipForNextFrame(client.font, it, mouseX, mouseY) }
    }

    fun click(event: MouseButtonEvent) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return
        val l = layout()
        val x = l.localX(event.x)
        val y = l.localY(event.y)
        renderedCards.indices.reversed().firstOrNull { renderedCards[it].contains(x, y) }?.let(::purchase)
    }

    fun key(event: KeyEvent) {
        when {
            event.key == GLFW.GLFW_KEY_ESCAPE -> close()
            event.key >= 0 && ShopCardId.entries.any { binding(it) == event.key } -> {
                if (keyPressGate.press(event.key)) ShopKeyBindings.indexForKey(renderedIds, event.key, binding)?.let(::purchase)
            }
            client.options.keyInventory.matches(event) -> close()
        }
    }

    private fun close() { onExit(); screen.onClose() }

    private fun purchase(index: Int) {
        val entry = displayed.getOrNull(index) ?: return
        val player = client.player ?: return
        // Do not apply an old card to a different menu, changed item, or resized layout.
        if (client.screen !== screen || player.containerMenu !== menu || screen.width != renderedWidth || screen.height != renderedHeight || layout().scale != renderedScale || positions.revision != renderedRevision) return
        if (entry.slot !in 0 until menu.rowCount * 9 || !ItemStack.matches(menu.slots[entry.slot].item, entry.stack)) return
        val now = System.nanoTime()
        if (lastPurchase != 0L && now - lastPurchase < 200_000_000L) return
        val gameMode = client.gameMode ?: return
        lastPurchase = now
        onAction()
        gameMode.handleContainerInput(menu.containerId, entry.slot, 0, ContainerInput.PICKUP, player)
    }

}
