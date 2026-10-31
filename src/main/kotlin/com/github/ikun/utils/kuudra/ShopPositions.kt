package com.github.ikun.utils.kuudra

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/** A card keeps its position when the server changes its slot, name, or tier. */
enum class ShopCardId(val defaultRect: ShopRect, val shortcut: Int? = null) {
    CLASS_ROUTE(ShopRect(118, 126, 204, 68), 1),
    BALLISTA(ShopRect(12, 42, 204, 68), 1),
    CANNON(ShopRect(12, 118, 204, 68), 2),
    OPEN_SHOP(ShopRect(224, 194, 204, 68), 3),
    CONFIRM(ShopRect(108, 142, 104, 36), 1),
    CANCEL(ShopRect(228, 142, 104, 36), 2);

    companion object {
        /** Missing or ambiguous products do not transfer their key to another card. */
        fun indexForShortcut(ids: List<ShopCardId>, number: Int): Int? =
            ids.indices.singleOrNull { ids[it].shortcut == number }
    }
}

/** The card's bounds after independent scaling, in the global shop's virtual coordinates. */
data class ShopCardPlacement(val x: Double, val y: Double, val scale: Double, val width: Double, val height: Double) {
    fun contains(localX: Double, localY: Double) =
        localX >= x && localX < x + width && localY >= y && localY < y + height
}

/** Saved positions are centers relative to the screen, independent of the shop scale. */
class ShopPositions {
    private data class Center(val x: Double, val y: Double)
    private val centers = mutableMapOf<ShopCardId, Center>()
    private val scales = mutableMapOf<ShopCardId, Double>()
    var revision: Int = 0
        private set

    fun scale(id: ShopCardId): Double = scales[id] ?: 1.0

    fun setScale(id: ShopCardId, value: Double) {
        if (!value.isFinite()) return
        val requested = value.coerceIn(0.1, 2.0)
        if (scale(id) == requested) return
        if (requested == 1.0) scales.remove(id) else scales[id] = requested
        revision++
    }

    fun resetScale(id: ShopCardId) {
        if (scales.remove(id) != null) revision++
    }

    fun placement(id: ShopCardId, layout: ShopLayout, screenWidth: Int, screenHeight: Int): ShopCardPlacement {
        val base = id.defaultRect
        val screenW = screenWidth.coerceAtLeast(1).toDouble()
        val screenH = screenHeight.coerceAtLeast(1).toDouble()
        val actualScale = minOf(scale(id), screenW / (base.width * layout.scale), screenH / (base.height * layout.scale))
        val width = base.width * actualScale
        val height = base.height * actualScale
        // Keep existing integer positions exactly when independent scaling is unused.
        if (actualScale == 1.0) {
            val legacy = unscaledRect(id, layout, screenWidth, screenHeight)
            val left = layout.left + legacy.x * layout.scale
            val top = layout.top + legacy.y * layout.scale
            if (left >= 0.0 && top >= 0.0 && left + width * layout.scale <= screenW && top + height * layout.scale <= screenH)
                return ShopCardPlacement(legacy.x.toDouble(), legacy.y.toDouble(), 1.0, width, height)
        }
        val center = centers[id]
        val centerX = center?.let { layout.localX(it.x * screenW) } ?: (base.x + base.width / 2.0)
        val centerY = center?.let { layout.localY(it.y * screenH) } ?: (base.y + base.height / 2.0)
        val minX = -layout.left / layout.scale
        val minY = -layout.top / layout.scale
        val maxX = ((screenW - layout.left) / layout.scale - width).coerceAtLeast(minX)
        val maxY = ((screenH - layout.top) / layout.scale - height).coerceAtLeast(minY)
        return ShopCardPlacement((centerX - width / 2).coerceIn(minX, maxX),
            (centerY - height / 2).coerceIn(minY, maxY), actualScale, width, height)
    }

    fun rect(id: ShopCardId, layout: ShopLayout, screenWidth: Int, screenHeight: Int): ShopRect {
        if (scale(id) == 1.0) return unscaledRect(id, layout, screenWidth, screenHeight)
        val placed = placement(id, layout, screenWidth, screenHeight)
        return ShopRect(placed.x.roundToInt(), placed.y.roundToInt(),
            placed.width.roundToInt().coerceAtLeast(1), placed.height.roundToInt().coerceAtLeast(1))
    }

    private fun unscaledRect(id: ShopCardId, layout: ShopLayout, screenWidth: Int, screenHeight: Int): ShopRect {
        val base = id.defaultRect
        val center = centers[id] ?: return base
        val x = (center.x * screenWidth.coerceAtLeast(1) - base.width * layout.scale / 2 - layout.left) / layout.scale
        val y = (center.y * screenHeight.coerceAtLeast(1) - base.height * layout.scale / 2 - layout.top) / layout.scale
        // Round in virtual coordinates, then clamp there too: rounding at the screen edge
        // must not put even a fraction of a pixel outside the screen.
        val minX = ceil(-layout.left / layout.scale).toInt()
        val minY = ceil(-layout.top / layout.scale).toInt()
        val maxX = floor((screenWidth.coerceAtLeast(1) - layout.left) / layout.scale - base.width).toInt().coerceAtLeast(minX)
        val maxY = floor((screenHeight.coerceAtLeast(1) - layout.top) / layout.scale - base.height).toInt().coerceAtLeast(minY)
        return base.copy(x = x.roundToInt().coerceIn(minX, maxX), y = y.roundToInt().coerceIn(minY, maxY))
    }

    fun move(id: ShopCardId, screenLeft: Double, screenTop: Double, layout: ShopLayout, screenWidth: Int, screenHeight: Int) {
        if (!screenLeft.isFinite() || !screenTop.isFinite()) return
        val width = screenWidth.coerceAtLeast(1).toDouble()
        val height = screenHeight.coerceAtLeast(1).toDouble()
        val placed = placement(id, layout, screenWidth, screenHeight)
        val cardWidth = placed.width * layout.scale
        val cardHeight = placed.height * layout.scale
        val left = screenLeft.coerceIn(0.0, (width - cardWidth).coerceAtLeast(0.0))
        val top = screenTop.coerceIn(0.0, (height - cardHeight).coerceAtLeast(0.0))
        val center = Center((left + cardWidth / 2) / width, (top + cardHeight / 2) / height)
        if (centers[id] != center) {
            centers[id] = center
            revision++
        }
    }

    fun reset(id: ShopCardId? = null) {
        val changed = if (id == null) {
            (centers.isNotEmpty() || scales.isNotEmpty()).also { centers.clear(); scales.clear() }
        } else {
            val positionRemoved = centers.remove(id) != null
            val scaleRemoved = scales.remove(id) != null
            positionRemoved || scaleRemoved
        }
        if (changed) revision++
    }

    fun read(element: JsonElement) {
        if (!element.isJsonObject) return
        val root = element.asJsonObject
        for (id in ShopCardId.entries) {
            val value = root.get(id.name.lowercase()) ?: continue
            if (!value.isJsonObject) continue
            val fields = value.asJsonObject
            val x = coordinate(fields.get("x"))
            val y = coordinate(fields.get("y"))
            if (x != null && y != null) {
                val center = Center(x, y)
                if (centers[id] != center) {
                    centers[id] = center
                    revision++
                }
            }
            number(fields.get("scale"))?.takeIf { it in 0.1..2.0 }?.let { setScale(id, it) }
        }
    }

    fun write(): JsonObject = JsonObject().apply {
        for (id in ShopCardId.entries) {
            val center = centers[id]
            val requested = scales[id]
            if (center == null && requested == null) continue
            add(id.name.lowercase(), JsonObject().apply {
                center?.let { addProperty("x", it.x); addProperty("y", it.y) }
                requested?.let { addProperty("scale", it) }
            })
        }
    }

    private fun coordinate(element: JsonElement?): Double? = number(element)?.takeIf { it in 0.0..1.0 }

    private fun number(element: JsonElement?): Double? {
        if (element == null || !element.isJsonPrimitive || !element.asJsonPrimitive.isNumber) return null
        return runCatching { element.asDouble }.getOrNull()?.takeIf { it.isFinite() }
    }
}
