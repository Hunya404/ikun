package com.github.ikun

import com.github.ikun.config.ConfigManager
import com.github.ikun.event.EventBus
import com.github.ikun.event.impl.ChatMessageEvent
import com.github.ikun.event.impl.WorldChangeEvent
import com.github.ikun.features.FeatureManager
import com.github.ikun.features.impl.general.KuudraParty
import com.github.ikun.ui.clickgui.ClickGuiScreen
import com.github.ikun.ui.kuudra.KuudraShopEditorScreen
import com.github.ikun.utils.PartyUtils
import com.mojang.brigadier.arguments.StringArgumentType
import kotlinx.coroutines.*
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory

object Ikun: ClientModInitializer {
    const val MOD_ID = "ikun"
    const val MOD_NAME = "ikun"
    const val MOD_VERSION = "1.0.0"
    val PREFIX = Component.literal("§b§l[ikun]§r")
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineName(MOD_NAME))
    @JvmField val logger = LoggerFactory.getLogger(MOD_NAME)
    @JvmStatic val mc get() = Minecraft.getInstance()
    @JvmField var isLoaded = false

    override fun onInitializeClient() {
        PartyUtils.init()
        FeatureManager.features.forEach { it.initialize() }
        ConfigManager.load()
        isLoaded = true

        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) EventBus.post(ChatMessageEvent(message))
        }
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ -> EventBus.post(WorldChangeEvent) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            EventBus.post(WorldChangeEvent)
            PartyUtils.reset()
        }
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(literal("ikun").then(literal("hudedit").executes {
                mc.execute { mc.setScreen(KuudraShopEditorScreen()) }
                1
            }).then(literal("retry").then(argument("id", StringArgumentType.word()).executes {
                KuudraParty.retryPlayer(StringArgumentType.getString(it, "id"))
                1
            })).then(literal("kick").then(argument("id", StringArgumentType.word()).executes {
                KuudraParty.kickPlayer(StringArgumentType.getString(it, "id"))
                1
            })).executes {
                mc.execute { mc.setScreen(ClickGuiScreen()) }
                1
            })
        }
        val category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"))
        val openGui = KeyMappingHelper.registerKeyMapping(KeyMapping("key.ikun.open_gui", GLFW.GLFW_KEY_RIGHT_SHIFT, category))
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (openGui.consumeClick()) if (client.screen == null) client.setScreen(ClickGuiScreen())
            FeatureManager.features.forEach { if (it.enabled) it.tick(client) }
        }
        ClientLifecycleEvents.CLIENT_STOPPING.register {
            ConfigManager.save()
            FeatureManager.features.forEach { it.onDisable() }
            scope.cancel()
        }
    }
}
