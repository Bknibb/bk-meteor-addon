package org.bknibb.bk_meteor_addon.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.border.WorldBorder;
import org.bknibb.bk_meteor_addon.MineplayUtils;
import org.bknibb.bk_meteor_addon.modules.MineplayBetterBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void onPreMove(CallbackInfo ci) {
        if (!Modules.get().isActive(MineplayBetterBorder.class)) return;
        if (MeteorClient.mc.world == null || MeteorClient.mc.player == null) return;
        if (!MineplayUtils.isOnMineplay()) return;

        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        WorldBorder border = player.clientWorld.getWorldBorder();

        double shrink = Modules.get().get(MineplayBetterBorder.class).shrinkBy.get();
        double minX = border.getBoundWest();
        double maxX = border.getBoundEast();
        double minZ = border.getBoundNorth();
        double maxZ = border.getBoundSouth();

        Vec3d pos = player.getPos();
        Vec3d vel = player.getVelocity();

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
            player.setPosition(clampedX, pos.y, clampedZ);
            // Soft cancel motion only in directions of collision
            player.setVelocity(vx, vel.y, vz);
        }
    }
}
