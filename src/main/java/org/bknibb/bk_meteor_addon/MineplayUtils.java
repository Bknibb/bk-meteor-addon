package org.bknibb.bk_meteor_addon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

public class MineplayUtils {
    public static boolean isRobloxPlayer(PlayerInfo entry) {
        if (entry == null || entry.getTabListDisplayName() == null || entry.getTabListDisplayName().getStyle().getColor() == null) {
            return false;
        }
        return entry.getTabListDisplayName().getStyle().getColor().getValue() == 0xFF999B;
    }
    public static boolean isOnMineplay() {
        Minecraft client = Minecraft.getInstance();
        if (client.getCurrentServer() == null) {
            return false;
        }
        String serverName = client.getCurrentServer().ip;
        return serverName.contains("mineplay.nl");
    }
//    public static boolean isDisconnectedPlayer(PlayerEntity player) {
//        MinecraftClient client = MinecraftClient.getInstance();
//        if (client.getNetworkHandler() == null) return false;
//        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
//        if (entry == null) {
//            return false;
//        }
//        if (!isRobloxPlayer(entry)) return false;
//        return entry.getLatency() == 0;
//    }
//    public static boolean isDisconnectedPlayer(String player) {
//        MinecraftClient client = MinecraftClient.getInstance();
//        if (client.getNetworkHandler() == null) return false;
//        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(player);
//        if (entry == null) {
//            return false;
//        }
//        if (!isRobloxPlayer(entry)) return false;
//        return entry.getLatency() == 0;
//    }
//    public static boolean isDisconnectedPlayer(PlayerListEntry entry) {
//        if (!isRobloxPlayer(entry)) return false;
//        return entry.getLatency() == 0;
//    }
//    public static boolean canHide() {
//        return (isOnMineplay() && Modules.get().isActive(MineplayRemoveOfflineRobloxPlayers.class));
//    }
//    public static boolean hidePlayer(PlayerEntity player) {
//        return (canHide() && player != MinecraftClient.getInstance().player && isDisconnectedPlayer(player));
//    }
//    public static boolean hidePlayer(PlayerListEntry entry) {
//        PlayerEntity clientPlayer = MinecraftClient.getInstance().player;
//        if (clientPlayer == null) return false;
//        return (canHide() && entry.getProfile().getId() != clientPlayer.getUuid() && isDisconnectedPlayer(entry));
//    }
//    public static boolean hidePlayer(String player) {
//        PlayerEntity clientPlayer = MinecraftClient.getInstance().player;
//        if (clientPlayer == null) return false;
//        return (canHide() && !player.equals(clientPlayer.getName().getString()) && isDisconnectedPlayer(player));
//    }
}
