import com.github.ikun.utils.kuudra.ShopKeyPressGate
import kotlin.test.*

class ShopKeyPressGateTest {
    @Test
    fun `a held key produces only one action until its release`() {
        val gate = ShopKeyPressGate()
        assertTrue(gate.press(49))
        repeat(10) { assertFalse(gate.press(49)) }
        assertTrue(gate.press(50))
        gate.release(49)
        assertTrue(gate.press(49))
        assertFalse(gate.press(50))
    }

    @Test
    fun `same key on the next server screen requires a fresh press before confirmation`() {
        val gate = ShopKeyPressGate()
        assertTrue(gate.press(49), "The first press selects a class")
        gate.blockHeld(listOf(49, 50)) { it == 49 }
        assertFalse(gate.press(49), "Holding the purchase key must not confirm the next screen")
        gate.releaseNotHeld { it == 49 }
        assertFalse(gate.press(49))
        gate.release(49)
        assertTrue(gate.press(49), "A release and fresh press can confirm")
    }

    @Test
    fun `mouse opened confirmation blocks bindings already physically held before the page opened`() {
        val gate = ShopKeyPressGate()
        gate.blockHeld(listOf(-1, 89, 78, 89)) { key ->
            assertTrue(key >= 0, "Unbound keys must not be queried from the keyboard")
            key == 89
        }
        assertFalse(gate.press(89))
        assertTrue(gate.press(78), "An initially released binding can act on its first press")
        gate.release(89)
        assertTrue(gate.press(89))
    }

    @Test
    fun `opening a page with different bindings does not discard a previously held key`() {
        val gate = ShopKeyPressGate()
        assertTrue(gate.press(49))
        gate.blockHeld(listOf(89, 78)) { it == 89 }
        assertFalse(gate.press(49))
        assertFalse(gate.press(89))
        gate.release(49)
        assertTrue(gate.press(49))
    }

    @Test
    fun `physical state recovers missed release events without freeing keys that remain held`() {
        val gate = ShopKeyPressGate()
        assertTrue(gate.press(49))
        assertTrue(gate.press(50))
        val checked = mutableSetOf<Int>()
        gate.releaseNotHeld { key -> checked.add(key); key == 49 }
        assertEquals(setOf(49, 50), checked)
        assertFalse(gate.press(49))
        assertTrue(gate.press(50), "The missed release allows a fresh press")
        gate.releaseNotHeld { false }
        assertTrue(gate.press(49))
        assertTrue(gate.press(50))
    }

    @Test
    fun `reset clears held state and unbound keys never produce actions`() {
        val gate = ShopKeyPressGate()
        assertFalse(gate.press(-1))
        assertFalse(gate.press(Int.MIN_VALUE))
        gate.release(-1)
        gate.blockHeld(listOf(-1)) { fail("Unbound keys should not reach physical keyboard lookup") }
        gate.releaseNotHeld { fail("Rejected keys must not be tracked") }
        assertTrue(gate.press(89))
        gate.reset()
        assertTrue(gate.press(89))
        gate.reset()
        gate.reset()
        assertTrue(gate.press(89))
    }
}
