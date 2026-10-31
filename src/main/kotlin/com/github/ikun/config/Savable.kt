package com.github.ikun.config

import com.google.gson.JsonElement

interface Savable {
    fun write(): JsonElement
    fun read(element: JsonElement)
}
