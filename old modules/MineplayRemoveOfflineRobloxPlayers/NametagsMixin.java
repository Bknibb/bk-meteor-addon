package org.bknibb.bk_meteor_addon.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Nametags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import org.bknibb.bk_meteor_addon.MineplayUtils;
import org.bknibb.bk_meteor_addon.modules.MineplayRemoveOfflineRobloxPlayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//@Mixin(Nametags.class)
//public class NametagsMixin {
//    @Redirect(method = "onTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getType()Lnet/minecraft/entity/EntityType;"))
//    private EntityType<?> redirectGetType(Entity entity) {
//        if (entity instanceof PlayerEntity player) {
//            if (MineplayUtils.hidePlayer(player) && Modules.get().get(MineplayRemoveOfflineRobloxPlayers.class).hidePlayerInNametags.get()) {
//                return null;
//            }
//        }
//        return entity.getType();
//    }
//}
