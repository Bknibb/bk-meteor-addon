package org.bknibb.bk_meteor_addon.modules;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.bknibb.bk_meteor_addon.BkMeteorAddon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NetworkLoginLogoutNotifier extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");
    private final SettingGroup sgServerWhitelist = settings.createGroup("Server Whitelist");

    private final Setting<Integer> scanInterval = sgGeneral.add(new IntSetting.Builder()
        .name("scan-interval")
        .description("How long to wait in ticks between checking player completions.")
        .range(20, 200)
        .sliderRange(20, 200)
        .defaultValue(100)
        .build()
    );

    public final Setting<Boolean> simpleNotifications = sgGeneral.add(new BoolSetting.Builder()
        .name("simple-notifications")
        .description("Display join/leave notifications without a prefix, to reduce chat clutter.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Ignores any join/leave messages from yourself.")
        .defaultValue(true)
        .build()
    );

//    will add if requested
//    private final Setting<Boolean> ignoreInServer = sgGeneral.add(new BoolSetting.Builder()
//        .name("ignore-in-server")
//        .description("Ignores any join/leave messages from any players in your current server.")
//        .defaultValue(true)
//        .build()
//    );

    private final Setting<ListMode> listMode = sgWhitelist.add(new EnumSetting.Builder<ListMode>()
        .name("list-mode")
        .description("Selection mode.")
        .defaultValue(ListMode.Whitelist)
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
    private static final Random RANDOM = new Random();
    private Integer waitingPacket = null;
    private boolean firstRefresh = true;
    public List<String> onlinePlayers = new ArrayList<>();

    public NetworkLoginLogoutNotifier() {
        super(BkMeteorAddon.CATEGORY, "network-login-logout-notifier", "Notifies you when a player logs in or out of the network (for mineplay, also may work on other server networks).");
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
        if (waitingPacket != null) return;
        if (mc.isInSingleplayer()) return;
        if (mc.getCurrentServerEntry() == null) return;
        if (mc.getCurrentServerEntry().isLocal()) return;
        if (!ServerAllowed()) return;
        if (mc.getNetworkHandler() == null) return;
        timer++;
        if (timer > scanInterval.get()) {
            timer = 0;
            waitingPacket = RANDOM.nextInt(200);
            mc.getNetworkHandler().sendPacket(new RequestCommandCompletionsC2SPacket(waitingPacket, "/msg "));
        }
    }

    @EventHandler
    private void onGameLeave(GameLeftEvent event) {
        waitingPacket = null;
        onlinePlayers.clear();
        firstRefresh = true;
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (waitingPacket == null) return;
        if (event.packet instanceof CommandSuggestionsS2CPacket packet) {
            if (packet.id() != waitingPacket) return;
            waitingPacket = null;
            Suggestions suggestions = packet.getSuggestions();
            if (suggestions.isEmpty()) return;
            if (mc.player == null) return;
            List<String> prevOnlinePlayers = onlinePlayers;
            onlinePlayers = new ArrayList<>();
            for (Suggestion suggestion : suggestions.getList()) {
                String name = suggestion.getText();
                if (ignoreSelf.get() && name.equals(mc.player.getName().getString())) continue;
                //if (ignoreInServer.gte() && mc.getNetworkHandler().getPlayerListEntry(name) != null) return;
                if (listMode.get() == ListMode.Blacklist) {
                    if (blacklist.get().contains(name)) {
                        continue;
                    }
                } else {
                    if (!(whitelist.get().contains(name) || (includeFriends.get() && Friends.get().get(name) != null))) {
                        continue;
                    }
                }
                onlinePlayers.add(name);
                if (!prevOnlinePlayers.contains(name) && !firstRefresh) {
                    showJoinNotification(name);
                }
            }
            firstRefresh = false;
            for (String name : prevOnlinePlayers) {
                if (ignoreSelf.get() && name.equals(mc.player.getName().getString())) continue;
                //if (ignoreInServer.gte() && mc.getNetworkHandler().getPlayerListEntry(name) != null) return;
                if (listMode.get() == ListMode.Blacklist) {
                    if (blacklist.get().contains(name)) {
                        continue;
                    }
                } else {
                    if (!(whitelist.get().contains(name) || (includeFriends.get() && Friends.get().get(name) != null))) {
                        continue;
                    }
                }
                if (!onlinePlayers.contains(name)) {
                    showLeaveNotification(name);
                }
            }
        }
    }

    private void showJoinNotification(String name) {
        if (simpleNotifications.get()) {
            if (mc.player == null) return;
            mc.player.sendMessage(Text.literal(
                Formatting.GRAY + "["
                    + Formatting.LIGHT_PURPLE + "Network"
                    + Formatting.GRAY + "] "
                    + Formatting.GRAY + "["
                    + Formatting.GREEN + "+"
                    + Formatting.GRAY + "] "
                    + Formatting.RESET + name
            ), false);
        } else {
            ChatUtils.sendMsg(Text.literal(
                    name
                    + Formatting.GREEN + " joined "
                    + Formatting.RESET + " the network."
            ));
        }
    }

    private void showLeaveNotification(String name) {
        if (simpleNotifications.get()) {
            if (mc.player == null) return;
            mc.player.sendMessage(Text.literal(
                Formatting.GRAY + "["
                    + Formatting.LIGHT_PURPLE + "Network"
                    + Formatting.GRAY + "] "
                    + Formatting.GRAY + "["
                    + Formatting.RED + "-"
                    + Formatting.GRAY + "] "
                    + Formatting.RESET + name
            ), false);
        } else {
            ChatUtils.sendMsg(Text.literal(
                name
                    + Formatting.RED + " left "
                    + Formatting.RESET + " the network."
            ));
        }
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }
}
