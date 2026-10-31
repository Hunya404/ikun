import com.github.ikun.event.EventBus
import com.github.ikun.event.impl.ChatMessageEvent
import com.github.ikun.event.impl.WorldChangeEvent
import com.github.ikun.features.impl.kuudra.AutoCannonClose
import com.github.ikun.utils.kuudra.CannonCloseWindow
import kotlin.test.*

class CannonCloseWindowTest {
    @Test
    fun `purchase arms the next 500 milliseconds only for Perk Menu`() {
        val window = CannonCloseWindow()
        assertFalse(window.shouldClose("Perk Menu", 1_000))
        window.chat("You purchased Human Cannonball!", 1_000)
        assertTrue(window.shouldClose("Perk Menu", 1_000))
        assertTrue(window.shouldClose("Perk Menu", 1_499))
        assertFalse(window.shouldClose("Perk Menu", 1_500))
        assertFalse(window.shouldClose("Perk Menu", 2_000))
        assertFalse(window.shouldClose("Inventory", 1_100))
        assertFalse(window.shouldClose("Croesus", 1_100))
    }

    @Test
    fun `other purchases and unrelated messages do not arm or extend the window`() {
        val window = CannonCloseWindow()
        window.chat("You purchased Rapid Fire!", 1_000)
        assertFalse(window.shouldClose("Perk Menu", 1_100))
        window.chat("You purchased Human Cannonball!", 1_000)
        window.chat("Party > example: hello", 1_400)
        assertFalse(window.shouldClose("Perk Menu", 1_500))
    }

    @Test
    fun `closing disabling or changing worlds cancels the pending close`() {
        val window = CannonCloseWindow()
        window.chat("You purchased Human Cannonball!", 1_000)
        window.reset()
        assertFalse(window.shouldClose("Perk Menu", 1_100))
        window.chat("You purchased Human Cannonball!", 2_000)
        assertTrue(window.shouldClose("Perk Menu", 2_100))
    }

    @Test
    fun `feature defaults off and repeated toggling keeps listener counts stable`() {
        val feature = AutoCannonClose
        assertFalse(feature.enabled)
        val chatBefore = EventBus.listeners[ChatMessageEvent::class.java]?.size ?: 0
        val worldBefore = EventBus.listeners[WorldChangeEvent::class.java]?.size ?: 0
        feature.initialize()
        try {
            repeat(3) {
                feature.toggle()
                assertEquals(chatBefore + 1, EventBus.listeners[ChatMessageEvent::class.java]?.size)
                assertEquals(worldBefore + 1, EventBus.listeners[WorldChangeEvent::class.java]?.size)
                feature.toggle()
                assertEquals(chatBefore, EventBus.listeners[ChatMessageEvent::class.java]?.size ?: 0)
                assertEquals(worldBefore, EventBus.listeners[WorldChangeEvent::class.java]?.size ?: 0)
            }
        } finally {
            if (feature.enabled) feature.toggle()
            feature.onDisable()
        }
    }
}
