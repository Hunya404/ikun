package com.github.ikun.mixin;

import com.github.ikun.features.impl.cheat.RendRotate;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class RendRotationPacketMixin {
    @Inject(method = "handleMovePlayer(Lnet/minecraft/network/protocol/game/ClientboundPlayerPositionPacket;)V", at = @At("TAIL"))
    private void ikun$rendPosition(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        RendRotate.INSTANCE.onPositionChange(packet);
    }

    @Inject(method = "handlePlayerInfoUpdate", at = @At("TAIL"))
    private void ikun$rendArea(ClientboundPlayerInfoUpdatePacket packet, CallbackInfo ci) {
        RendRotate.INSTANCE.onPlayerInfo(packet);
    }
}
