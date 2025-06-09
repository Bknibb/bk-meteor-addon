package org.bknibb.bk_meteor_addon.modules;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.ArrayListDeque;
import org.bknibb.bk_meteor_addon.BkMeteorAddon;
import org.bknibb.bk_meteor_addon.MineplayUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerLoginLogoutNotifier extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");
    private final SettingGroup sgServerWhitelist = settings.createGroup("Server Whitelist");

    private final Setting<JoinLeaveModes> joinsLeavesMode = sgGeneral.add(new EnumSetting.Builder<JoinLeaveModes>()
        .name("player-joins-leaves")
        .description("How to handle player join/leave notifications.")
        .defaultValue(JoinLeaveModes.Both)
        .build()
    );

    private final Setting<Integer> notificationDelay = sgGeneral.add(new IntSetting.Builder()
        .name("notification-delay")
        .description("How long to wait in ticks before posting the next join/leave notification in your chat.")
        .range(0, 1000)
        .sliderRange(0, 100)
        .defaultValue(0)
        .build()
    );

    private final Setting<Integer> joinDataDelay = sgGeneral.add(new IntSetting.Builder()
        .name("join-data-delay")
        .description("How long to wait in ticks before posting the next join/leave notification in your chat.")
        .range(0, 10000)
        .sliderRange(0, 10000)
        .defaultValue(100)
        .build()
    );

    private final Setting<Boolean> customMineplayNotifications = sgGeneral.add(new BoolSetting.Builder()
        .name("custom-mineplay-notifications")
        .description("Display join/leave notifications similar to the normal ones normally on mineplay (for mineplay).")
        .defaultValue(true)
        .build()
    );

    private final Setting<MineplayPlatformType> mineplayPlatformFilter = sgGeneral.add(new EnumSetting.Builder<MineplayPlatformType>()
        .name("mineplay-platform-filter")
        .description("Mineplay Platform Filter.")
        .defaultValue(MineplayPlatformType.BOTH)
        .build()
    );

    private final Setting<Boolean> simpleNotifications = sgGeneral.add(new BoolSetting.Builder()
        .name("simple-notifications")
        .description("Display join/leave notifications without a prefix, to reduce chat clutter.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Ignores any join/leave messages from yourself (usually caused by vanish).")
        .defaultValue(true)
        .build()
    );

    private final Setting<ListMode> listMode = sgWhitelist.add(new EnumSetting.Builder<ListMode>()
        .name("list-mode")
        .description("Selection mode.")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    private final Setting<List<String>> blacklist = sgWhitelist.add(new StringListSetting.Builder()
        .name("blacklist")
        .description("The players you don't want to see.")
        .visible(() -> listMode.get() == ListMode.Blacklist)
        .build()
    );

    private final Setting<Boolean> includeFriends = sgWhitelist.add(new BoolSetting.Builder()
        .name("include-friends")
        .description("Include meteor friends in the whitelist.")
        .defaultValue(true)
        .visible(() -> listMode.get() == ListMode.Whitelist)
        .build()
    );

    private final Setting<List<String>> whitelist = sgWhitelist.add(new StringListSetting.Builder()
        .name("whitelist")
        .description("The players you want to see.")
        .visible(() -> listMode.get() == ListMode.Whitelist)
        .build()
    );

    private final Setting<ListMode> serverListMode = sgServerWhitelist.add(new EnumSetting.Builder<ListMode>()
        .name("list-mode")
        .description("Selection mode.")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    private final Setting<List<String>> serverBlacklist = sgServerWhitelist.add(new StringListSetting.Builder()
        .name("blacklist")
        .description("The servers you don't want this to work on.")
        .visible(() -> serverListMode.get() == ListMode.Blacklist)
        .build()
    );

    private final Setting<List<String>> serverWhitelist = sgServerWhitelist.add(new StringListSetting.Builder()
        .name("whitelist")
        .description("The players you want this to work on.")
        .visible(() -> serverListMode.get() == ListMode.Whitelist)
        .build()
    );

    private int timer;
    //private boolean loginPacket = true;
    private final ArrayListDeque<Text> messageQueue = new ArrayListDeque<>();
    private List<String> onlineRobloxPlayers = new ArrayList<>();
    private final ArrayListDeque<Pair<Instant, Runnable>> taskQueue = new ArrayListDeque<>();

    public PlayerLoginLogoutNotifier() {
        super(BkMeteorAddon.CATEGORY, "player-login-logout-notifier", "Notifies you when a player logs in or out.");
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WHorizontalList list = theme.horizontalList();
        list.add(theme.button("Copy List Settings")).widget().action = () -> {
            NbtCompound tag = new NbtCompound();
            tag.put("listMode", listMode.toTag());
            tag.put("blacklist", blacklist.toTag());
            tag.put("includeFriends", includeFriends.toTag());
            tag.put("whitelist", whitelist.toTag());
            NbtUtils.toClipboard(tag);
        };
        list.add(theme.button("Paste List Settings")).widget().action = () -> {
            NbtCompound tag = NbtUtils.fromClipboard();
            if (tag == null) return;
            if (tag.contains("listMode")) {
                listMode.fromTag(tag.getCompound("listMode").get());
            }
            if (tag.contains("blacklist")) {
                blacklist.fromTag(tag.getCompound("blacklist").get());
            }
            if (tag.contains("includeFriends")) {
                includeFriends.fromTag(tag.getCompound("includeFriends").get());
            }
            if (tag.contains("whitelist")) {
                whitelist.fromTag(tag.getCompound("whitelist").get());
            }
        };
        list.add(theme.button("Copy Server List Settings")).widget().action = () -> {
            NbtCompound tag = new NbtCompound();
            tag.put("serverListMode", serverListMode.toTag());
            tag.put("serverBlacklist", serverBlacklist.toTag());
            tag.put("serverWhitelist", serverWhitelist.toTag());
            NbtUtils.toClipboard(tag);
        };
        list.add(theme.button("Paste Server List Settings")).widget().action = () -> {
            NbtCompound tag = NbtUtils.fromClipboard();
            if (tag == null) return;
            if (tag.contains("serverListMode")) {
                serverListMode.fromTag(tag.getCompound("serverListMode"));
            }
            if (tag.contains("serverBlacklist")) {
                serverBlacklist.fromTag(tag.getCompound("serverBlacklist"));
            }
            if (tag.contains("serverWhitelist")) {
                serverWhitelist.fromTag(tag.getCompound("serverWhitelist"));
            }
        };
        return list;
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        messageQueue.clear();
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        timer = 0;
        messageQueue.clear();
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!ServerAllowed()) return;
        switch (event.packet) {
            case PlayerListS2CPacket packet when joinsLeavesMode.get().equals(JoinLeaveModes.Both) || joinsLeavesMode.get().equals(JoinLeaveModes.Joins) -> {

                if (packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
                    createJoinNotifications(packet);
                }
            }
            case PlayerRemoveS2CPacket packet when joinsLeavesMode.get().equals(JoinLeaveModes.Both) || joinsLeavesMode.get().equals(JoinLeaveModes.Leaves) ->
                createLeaveNotification(packet);
            default -> {}
        }
    }

    private boolean ServerAllowed() {
        if (mc.getCurrentServerEntry() == null) {
            return false;
        }
        if (serverListMode.get() == ListMode.Blacklist) {
            return !serverBlacklist.get().contains(mc.getCurrentServerEntry().address);
        } else {
            return serverWhitelist.get().contains(mc.getCurrentServerEntry().address);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        timer++;
        while (timer >= notificationDelay.get() && !messageQueue.isEmpty()) {
            timer = 0;
            if (simpleNotifications.get()) {
                if (mc.player == null) continue;
                mc.player.sendMessage(messageQueue.removeFirst(), false);
            } else {
                ChatUtils.sendMsg(messageQueue.removeFirst());
            }
        }
        while (!taskQueue.isEmpty()) {
            if (Instant.now().isAfter(taskQueue.getFirst().getLeft().plusMillis(joinDataDelay.get()))) {
                break;
            }
            taskQueue.removeFirst().getRight().run();
        }
        if (Modules.get().isActive(MineplayRemoveOfflineRobloxPlayers.class) && Modules.get().get(MineplayRemoveOfflineRobloxPlayers.class).hidePlayerLoginLogoutMessages.get() && mc.getNetworkHandler() != null && ServerAllowed()) {
            List<String> prevPlayers = onlineRobloxPlayers;
            onlineRobloxPlayers = new ArrayList<>();
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                if (listMode.get() == ListMode.Blacklist) {
                    if (blacklist.get().contains(entry.getProfile().getName())) {
                        continue;
                    }
                } else {
                    if (!(whitelist.get().contains(entry.getProfile().getName()) || (includeFriends.get() && Friends.get().get(entry.getProfile().getName()) != null))) {
                        continue;
                    }
                }
                if (!MineplayUtils.isDisconnectedPlayer(entry)) {
                    onlineRobloxPlayers.add(entry.getProfile().getName());
                }
            }
            for (String player : prevPlayers) {
                if (listMode.get() == ListMode.Blacklist) {
                    if (blacklist.get().contains(player)) {
                        continue;
                    }
                } else {
                    if (!(whitelist.get().contains(player) || (includeFriends.get() && Friends.get().get(player) != null))) {
                        continue;
                    }
                }
                PlayerListEntry toRemove = mc.getNetworkHandler().getPlayerListEntry(player);
                if (toRemove == null) continue;
                if (MineplayUtils.isOnMineplay() && MineplayUtils.isRobloxPlayer(toRemove) && mineplayPlatformFilter.get() == MineplayPlatformType.MINECRAFT) continue;
                if (MineplayUtils.isOnMineplay() && !MineplayUtils.isRobloxPlayer(toRemove) && mineplayPlatformFilter.get() == MineplayPlatformType.ROBLOX) continue;
                if (!onlineRobloxPlayers.contains(player)) {
                    doCreateLeaveNotification(toRemove);
                }
            }
        }
    }

    private void createJoinNotifications(PlayerListS2CPacket packet) {
        if (mc.getNetworkHandler() == null || mc.player == null) return;
        for (PlayerListS2CPacket.Entry packetEntry : packet.getPlayerAdditionEntries()) {
            if (packetEntry.profile() == null) continue;
            taskQueue.addLast(new Pair<>(Instant.now(), () -> {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(packetEntry.profile().getId());
                if (entry == null) return;
                if (ignoreSelf.get() && entry.getProfile().getId().equals(mc.player.getUuid())) return;
                if (MineplayUtils.isOnMineplay() && MineplayUtils.isRobloxPlayer(entry) && mineplayPlatformFilter.get() == MineplayPlatformType.MINECRAFT)
                    return;
                if (MineplayUtils.isOnMineplay() && !MineplayUtils.isRobloxPlayer(entry) && mineplayPlatformFilter.get() == MineplayPlatformType.ROBLOX)
                    return;
                if (MineplayUtils.isDisconnectedPlayer(entry) && Modules.get().isActive(MineplayRemoveOfflineRobloxPlayers.class) && Modules.get().get(MineplayRemoveOfflineRobloxPlayers.class).hidePlayerLoginLogoutMessages.get())
                    return;
                if (listMode.get() == ListMode.Blacklist) {
                    if (blacklist.get().contains(entry.getProfile().getName())) {
                        return;
                    }
                } else {
                    if (!(whitelist.get().contains(entry.getProfile().getName()) || (includeFriends.get() && Friends.get().get(entry.getProfile().getName()) != null))) {
                        return;
                    }
                }

                doCreateJoinNotification(entry);
            }));
        }
    }

    private void createLeaveNotification(PlayerRemoveS2CPacket packet) {
        if (mc.getNetworkHandler() == null || mc.player == null) return;

        for (UUID id : packet.profileIds()) {
            PlayerListEntry toRemove = mc.getNetworkHandler().getPlayerListEntry(id);
            if (toRemove == null) continue;
            if (ignoreSelf.get() && toRemove.getProfile().getId().equals(mc.player.getUuid())) continue;
            if (MineplayUtils.isOnMineplay() && MineplayUtils.isRobloxPlayer(toRemove) && mineplayPlatformFilter.get() == MineplayPlatformType.MINECRAFT) continue;
            if (MineplayUtils.isOnMineplay() && !MineplayUtils.isRobloxPlayer(toRemove) && mineplayPlatformFilter.get() == MineplayPlatformType.ROBLOX) continue;
            if (MineplayUtils.isDisconnectedPlayer(toRemove) && Modules.get().isActive(MineplayRemoveOfflineRobloxPlayers.class) && Modules.get().get(MineplayRemoveOfflineRobloxPlayers.class).hidePlayerLoginLogoutMessages.get()) return;
            if (listMode.get() == ListMode.Blacklist) {
                if (blacklist.get().contains(toRemove.getProfile().getName())) {
                    continue;
                }
            } else {
                if (!(whitelist.get().contains(toRemove.getProfile().getName()) || (includeFriends.get() && Friends.get().get(toRemove.getProfile().getName()) != null))) {
                    continue;
                }
            }

            doCreateLeaveNotification(toRemove);
        }
    }

    private void doCreateJoinNotification(PlayerListEntry entry) {
        if (customMineplayNotifications.get() && MineplayUtils.isOnMineplay()) {
            if (MineplayUtils.isRobloxPlayer(entry)) {
                messageQueue.addLast(Text.literal(
                    Formatting.GRAY + "["
                        + Formatting.GREEN + "+"
                        + Formatting.GRAY + "] ").append(
                    Text.literal(entry.getProfile().getName()).setStyle(Style.EMPTY.withColor(0xFF999B))).append(
                    Text.literal(Formatting.RESET + " joined the server on ")).append(
                    Text.literal("Roblox").setStyle(Style.EMPTY.withColor(0xFF999B)))
                );
            } else {
                messageQueue.addLast(Text.literal(
                    Formatting.GRAY + "["
                        + Formatting.GREEN + "+"
                        + Formatting.GRAY + "] "
                        + Formatting.GREEN + entry.getProfile().getName()
                        + Formatting.RESET + " joined the server on "
                        + Formatting.GREEN + "Minecraft"
                ));
            }
        } else if (simpleNotifications.get()) {
            messageQueue.addLast(Text.literal(
                Formatting.GRAY + "["
                    + Formatting.GREEN + "+"
                    + Formatting.GRAY + "] "
                    + entry.getProfile().getName()
            ));
        } else {
            messageQueue.addLast(Text.literal(
                Formatting.WHITE
                    + entry.getProfile().getName()
                    + Formatting.GRAY + " joined."
            ));
        }
    }

    private void doCreateLeaveNotification(PlayerListEntry entry) {
        if (customMineplayNotifications.get() && MineplayUtils.isOnMineplay()) {
            if (MineplayUtils.isRobloxPlayer(entry)) {
                messageQueue.addLast(Text.literal(
                    Formatting.GRAY + "["
                        + Formatting.RED + "-"
                        + Formatting.GRAY + "] ").append(
                        Text.literal(entry.getProfile().getName()).setStyle(Style.EMPTY.withColor(0xFF999B))).append(
                        Text.literal(Formatting.RESET + " left the server on ")).append(
                        Text.literal("Roblox").setStyle(Style.EMPTY.withColor(0xFF999B)))
                );
            } else {
                messageQueue.addLast(Text.literal(
                    Formatting.GRAY + "["
                        + Formatting.RED + "-"
                        + Formatting.GRAY + "] "
                        + Formatting.GREEN + entry.getProfile().getName()
                        + Formatting.RESET + " left the server on "
                        + Formatting.GREEN + "Minecraft"
                ));
            }
        } else if (simpleNotifications.get()) {
            messageQueue.addLast(Text.literal(
                Formatting.GRAY + "["
                    + Formatting.RED + "-"
                    + Formatting.GRAY + "] "
                    + entry.getProfile().getName()
            ));
        } else {
            messageQueue.addLast(Text.literal(
                Formatting.WHITE
                    + entry.getProfile().getName()
                    + Formatting.GRAY + " left."
            ));
        }
    }

    public enum JoinLeaveModes {
        Joins, Leaves, Both
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }

    public enum MineplayPlatformType {
        BOTH,
        ROBLOX,
        MINECRAFT
    }
}
