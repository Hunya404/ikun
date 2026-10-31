import com.github.ikun.config.types.KeybindSetting
import com.github.ikun.ui.clickgui.components.settings.WidgetFactory
import com.github.ikun.ui.clickgui.components.settings.impl.KeybindWidget
import com.google.gson.JsonParser
import org.lwjgl.glfw.GLFW
import kotlin.test.*

class KeybindSettingTest {
    @Test
    fun `editable keys enforce conflicts while saved bindings load independently`() {
        val setting = KeybindSetting("Cannon key", GLFW.GLFW_KEY_2)
        setting.validator = { it != GLFW.GLFW_KEY_1 }
        assertFalse(setting.set(GLFW.GLFW_KEY_1))
        assertEquals(GLFW.GLFW_KEY_2, setting.value)
        assertTrue(setting.set(GLFW.GLFW_KEY_F8))
        val restored = KeybindSetting("Cannon key", GLFW.GLFW_KEY_2)
        restored.validator = { false }
        restored.read(JsonParser.parseString(setting.write().toString()))
        assertEquals(GLFW.GLFW_KEY_F8, restored.value)
        assertTrue(restored.set(KeybindSetting.UNBOUND))
        assertEquals("-1", restored.write().toString())
        restored.reset()
        assertEquals(KeybindSetting.UNBOUND, restored.value)
        setting.reset()
        assertEquals(GLFW.GLFW_KEY_2, setting.value)
    }

    @Test
    fun `malformed fractional overflowing and unsupported saved codes preserve the binding`() {
        val setting = KeybindSetting("Cannon key", GLFW.GLFW_KEY_2)
        for (json in listOf("null", "[]", "{}", "true", "\"65\"", "65.5", "4294967361", "-2", "0", "64", "256", "270", "315", "349")) {
            setting.read(JsonParser.parseString(json))
            assertEquals(GLFW.GLFW_KEY_2, setting.value, json)
        }
        assertFalse(setting.set(GLFW.GLFW_KEY_ESCAPE))
        assertFalse(setting.set(64))
        setting.read(JsonParser.parseString("-1"))
        assertEquals(KeybindSetting.UNBOUND, setting.value)
        setting.read(JsonParser.parseString("65.0"))
        assertEquals(GLFW.GLFW_KEY_A, setting.value)
    }

    @Test
    fun `binding widget is available and names work without a client window`() {
        assertIs<KeybindWidget>(WidgetFactory.fromSetting(KeybindSetting("Cannon key", GLFW.GLFW_KEY_2)))
        assertEquals("Unbound", KeybindSetting.keyName(KeybindSetting.UNBOUND))
        assertEquals("2", KeybindSetting.keyName(GLFW.GLFW_KEY_2))
        assertEquals("A", KeybindSetting.keyName(GLFW.GLFW_KEY_A))
        assertEquals("F25", KeybindSetting.keyName(GLFW.GLFW_KEY_F25))
        assertEquals("Num 7", KeybindSetting.keyName(GLFW.GLFW_KEY_KP_7))
        assertEquals("Right Shift", KeybindSetting.keyName(GLFW.GLFW_KEY_RIGHT_SHIFT))
        assertEquals("Page Down", KeybindSetting.keyName(GLFW.GLFW_KEY_PAGE_DOWN))
        assertEquals("Unknown", KeybindSetting.keyName(64))
    }
}
