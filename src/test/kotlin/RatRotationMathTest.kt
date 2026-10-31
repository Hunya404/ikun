import com.github.ikun.utils.rotation.RatHumanRotation
import com.github.ikun.utils.rotation.RotationMath
import net.minecraft.world.phys.Vec3
import kotlin.test.*

class RatRotationMathTest {
    @Test fun `source smoother step golden values retain endpoints and midpoint`() {
        val cases = listOf(0f to 0f, 0.25f to 0.103515625f, 0.5f to 0.5f,
            0.75f to 0.896484375f, 1f to 1f)
        cases.forEach { (input, expected) -> assertEquals(expected, RotationMath.smootherStep(input), 0.000001f) }
        assertEquals(-25f, RotationMath.lerp(-30f, -20f, 0.5f))
    }

    @Test fun `flick pulse is zero outside its window and peaks at its center`() {
        assertEquals(0f, RotationMath.pulse(0.2f, 0.5f, 0.1f))
        assertEquals(0f, RotationMath.pulse(0.8f, 0.5f, 0.1f))
        assertEquals(1f, RotationMath.pulse(0.5f, 0.5f, 0.1f), 0.000001f)
        assertEquals(0.70710677f, RotationMath.pulse(0.55f, 0.5f, 0.1f), 0.000001f)
    }

    @Test fun `mouse quantization takes shortest yaw across the wrap boundary`() {
        val angles = RotationMath.quantize(179f, 0f, -179f, -30f, 0.5f)
        assertEquals(180.95f, angles.yaw(), 0.0001f)
        assertEquals(-30f, angles.pitch(), 0.0001f)
    }

    @Test fun `sub step motion and negative half steps retain Java rounding`() {
        assertEquals(0f, RotationMath.quantize(0f, 0f, 0.06f, 0f, 0.5f).yaw())
        assertEquals(0.15f, RotationMath.quantize(0f, 0f, 0.09f, 0f, 0.5f).yaw(), 0.000001f)
        assertEquals(-0.15f, RotationMath.quantize(0f, 0f, -0.09f, 0f, 0.5f).yaw(), 0.000001f)
        assertEquals(0f, RotationMath.quantize(0f, 0f, -0.075f, 0f, 0.5f).yaw())
    }

    @Test fun `rotation never pushes pitch outside the vanilla range`() {
        assertEquals(90f, RotationMath.quantize(0f, 89f, 0f, 120f, 0.5f).pitch())
        assertEquals(-90f, RotationMath.quantize(0f, -89f, 0f, -120f, 0.5f).pitch())
    }

    @Test fun `source profile preserves overshoot noise duration and timeout settings`() {
        val p = RatHumanRotation.Profile.speeds(85f, 145f)
        assertEquals(85f, p.minSpeed())
        assertEquals(145f, p.maxSpeed())
        assertEquals(0.1f, p.minDuration())
        assertEquals(0.9f, p.maxDuration())
        assertEquals(0.78f, p.overshootChance())
        assertEquals(0.2f, p.minOvershoot())
        assertEquals(1.65f, p.maxOvershoot())
        assertEquals(0f, p.flickChance())
        assertEquals(0.075f, p.microCorrection())
        assertEquals(1.5f, p.timeout())
        assertEquals(295f, RatHumanRotation.Profile.speeds(295f, 235f).maxSpeed())
    }

    @Test fun `direction angles agree with source yaw orientation`() {
        assertEquals(90f, RatHumanRotation.look(Vec3(-1.0, 0.0, 0.0), Vec3.ZERO)[0], 0.00001f)
        assertEquals(-90f, RatHumanRotation.look(Vec3(1.0, 0.0, 0.0), Vec3.ZERO)[0], 0.00001f)
        assertEquals(-90f, RatHumanRotation.look(Vec3(0.0, 1.0, 0.0), Vec3.ZERO)[1], 0.00001f)
    }
}
