package org.bknibb.bk_meteor_addon.mixin;

import net.minecraft.client.gui.screen.TitleScreen;
import org.bknibb.bk_meteor_addon.BkMeteorAddon;
import org.bknibb.bk_meteor_addon.ConfigModifier;
import org.bknibb.bk_meteor_addon.update_system.UpdateSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Unique private static boolean firstTime = true;
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"))
    private void onRender(CallbackInfo ci) {
        if (firstTime) {
            firstTime = false;
            if (ConfigModifier.get().checkForUpdates.get()) {
                UpdateSystem.checkForUpdates(BkMeteorAddon.INSTNACE);
            }
        }
    }
}
