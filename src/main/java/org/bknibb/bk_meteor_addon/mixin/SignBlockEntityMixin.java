package org.bknibb.bk_meteor_addon.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import org.bknibb.bk_meteor_addon.modules.BadWordFinder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntity {

    @Shadow
    protected abstract SignText loadLines(SignText signText);

    private SignBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

//    @Redirect(method = "readNbt", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/SignBlockEntity;parseLines(Lnet/minecraft/block/entity/SignText;)Lnet/minecraft/block/entity/SignText;"))
//    private SignText onFrontWordsReadParse(SignBlockEntity instance, SignText signText) {
//        if (Modules.get().get(BadWordFinder.class).isActive()) {
//            Text[] texts = signText.getMessages(false);
//            //frontBadWords = BadWordFinder.badWordCheck(texts, instance.getPos());
//            BadWordFinder.BadWordCheck(texts, instance.getPos(), false);
//        }
//        return parseLines(signText);
//    }

    @Redirect(method="loadAdditional", at = @At(value = "INVOKE", target = "Ljava/util/Optional;map(Ljava/util/function/Function;)Ljava/util/Optional;", ordinal = 0))
    private java.util.Optional<SignText> onFrontWordsReadParse(java.util.Optional<SignText> instance, java.util.function.Function<? super SignText, ? extends SignText> mapper) {
        if (Modules.get().get(BadWordFinder.class).isActive()) {
            Component[] texts = instance.orElseGet(SignText::new).getMessages(false);
            //frontBadWords = BadWordFinder.badWordCheck(texts, instance.getPos());
            BadWordFinder.BadWordCheck(texts, this.getBlockPos(), false);
        }
        return instance.map(mapper);
    }

//    @Redirect(method = "method_49851", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/SignBlockEntity;parseLines(Lnet/minecraft/block/entity/SignText;)Lnet/minecraft/block/entity/SignText;"))
//    private SignText onBackWordsReadParse(SignBlockEntity instance, SignText signText) {
//        if (Modules.get().get(BadWordFinder.class).isActive()) {
//            Text[] texts = signText.getMessages(false);
//            //backBadWords = BadWordFinder.badWordCheck(texts, instance.getPos());
//            BadWordFinder.BadWordCheck(texts, instance.getPos(), true);
//        }
//        return parseLines(signText);
//    }

    @Redirect(method="loadAdditional", at = @At(value = "INVOKE", target = "Ljava/util/Optional;map(Ljava/util/function/Function;)Ljava/util/Optional;", ordinal = 1))
    private java.util.Optional<SignText> onBackWordsReadParse(java.util.Optional<SignText> instance, java.util.function.Function<? super SignText, ? extends SignText> mapper) {
        if (Modules.get().get(BadWordFinder.class).isActive()) {
            Component[] texts = instance.orElseGet(SignText::new).getMessages(false);
            //frontBadWords = BadWordFinder.badWordCheck(texts, instance.getPos());
            BadWordFinder.BadWordCheck(texts, this.getBlockPos(), true);
        }
        return instance.map(mapper);
    }

    @Inject(method = "setFrontText", at = @At("RETURN"))
    private void onSetFrontText(SignText signText, CallbackInfoReturnable<Boolean> cir) {
        if (Modules.get().get(BadWordFinder.class).isActive()) {
            Component[] texts = signText.getMessages(false);
            //frontBadWords = BadWordFinder.badWordCheck(texts, instance.getPos());
            BadWordFinder.BadWordCheck(texts, getBlockPos(), false);
        }
    }

    @Inject(method = "setBackText", at = @At("RETURN"))
    private void onSetBackText(SignText signText, CallbackInfoReturnable<Boolean> cir) {
        if (Modules.get().get(BadWordFinder.class).isActive()) {
            Component[] texts = signText.getMessages(false);
            //backBadWords = BadWordFinder.badWordCheck(texts, instance.getPos());
            BadWordFinder.BadWordCheck(texts, getBlockPos(), true);
        }
    }


}
