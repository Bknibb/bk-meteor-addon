package org.bknibb.bk_meteor_addon.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import org.bknibb.bk_meteor_addon.MineplayUtils;
import org.bknibb.bk_meteor_addon.modules.MineplayRemoveOfflineRobloxPlayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

//@Mixin(PlayerListHud.class)
//public class PlayerListHudMixin {
//    @ModifyReturnValue(method = "collectPlayerEntries", at = @At("RETURN"))
//    private List<PlayerListEntry> onCollectPlayerEntries(List<PlayerListEntry> original) {
//        if (!MineplayUtils.canHide()) return original;
//        List<PlayerListEntry> filtered = new ArrayList<>(original);
//        filtered.removeIf(entry -> MineplayUtils.hidePlayer(entry) && Modules.get().get(MineplayRemoveOfflineRobloxPlayers.class).hidePlayerListEntry.get());
//        return filtered;
//    }
//}
