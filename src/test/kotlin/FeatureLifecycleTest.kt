import com.github.ikun.event.EventBus
import com.github.ikun.event.impl.ChatMessageEvent
import com.github.ikun.event.impl.WorldChangeEvent
import com.github.ikun.features.impl.general.KuudraParty
import com.github.ikun.config.types.ToggleSetting
import kotlin.test.*

class FeatureLifecycleTest {
    @Test
    fun `standalone feature defaults to stats only and toggling does not duplicate listeners`() {
        val feature = KuudraParty
        assertTrue(feature.enabled)
        assertTrue((feature.getSettingByName("Join Stats") as ToggleSetting).value)
        for (setting in listOf("AutoKick", "API", "Check Kuudra T5", "RCM Terror", "TAP", "No Hyp")) {
            assertFalse((feature.getSettingByName(setting) as ToggleSetting).value, setting)
        }
        assertNull(feature.getSettingByName("Query Service"))

        feature.initialize()
        try {
            repeat(3) {
                assertEquals(1, EventBus.listeners[ChatMessageEvent::class.java]?.size)
                assertEquals(1, EventBus.listeners[WorldChangeEvent::class.java]?.size)
                feature.toggle()
                assertFalse(feature.enabled)
                assertNull(EventBus.listeners[ChatMessageEvent::class.java])
                assertNull(EventBus.listeners[WorldChangeEvent::class.java])
                feature.toggle()
                assertTrue(feature.enabled)
            }
        } finally {
            feature.onDisable()
        }
    }
}
