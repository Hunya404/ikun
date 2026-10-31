package com.github.ikun.features
import com.github.ikun.ui.clickgui.enums.CategoryType
import com.github.ikun.features.impl.general.KuudraParty
import com.github.ikun.features.impl.kuudra.AutoCannonClose
import com.github.ikun.features.impl.kuudra.AutoGFS
import com.github.ikun.features.impl.kuudra.KuudraShop
import com.github.ikun.features.impl.kuudra.SupplySwap
import com.github.ikun.features.impl.cheat.PearlSupplyInteract
import com.github.ikun.features.impl.cheat.RendRotate
object FeatureManager {
    val features: Set<Feature> = linkedSetOf(KuudraParty, AutoCannonClose, KuudraShop, SupplySwap, PearlSupplyInteract, AutoGFS, RendRotate)
    fun getFeaturesByCategory(category: CategoryType) = features.filter { it.category == category }
}
