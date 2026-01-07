package org.bknibb.bk_meteor_addon.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface BetterBreakAccessor {
    @Accessor("destroyDelay")
    void setCooldown(int cooldown);

    @Accessor("destroyDelay")
    int getCooldown();
}
