package com.github.ikun.config.types

import com.github.ikun.config.ConfigHolder
import com.github.ikun.config.Savable
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import kotlin.math.roundToInt

class NumberSetting(
    name: String, defaultValue: Double, val minimum: Double, val maximum: Double,
    val step: Double, val suffix: String = ""
): ConfigHolder<Double>(name, defaultValue), Savable {
    init {
        require(minimum.isFinite() && maximum.isFinite() && minimum < maximum)
        require(step.isFinite() && step > 0 && defaultValue.isFinite() && defaultValue in minimum..maximum)
    }

    fun normalized(amount: Double): Double {
        require(amount.isFinite()) { "Setting must be a finite number" }
        val steps = ((amount.coerceIn(minimum, maximum) - minimum) / step).roundToInt()
        return (minimum + steps * step).coerceIn(minimum, maximum)
    }

    fun set(amount: Double) { value = normalized(amount) }
    override fun write() = JsonPrimitive(normalized(value))
    override fun read(element: JsonElement) { set(element.asDouble) }
}
