package com.github.ikun.mixin;

import com.github.ikun.features.impl.kuudra.KuudraShop;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the live server container; only its presentation changes. */
@Mixin(Screen.class)
public abstract class ShopScreenMixin {
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"), cancellable = true)
    private void ikun$shop(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (KuudraShop.render((Screen) (Object) this, graphics, mouseX, mouseY)) {
            graphics.extractDeferredElements(mouseX, mouseY, delta);
            ci.cancel();
        }
    }
}
