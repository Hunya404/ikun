import com.github.ikun.config.Savable
import com.github.ikun.config.types.KeybindSetting
import com.github.ikun.features.impl.kuudra.KuudraShop
import com.github.ikun.ui.clickgui.components.settings.WidgetFactory
import com.github.ikun.ui.clickgui.components.settings.impl.KeybindWidget
import com.github.ikun.utils.kuudra.ShopCardId
import com.github.ikun.utils.kuudra.ShopKeyBindings
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.lwjgl.glfw.GLFW
import kotlin.test.*

class ShopKeySettingsTest {
    private val names = mapOf(
        ShopCardId.CLASS_ROUTE to "Class Key",
        ShopCardId.BALLISTA to "Ballista Key",
        ShopCardId.CANNON to "Cannon Key",
        ShopCardId.OPEN_SHOP to "Fill your Quiver Key",
        ShopCardId.CONFIRM to "Confirm Key",
        ShopCardId.CANCEL to "Cancel Key"
    )
    private val defaults = mapOf(
        ShopCardId.CLASS_ROUTE to GLFW.GLFW_KEY_1,
        ShopCardId.BALLISTA to GLFW.GLFW_KEY_1,
        ShopCardId.CANNON to GLFW.GLFW_KEY_2,
        ShopCardId.OPEN_SHOP to GLFW.GLFW_KEY_3,
        ShopCardId.CONFIRM to GLFW.GLFW_KEY_1,
        ShopCardId.CANCEL to GLFW.GLFW_KEY_2
    )
    private val custom = mapOf(
        ShopCardId.CLASS_ROUTE to GLFW.GLFW_KEY_G,
        ShopCardId.BALLISTA to GLFW.GLFW_KEY_B,
        ShopCardId.CANNON to GLFW.GLFW_KEY_C,
        ShopCardId.OPEN_SHOP to GLFW.GLFW_KEY_O,
        ShopCardId.CONFIRM to GLFW.GLFW_KEY_Y,
        ShopCardId.CANCEL to GLFW.GLFW_KEY_N
    )
    private val pages = listOf(
        listOf(ShopCardId.CLASS_ROUTE, ShopCardId.OPEN_SHOP),
        listOf(ShopCardId.BALLISTA, ShopCardId.CANNON, ShopCardId.OPEN_SHOP),
        listOf(ShopCardId.CONFIRM, ShopCardId.CANCEL)
    )

    private fun resetKeys() {
        ShopCardId.entries.forEach { KuudraShop.keySetting(it).set(KeybindSetting.UNBOUND) }
        ShopCardId.entries.forEach { KuudraShop.keySetting(it).reset() }
    }

    private fun withDefaultKeys(test: () -> Unit) {
        resetKeys()
        try {
            test()
        } finally {
            resetKeys()
        }
    }

    @Test
    fun `all six card bindings are saved right click settings with editable key widgets`() = withDefaultKeys {
        val registered = KuudraShop.configSettings.filterIsInstance<KeybindSetting>()
        assertEquals(names.values.toSet(), registered.map { it.name }.toSet())
        assertEquals(6, registered.size)
        for ((id, name) in names) {
            val setting = assertIs<KeybindSetting>(KuudraShop.getSettingByName(name))
            assertSame(setting, KuudraShop.keySetting(id))
            assertIs<Savable>(setting)
            assertIs<KeybindWidget>(WidgetFactory.fromSetting(setting))
            assertEquals(defaults.getValue(id), setting.defaultValue)
            assertEquals(defaults.getValue(id), KuudraShop.keyFor(id))
            assertEquals(KeybindSetting.keyName(defaults.getValue(id)), KuudraShop.keyLabel(id))
        }
    }

    @Test
    fun `custom settings change both card labels and visible card keyboard targets`() = withDefaultKeys {
        val labels = mapOf(ShopCardId.CLASS_ROUTE to "G", ShopCardId.BALLISTA to "B",
            ShopCardId.CANNON to "C", ShopCardId.OPEN_SHOP to "O", ShopCardId.CONFIRM to "Y", ShopCardId.CANCEL to "N")
        for ((id, key) in custom) {
            assertTrue(KuudraShop.keySetting(id).set(key))
            assertEquals(key, KuudraShop.keyFor(id))
            assertEquals(labels.getValue(id), KuudraShop.keyLabel(id))
        }
        for (page in pages) {
            page.forEachIndexed { index, id ->
                assertEquals(index, ShopKeyBindings.indexForKey(page, custom.getValue(id), KuudraShop::keyFor))
            }
            assertNull(ShopKeyBindings.indexForKey(page, GLFW.GLFW_KEY_1, KuudraShop::keyFor))
            assertNull(ShopKeyBindings.indexForKey(page, GLFW.GLFW_KEY_2, KuudraShop::keyFor))
        }
    }

    @Test
    fun `open shop shortcut rejects conflicts on both the class and perk pages`() = withDefaultKeys {
        val entrance = KuudraShop.keySetting(ShopCardId.OPEN_SHOP)
        assertTrue(KuudraShop.keySetting(ShopCardId.CLASS_ROUTE).set(GLFW.GLFW_KEY_G))
        assertTrue(KuudraShop.keySetting(ShopCardId.BALLISTA).set(GLFW.GLFW_KEY_B))
        assertFalse(entrance.set(GLFW.GLFW_KEY_G))
        assertFalse(entrance.set(GLFW.GLFW_KEY_B))
        assertFalse(entrance.set(GLFW.GLFW_KEY_2))
        assertEquals(GLFW.GLFW_KEY_3, entrance.value)
        assertTrue(entrance.set(GLFW.GLFW_KEY_O))
        assertFalse(KuudraShop.keySetting(ShopCardId.CLASS_ROUTE).set(GLFW.GLFW_KEY_O))
        assertFalse(KuudraShop.keySetting(ShopCardId.CANNON).set(GLFW.GLFW_KEY_O))
        assertTrue(KuudraShop.keySetting(ShopCardId.CONFIRM).set(GLFW.GLFW_KEY_O))
    }

    @Test
    fun `bindings reject same page conflicts without changing the previous key`() = withDefaultKeys {
        for (page in pages.filter { it.size == 2 }) {
            val first = KuudraShop.keySetting(page[0])
            val second = KuudraShop.keySetting(page[1])
            assertFalse(first.set(second.value))
            assertEquals(defaults.getValue(page[0]), first.value)
            assertFalse(second.set(first.value))
            assertEquals(defaults.getValue(page[1]), second.value)
            assertTrue(first.set(GLFW.GLFW_KEY_F))
            assertFalse(second.set(GLFW.GLFW_KEY_F))
            assertEquals(defaults.getValue(page[1]), second.value)
            assertTrue(first.set(KeybindSetting.UNBOUND))
            assertTrue(second.set(GLFW.GLFW_KEY_F))
        }
    }

    @Test
    fun `different shop pages can reuse a key while each page keeps a single target`() = withDefaultKeys {
        val sharedKeyCards = listOf(ShopCardId.CLASS_ROUTE, ShopCardId.BALLISTA, ShopCardId.CONFIRM)
        for (id in sharedKeyCards) assertTrue(KuudraShop.keySetting(id).set(GLFW.GLFW_KEY_F))
        for (page in pages) {
            val index = assertNotNull(ShopKeyBindings.indexForKey(page, GLFW.GLFW_KEY_F, KuudraShop::keyFor))
            assertEquals(sharedKeyCards[pages.indexOf(page)], page[index])
        }
    }

    @Test
    fun `saved custom binding json restores the same settings and card labels`() = withDefaultKeys {
        val saved = JsonObject()
        for ((id, key) in custom) {
            val setting = KuudraShop.keySetting(id)
            assertTrue(setting.set(key))
            saved.add(setting.jsonName, setting.write())
        }
        val serialized = JsonParser.parseString(saved.toString()).asJsonObject
        ShopCardId.entries.forEach { assertTrue(KuudraShop.keySetting(it).set(KeybindSetting.UNBOUND)) }
        for ((id, key) in custom) {
            val setting = KuudraShop.keySetting(id)
            setting.read(serialized.get(setting.jsonName))
            assertEquals(key, KuudraShop.keyFor(id))
            assertEquals(('A'.code + key - GLFW.GLFW_KEY_A).toChar().toString(), KuudraShop.keyLabel(id))
        }
    }

    @Test
    fun `unbound settings hide card key labels and survive saving and loading`() = withDefaultKeys {
        for (id in ShopCardId.entries) {
            val setting = KuudraShop.keySetting(id)
            assertTrue(setting.set(KeybindSetting.UNBOUND))
            assertEquals(KeybindSetting.UNBOUND, KuudraShop.keyFor(id))
            assertNull(KuudraShop.keyLabel(id))
            val serialized = JsonParser.parseString(setting.write().toString())
            assertEquals(KeybindSetting.UNBOUND, serialized.asInt)
            setting.reset()
            setting.read(serialized)
            assertEquals(KeybindSetting.UNBOUND, KuudraShop.keyFor(id))
            assertNull(KuudraShop.keyLabel(id))
        }
        for (page in pages) {
            for (key in listOf(GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, KeybindSetting.UNBOUND)) {
                assertNull(ShopKeyBindings.indexForKey(page, key, KuudraShop::keyFor))
            }
        }
    }
}
