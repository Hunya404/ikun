package com.github.ikun.mixin;

import com.github.ikun.features.impl.kuudra.SupplySwap;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class SupplySwapMixin {
    @Inject(method = "useItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), cancellable = true)
    private void ikun$supplyUse(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (SupplySwap.beforeUseItem(player, hand)) cir.setReturnValue(InteractionResult.SUCCESS);
    }

    @Inject(method = "useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), cancellable = true)
    private void ikun$supplyUseOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (SupplySwap.beforeUseItem(player, hand)) cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
