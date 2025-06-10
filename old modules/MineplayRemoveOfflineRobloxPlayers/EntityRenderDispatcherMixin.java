package org.bknibb.bk_meteor_addon.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.WorldView;
import org.bknibb.bk_meteor_addon.MineplayUtils;
import org.bknibb.bk_meteor_addon.modules.MineplayRemoveOfflineRobloxPlayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//@Mixin(EntityRenderDispatcher.class)
//public abstract class EntityRenderDispatcherMixin {
//    @Shadow
//    private static void renderShadow(MatrixStack matrices, VertexConsumerProvider vertexConsumers, EntityRenderState renderState, float opacity, float tickDelta, WorldView world, float radius) {}
//    @Redirect(method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;renderShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/render/entity/state/EntityRenderState;FFLnet/minecraft/world/WorldView;F)V"))
//    private <E extends Entity> void cancelShadowRendering(MatrixStack matrices, VertexConsumerProvider vertexConsumers, EntityRenderState renderState, float opacity, float tickDelta, WorldView world, float radius, E entity) {
//        if (entity instanceof PlayerEntity player) {
//            if (MineplayUtils.hidePlayer(player) && Modules.get().get(MineplayRemoveOfflineRobloxPlayers.class).hidePlayerShadow.get()) {
//                return;
//            }
//        }
//        renderShadow(matrices, vertexConsumers, renderState, opacity, tickDelta, world, radius);
//    }
//}
