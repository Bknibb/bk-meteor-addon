package org.bknibb.bk_meteor_addon.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.bknibb.bk_meteor_addon.MineplayUtils;
import org.bknibb.bk_meteor_addon.modules.MineplayBetterBreak;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(MultiPlayerGameMode.class)
public class BetterBreakMixin {
    @Inject(method = "startDestroyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z", at = @At("HEAD"), cancellable = true)
    private void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (!Modules.get().isActive(MineplayBetterBreak.class)) return;
        if (!MineplayUtils.isOnMineplay()) return;
        if (pos.getY() < 0) return;
        if (mc.gameMode == null || mc.getConnection() == null) return;

        mc.gameMode.destroyBlock(pos);
        mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction));

        ((BetterBreakAccessor) mc.gameMode).setCooldown(5);

        cir.setReturnValue(true);
    }
}
