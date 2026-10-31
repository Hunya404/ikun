package com.github.ikun.mixin;

import com.github.ikun.utils.rotation.RatHumanRotation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class RendRotationRenderMixin {
    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("HEAD"))
    private void ikun$rendRotation(DeltaTracker deltaTracker, boolean tick, CallbackInfo ci) {
        RatHumanRotation.onFrame();
    }
}
