package org.bknibb.bk_meteor_addon.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.bknibb.bk_meteor_addon.MineplayUtils;
import org.bknibb.bk_meteor_addon.modules.MineplayRemoveOfflineRobloxPlayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//@Mixin(LivingEntityRenderer.class)
//public class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
//    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
//    private void onRender(S state, net.minecraft.client.util.math.MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
//        if (state instanceof PlayerEntityRenderState playerState) {
//            if (MineplayUtils.hidePlayer(playerState.name) && Modules.get().get(MineplayRemoveOfflineRobloxPlayers.class).hidePlayerEntity.get()) {
//                ci.cancel();
//            }
//        }
//    }
//}
