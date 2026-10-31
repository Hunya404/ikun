package com.github.ikun.utils
import java.awt.Color
object ColorUtils {
    fun Color.withAlpha(value: Int) = Color(red, green, blue, value.coerceIn(0, 255))
    fun Color.lerp(other: Color, value: Number): Color {
        val t = value.toFloat().coerceIn(0f, 1f)
        fun blend(a: Int, b: Int) = (a + (b - a) * t).toInt()
        return Color(blend(red, other.red), blend(green, other.green), blend(blue, other.blue), blend(alpha, other.alpha))
    }
}
