package org.bknibb.bk_meteor_addon.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import org.bknibb.bk_meteor_addon.MineplayUtils;
import org.bknibb.bk_meteor_addon.modules.MineplayBetterBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class ClientPlayerEntityMixin {
    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onPreMove(CallbackInfo ci) {
        if (!Modules.get().isActive(MineplayBetterBorder.class)) return;
        if (MeteorClient.mc.level == null || MeteorClient.mc.player == null) return;
        if (!MineplayUtils.isOnMineplay()) return;

        LocalPlayer player = (LocalPlayer)(Object)this;
        WorldBorder border = player.level().getWorldBorder();

        double shrink = Modules.get().get(MineplayBetterBorder.class).shrinkBy.get();
        double minX = border.getMinX();
        double maxX = border.getMaxX();
        double minZ = border.getMinZ();
        double maxZ = border.getMaxZ();

        Vec3 pos = player.position();
        Vec3 vel = player.getDeltaMovement();

        // Allow player to go outside by up to `shrink` blocks before enforcing the barrier
        if (pos.x < minX - shrink || pos.x > maxX + shrink || pos.z < minZ - shrink || pos.z > maxZ + shrink) {
            return; // truly outside, do nothing
        }

        double clampedX = pos.x;
        double clampedZ = pos.z;
        double vx = vel.x;
        double vz = vel.z;

        boolean moved = false;

        // Check X axis
        if (pos.x < minX + shrink) {
            clampedX = minX + shrink;
            if (vx < 0) vx = 0;
            moved = true;
        } else if (pos.x > maxX - shrink) {
            clampedX = maxX - shrink;
            if (vx > 0) vx = 0;
            moved = true;
        }

        // Check Z axis
        if (pos.z < minZ + shrink) {
            clampedZ = minZ + shrink;
            if (vz < 0) vz = 0;
            moved = true;
        } else if (pos.z > maxZ - shrink) {
            clampedZ = maxZ - shrink;
            if (vz > 0) vz = 0;
            moved = true;
        }

        if (moved) {
            // Hard correct position
            player.setPos(clampedX, pos.y, clampedZ);
            // Soft cancel motion only in directions of collision
            player.setDeltaMovement(vx, vel.y, vz);
        }
    }
}
