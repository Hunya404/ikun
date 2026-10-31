import com.github.ikun.config.types.NumberSetting
import com.github.ikun.config.types.ActionSetting
import com.github.ikun.config.Savable
import com.github.ikun.features.impl.kuudra.KuudraShop
import com.github.ikun.ui.clickgui.components.settings.WidgetFactory
import com.github.ikun.ui.clickgui.components.settings.impl.NumberWidget
import com.github.ikun.ui.clickgui.components.settings.impl.ActionWidget
import com.google.gson.JsonParser
import kotlin.test.*

class NumberSettingTest {
    private fun scale() = NumberSetting("Render Scale", 1.0, 0.1, 2.0, 0.1, "x")

    @Test
    fun `scale is discoverable in the feature right click settings and has a slider`() {
        val setting = KuudraShop.getSettingByName("Render Scale")
        assertIs<NumberSetting>(setting)
        assertEquals(1.0, setting.defaultValue)
        assertIs<NumberWidget>(WidgetFactory.fromSetting(setting))
        val editorAction = KuudraShop.configSettings.find { it.name == "HUD Edit" }
        assertIs<ActionSetting>(editorAction)
        assertIs<ActionWidget>(WidgetFactory.fromSetting(editorAction))
        assertFalse((editorAction as Any) is Savable)
    }

    @Test
    fun `saved scale survives json serialization and snaps to supported steps`() {
        val source = scale()
        source.set(0.64)
        assertEquals(0.6, source.value, 1e-9)
        val restored = scale()
        restored.read(JsonParser.parseString(source.write().toString()))
        assertEquals(source.value, restored.value)
        restored.reset()
        assertEquals(1.0, restored.value)
    }

    @Test
    fun `out of range config values are bounded and malformed values preserve the setting`() {
        val setting = scale()
        setting.read(JsonParser.parseString("20"))
        assertEquals(2.0, setting.value)
        setting.read(JsonParser.parseString("-20"))
        assertEquals(0.1, setting.value)
        for (invalid in listOf("\"NaN\"", "\"Infinity\"", "\"bad\"", "null")) {
            assertFails { setting.read(JsonParser.parseString(invalid)) }
            assertEquals(0.1, setting.value)
        }
    }
}
