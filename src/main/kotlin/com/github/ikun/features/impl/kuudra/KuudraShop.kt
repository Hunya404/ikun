package com.github.ikun.features.impl.kuudra

import com.github.ikun.features.Feature
import com.github.ikun.config.types.NumberSetting
import com.github.ikun.config.types.ColorSetting
import com.github.ikun.config.types.ActionSetting
import com.github.ikun.config.types.KeybindSetting
import com.github.ikun.ui.kuudra.KuudraShopEditorScreen
import com.github.ikun.ui.clickgui.components.settings.Style
import com.github.ikun.utils.kuudra.ShopPositions
import com.github.ikun.event.impl.WorldChangeEvent
import com.github.ikun.ui.kuudra.KuudraShopView
import com.github.ikun.utils.kuudra.ShopConfirmationFlow
import com.github.ikun.utils.kuudra.ShopConfirmActions
import com.github.ikun.utils.kuudra.ShopCardId
import com.github.ikun.utils.kuudra.ShopKeyPressGate
import com.github.ikun.utils.location.LocationUtils
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.ChestMenu
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.roundToInt

object KuudraShop: Feature("ClickGUI-style buttons for the Kuudra Perk Menu. Purchases use the original server slots.", "Kuudra Shop") {
    val renderScale by NumberSetting("Render Scale", 1.0, 0.1, 2.0, 0.1, "x")
        .withDescription("Set the overall shop scale. Use HUD Edit to resize each card separately")
    val cardPositions = ShopPositions()
    private val hudEdit by ActionSetting("HUD Edit") {
        val client = Minecraft.getInstance()
        client.setScreen(KuudraShopEditorScreen(client.screen))
    }.withDescription("Drag cards to move; scroll over a card to resize it independently")
    val backgroundColor by ColorSetting("Background Color", Style.featureBackgroundColor.rgb and 0xFFFFFF).section("Appearance")
        .withDescription("Drag the hue strip and color field to choose the button background. Right-click to reset")
    val backgroundOpacity by NumberSetting("Background Opacity", 70.6, 0.0, 100.0, 0.1, "%").section("Appearance")
        .withDescription("0% hides the button background; 100% is opaque. Applies to all shop and confirmation buttons")
    fun buttonBackground() = Color(backgroundColor.value or
        ((backgroundOpacity.value.coerceIn(0.0, 100.0) * 255 / 100).roundToInt() shl 24), true)
    private val classKey by KeybindSetting("Class Key", GLFW.GLFW_KEY_1).section("Keybinds")
        .withDescription("Class entrance shortcut. Click then press a key; Backspace/Delete clears; right-click resets")
    private val ballistaKey by KeybindSetting("Ballista Key", GLFW.GLFW_KEY_1).section("Keybinds")
        .withDescription("Ballista Mechanic shortcut. Click then press a key; Backspace/Delete clears; right-click resets")
    private val cannonKey by KeybindSetting("Cannon Key", GLFW.GLFW_KEY_2).section("Keybinds")
        .withDescription("Human Cannonball shortcut. Click then press a key; Backspace/Delete clears; right-click resets")
    private val openShopKey by KeybindSetting("Fill your Quiver Key", GLFW.GLFW_KEY_3).section("Keybinds")
        .withDescription("Fill your Quiver shortcut. Click then press a key; Backspace/Delete clears; right-click resets")
    private val confirmKey by KeybindSetting("Confirm Key", GLFW.GLFW_KEY_1).section("Keybinds")
        .withDescription("Confirmation shortcut. Release the class key before pressing this key; right-click resets")
    private val cancelKey by KeybindSetting("Cancel Key", GLFW.GLFW_KEY_2).section("Keybinds")
        .withDescription("Cancel shortcut. Click then press a key; Backspace/Delete clears; right-click resets")
    private val keyGroups = listOf(listOf(ShopCardId.CLASS_ROUTE, ShopCardId.OPEN_SHOP),
        listOf(ShopCardId.BALLISTA, ShopCardId.CANNON, ShopCardId.OPEN_SHOP),
        listOf(ShopCardId.CONFIRM, ShopCardId.CANCEL))
    private var view: KuudraShopView? = null
    private val confirmationFlow = ShopConfirmationFlow()
    private val keyPressGate = ShopKeyPressGate()

    init {
        ShopCardId.entries.forEach { id ->
            val peers = keyGroups.filter { id in it }.flatten().distinct().filter { it != id }
            keySetting(id).validator = { key -> peers.none { keyFor(it) == key } }
        }
    }

    fun keySetting(id: ShopCardId): KeybindSetting = when (id) {
        ShopCardId.CLASS_ROUTE -> classKey
        ShopCardId.BALLISTA -> ballistaKey
        ShopCardId.CANNON -> cannonKey
        ShopCardId.OPEN_SHOP -> openShopKey
        ShopCardId.CONFIRM -> confirmKey
        ShopCardId.CANCEL -> cancelKey
    }
    fun keyFor(id: ShopCardId): Int = keySetting(id).value
    fun keyLabel(id: ShopCardId): String? = keyFor(id).takeIf { it != KeybindSetting.UNBOUND }?.let { KeybindSetting.keyName(it) }
    private fun isKeyDown(key: Int): Boolean = key >= 0 && KeybindSetting.isValidKey(key) &&
        GLFW.glfwGetKey(Minecraft.getInstance().window.handle(), key) == GLFW.GLFW_PRESS

    override fun init() {
        register<WorldChangeEvent> { view = null; confirmationFlow.reset(); keyPressGate.reset() }
        ScreenEvents.BEFORE_INIT.register { _, screen, _, _ ->
            if (screen !is AbstractContainerScreen<*> || screen.menu !is ChestMenu) return@register
            ScreenMouseEvents.allowMouseClick(screen).register { current, event ->
                active(current)?.let { it.click(event); false } ?: true
            }
            ScreenMouseEvents.allowMouseRelease(screen).register { current, _ -> active(current) == null }
            ScreenMouseEvents.allowMouseDrag(screen).register { current, _, _, _ -> active(current) == null }
            ScreenMouseEvents.allowMouseScroll(screen).register { current, _, _, _, _ -> active(current) == null }
            ScreenKeyboardEvents.allowKeyPress(screen).register { current, event ->
                active(current)?.let { it.key(event); false } ?: true
            }
            ScreenKeyboardEvents.allowKeyRelease(screen).register { current, event ->
                keyPressGate.release(event.key)
                active(current) == null
            }
            ScreenEvents.remove(screen).register { if (view?.screen === it) view = null }
        }
    }

    private fun active(screen: Screen): KuudraShopView? {
        val client = Minecraft.getInstance()
        if (!enabled || !LocationUtils.onHypixel || screen !== client.screen ||
            screen !is AbstractContainerScreen<*>) return null
        val menu = screen.menu as? ChestMenu ?: return null
        if (client.player?.containerMenu !== menu) return null
        val now = System.currentTimeMillis()
        val title = screen.title.string
        confirmationFlow.observe(title, now)
        val confirmation = title == "Are you sure?"
        if (title != "Perk Menu" && !confirmation) return null
        if (confirmation && view?.screen !== screen) {
            if (!confirmationFlow.pending(now)) return null
            val names = menu.slots.take(menu.rowCount * 9).mapIndexed { index, slot -> index to slot.item.hoverName.string }
            if (ShopConfirmActions.find(names, menu.rowCount * 9) == null) return null
            confirmationFlow.reset()
        }
        if (view?.screen !== screen) {
            // A held selection key must not activate a freshly opened confirmation page.
            keyPressGate.releaseNotHeld(::isKeyDown)
            keyPressGate.blockHeld(ShopCardId.entries.map(::keyFor), ::isKeyDown)
            view = KuudraShopView(screen, menu, confirmation,
                requestedScale = { renderScale.value },
                positions = cardPositions,
                binding = ::keyFor,
                keyPressGate = keyPressGate,
                onAction = { if (!confirmation) confirmationFlow.selected(System.currentTimeMillis()) },
                onExit = { confirmationFlow.reset() })
        }
        return view
    }

    override fun tick(client: Minecraft) {
        keyPressGate.releaseNotHeld(::isKeyDown)
        confirmationFlow.observe(client.screen?.title?.string, System.currentTimeMillis())
    }

    @JvmStatic
    fun render(screen: Screen, graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int): Boolean {
        val current = active(screen) ?: return false
        current.render(graphics, mouseX, mouseY)
        return true
    }

    override fun onDisable() { super.onDisable(); view = null; confirmationFlow.reset(); keyPressGate.reset() }
}
