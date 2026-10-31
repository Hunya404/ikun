import com.github.ikun.config.types.ColorSetting
import com.github.ikun.config.types.NumberSetting
import com.github.ikun.features.impl.kuudra.KuudraShop
import com.github.ikun.ui.clickgui.components.settings.WidgetFactory
import com.github.ikun.ui.clickgui.components.settings.impl.ColorWidget
import com.google.gson.JsonParser
import java.awt.Color
import kotlin.test.*

class ShopColorTest {
    @Test
    fun `color json preserves leading zeros and rejects invalid colors without replacing the value`() {
        val setting = ColorSetting("Background Color", 0x0F0F0F)
        setting.set(0x00007A)
        assertEquals("\"#00007A\"", setting.write().toString())
        val restored = ColorSetting("Background Color", 0)
        restored.read(JsonParser.parseString(setting.write().toString()))
        assertEquals(setting.value, restored.value)
        restored.read(JsonParser.parseString("\"#aBcDeF\""))
        assertEquals(0xABCDEF, restored.value)
        for (invalid in listOf("null", "12", "\"#FFF\"", "\"#FF000000\"", "\"#ZZZZZZ\"")) {
            assertFails { restored.read(JsonParser.parseString(invalid)) }
            assertEquals(0xABCDEF, restored.value)
        }
    }

    @Test
    fun `shop picker and opacity are discoverable and full transparency preserves the chosen rgb`() {
        val rgb = KuudraShop.getSettingByName("Background Color")
        val opacity = KuudraShop.getSettingByName("Background Opacity")
        assertIs<ColorSetting>(rgb)
        assertIs<NumberSetting>(opacity)
        assertIs<ColorWidget>(WidgetFactory.fromSetting(rgb))
        val originalRgb = rgb.value
        val originalOpacity = opacity.value
        try {
            rgb.set(0x237AB4)
            opacity.set(0.0)
            assertEquals(Color(0x23, 0x7A, 0xB4, 0), KuudraShop.buttonBackground())
            opacity.set(100.0)
            assertEquals(Color(0x23, 0x7A, 0xB4, 255), KuudraShop.buttonBackground())
            opacity.set(50.0)
            assertEquals(128, KuudraShop.buttonBackground().alpha)
        } finally {
            rgb.value = originalRgb
            opacity.value = originalOpacity
        }
    }

    @Test
    fun `choosing a hue while the color is gray preserves that hue when saturation is added`() {
        val setting = ColorSetting("Color", 0x777777)
        val picker = ColorWidget(setting).apply { x = 0; y = 0; width = 140 }
        assertTrue(picker.mouseClicked(69.5, 45.0, 0)) // Hue 0.5, still gray.
        picker.mouseReleased(0)
        assertTrue(picker.mouseClicked(129.0, 58.0, 0)) // Full saturation and brightness.
        picker.mouseReleased(0)
        assertEquals(0x00FFFF, setting.value)
    }
}
