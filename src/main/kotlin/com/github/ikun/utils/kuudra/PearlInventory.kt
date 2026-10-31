package com.github.ikun.utils.kuudra

import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

object PearlInventory {
    fun count(stacks: Iterable<ItemStack>): Int = stacks.sumOf { stack ->
        if (stack.isEmpty) 0 else {
            val tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
            val id = tag.getStringOr("id", "").ifEmpty {
                tag.getCompoundOrEmpty("ExtraAttributes").getStringOr("id", "")
            }
            if (id == "ENDER_PEARL") stack.count else 0
        }
    }
}
