package com.github.ikun.utils

object NumbersUtils {
    operator fun Number.div(number: Number) = toDouble() / number.toDouble()
    operator fun Number.times(number: Number) = toDouble() * number.toDouble()
    operator fun Number.minus(number: Number) = toDouble() - number.toDouble()
    operator fun Number.plus(number: Number) = toDouble() + number.toDouble()
}
