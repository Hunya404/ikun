package com.github.ikun.utils
fun String.startsWithOneOf(vararg prefixes: String) = prefixes.any { startsWith(it) }
fun String.remove(vararg values: String) = values.fold(this) { text, value -> text.replace(value, "") }
fun <T> T.equalsOneOf(vararg values: T) = values.contains(this)
