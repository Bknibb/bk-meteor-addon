package org.bknibb.bk_meteor_addon.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import org.bknibb.bk_meteor_addon.modules.VanishDetect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onPlayerList", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onPlayerList(PlayerListS2CPacket packet, CallbackInfo ci, Iterator var2, PlayerListS2CPacket.Entry entry, PlayerListEntry playerListEntry) {
        if (Modules.get().isActive(VanishDetect.class)) {
            VanishDetect vanishDetect = Modules.get().get(VanishDetect.class);
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.isInSingleplayer()) return;
            if (mc.getCurrentServerEntry() == null) return;
            if (mc.getCurrentServerEntry().isLocal()) return;
            if (mc.getNetworkHandler() == null) return;
            if (vanishDetect.infoUpdatesEnabled.get() && vanishDetect.InfoUpdatesAllowed()) {
                vanishDetect.infoUpdatesVanished(entry);
            }
        }
    }
}
