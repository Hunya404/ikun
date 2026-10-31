package com.github.ikun.config

import com.github.ikun.Ikun
import com.github.ikun.features.impl.general.KuudraParty
import com.github.ikun.features.impl.kuudra.AutoCannonClose
import com.github.ikun.features.impl.kuudra.AutoGFS
import com.github.ikun.features.impl.kuudra.KuudraShop
import com.github.ikun.features.impl.kuudra.SupplySwap
import com.github.ikun.features.impl.cheat.PearlSupplyInteract
import com.github.ikun.features.impl.cheat.RendRotate
import com.github.ikun.ui.clickgui.ClickGuiTheme
import com.github.ikun.utils.FileHandler
import com.github.ikun.utils.kuudra.ShopCardId
import com.github.ikun.config.types.KeybindSetting
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader

object ConfigManager {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configDir get() = FabricLoader.getInstance().configDir
    private val path get() = configDir.resolve("ikun/config.json").toFile()

    fun load() {
        runCatching {
            val file = if (path.exists()) path else configDir.resolve("IloveKu/config.json").toFile()
            if (file.exists()) {
                val root = JsonParser.parseString(file.readText()).asJsonObject
                ClickGuiTheme.palette.read(root.get("clickGuiTheme")).forEach { key ->
                    Ikun.logger.warn("Ignoring invalid ClickGUI theme setting: {}", key)
                }
                read(root.getAsJsonObject("settings"), root.get("enabled")?.asBoolean)
                val cannonClose = root.get("autoCannonClose")?.asBoolean ?: false
                if (cannonClose != AutoCannonClose.enabled) AutoCannonClose.toggle()
                val supplySwap = root.get("supplySwap")?.asBoolean ?: false
                if (supplySwap != SupplySwap.enabled) SupplySwap.toggle()
                val pearlSupplyInteract = root.get("pearlSupplyInteract")?.asBoolean ?: false
                if (pearlSupplyInteract != PearlSupplyInteract.enabled) PearlSupplyInteract.toggle()
                val autoGfs = root.get("autoGfs")?.asBoolean ?: false
                if (autoGfs != AutoGFS.enabled) AutoGFS.toggle()
                root.getAsJsonObject("autoGfsSettings")?.entrySet()?.forEach { (key, value) ->
                    runCatching { (AutoGFS.getSettingByName(key) as? Savable)?.read(value) }
                        .onFailure { Ikun.logger.warn("Ignoring invalid AutoGFS setting: {}", key) }
                }
                val rendRotate = root.get("rendRotate")?.asBoolean ?: false
                root.getAsJsonObject("rendRotateSettings")?.entrySet()?.forEach { (key, value) ->
                    runCatching { (RendRotate.getSettingByName(key) as? Savable)?.read(value) }
                        .onFailure { Ikun.logger.warn("Ignoring invalid Rend Rotate setting: {}", key) }
                }
                if (rendRotate != RendRotate.enabled) RendRotate.toggle()
                val shop = root.get("kuudraShop")?.asBoolean ?: false
                if (shop != KuudraShop.enabled) KuudraShop.toggle()
                val shopSettings = root.getAsJsonObject("kuudraShopSettings")
                shopSettings?.entrySet()?.forEach { (key, value) ->
                    val settingName = if (key == "Open Shop Key") "Fill your Quiver Key" else key
                    if (key == "Open Shop Key" && shopSettings.has(settingName)) return@forEach
                    runCatching { (KuudraShop.getSettingByName(settingName) as? Savable)?.read(value) }
                        .onFailure { Ikun.logger.warn("Ignoring invalid Kuudra Shop setting: {}", key) }
                }
                // Keep old custom keys; an occupied default leaves only the new shortcut unbound.
                val openShopKey = KuudraShop.keySetting(ShopCardId.OPEN_SHOP)
                val hasSavedQuiverKey = shopSettings?.has(openShopKey.jsonName) == true || shopSettings?.has("Open Shop Key") == true
                if (!hasSavedQuiverKey && !openShopKey.validator(openShopKey.value)) {
                    openShopKey.set(KeybindSetting.UNBOUND)
                }
                root.get("kuudraShopPositions")?.let { KuudraShop.cardPositions.read(it) }
                if (file != path) save()
            } else {
                // Only import this feature's settings from the old default profile.
                val legacy = configDir.resolve("NoammAddons/config.json").toFile()
                if (legacy.exists()) {
                    val root = JsonParser.parseString(legacy.readText()).asJsonObject
                    val entry = root.getAsJsonArray("config")?.firstOrNull {
                        it.asJsonObject.get("name")?.asString == "KuudraParty"
                    }?.asJsonObject
                    if (entry != null) read(entry.getAsJsonObject("configSettings"), entry.get("enabled")?.asBoolean)
                }
                save()
            }
        }.onFailure { Ikun.logger.warn("Could not load Ikun settings; using available defaults.", it) }
    }

    private fun read(settings: JsonObject?, enabled: Boolean?) {
        settings?.entrySet()?.forEach { (key, value) ->
            runCatching { (KuudraParty.getSettingByName(key) as? Savable)?.read(value) }
                .onFailure { Ikun.logger.warn("Ignoring invalid Ikun setting: {}", key) }
        }
        if (enabled != null && enabled != KuudraParty.enabled) KuudraParty.toggle()
    }

    fun save() {
        val settings = JsonObject()
        KuudraParty.configSettings.forEach { setting ->
            if (setting is Savable) settings.add(setting.jsonName, setting.write())
        }
        val root = JsonObject().apply {
            addProperty("version", 1)
            add("clickGuiTheme", ClickGuiTheme.palette.write())
            addProperty("enabled", KuudraParty.enabled)
            addProperty("autoCannonClose", AutoCannonClose.enabled)
            addProperty("supplySwap", SupplySwap.enabled)
            addProperty("pearlSupplyInteract", PearlSupplyInteract.enabled)
            addProperty("autoGfs", AutoGFS.enabled)
            addProperty("rendRotate", RendRotate.enabled)
            add("rendRotateSettings", JsonObject().apply {
                RendRotate.configSettings.forEach { setting ->
                    if (setting is Savable) add(setting.jsonName, setting.write())
                }
            })
            add("autoGfsSettings", JsonObject().apply {
                AutoGFS.configSettings.forEach { setting ->
                    if (setting is Savable) add(setting.jsonName, setting.write())
                }
            })
            addProperty("kuudraShop", KuudraShop.enabled)
            add("kuudraShopPositions", KuudraShop.cardPositions.write())
            add("kuudraShopSettings", JsonObject().apply {
                KuudraShop.configSettings.forEach { setting ->
                    if (setting is Savable) add(setting.jsonName, setting.write())
                }
            })
            add("settings", settings)
        }
        FileHandler(path).write(gson.toJson(root))
    }
}
