package org.bknibb.bk_meteor_addon.modules;

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
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.bknibb.bk_meteor_addon.BkMeteorAddon;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VivecraftVanishDetect extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");
    private final SettingGroup sgServerWhitelist = settings.createGroup("Server Whitelist");

    private final Setting<Integer> scanInterval = sgGeneral.add(new IntSetting.Builder()
        .name("scan-interval")
        .description("How long to wait in ticks between checking player completions.")
        .range(20, 600)
        .sliderRange(20, 600)
        .defaultValue(100)
        .build()
    );

    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Ignores any join/leave messages from yourself.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> leaveNotification = sgGeneral.add(new BoolSetting.Builder()
        .name("leave-notification")
        .description("Show a notification when a vanished player leaves.")
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
    private Integer waitingPacket = 0;
    public List<String> vanishedPlayers = new ArrayList<>();
    private final List<String> tempVrPlayers = new ArrayList<>();

    public VivecraftVanishDetect() {
        super(BkMeteorAddon.CATEGORY, "vivecraft-vanish-detect", "Detects if a player is in vanish mode using /vr list (the server and the player vanishing must have vivecraft).");
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

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (waitingPacket != 0) return;
        if (mc.isInSingleplayer()) return;
        if (mc.getCurrentServerEntry() == null) return;
        if (mc.getCurrentServerEntry().isLocal()) return;
        if (mc.getNetworkHandler() == null) return;
        if (!ServerAllowed()) return;
        timer++;
        if (timer > scanInterval.get()) {
            timer = 0;
            waitingPacket = 1;
            mc.getNetworkHandler().sendChatCommand("vr list");
        }
    }

    @EventHandler
    private void onGameLeave(GameLeftEvent event) {
        waitingPacket = 0;
        vanishedPlayers.clear();
        tempVrPlayers.clear();
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (waitingPacket == 0) return;
        if (event.packet instanceof GameMessageS2CPacket packet) {
            if (Objects.equals(packet.content().getString(), "There are currently no players with vivecraft installed.")) {
                waitingPacket--;
                vanishedPlayers.clear();
                event.cancel();
                return;
            } else if (packet.content().getString().startsWith("Players on Vivecraft: (")) {
                waitingPacket--;
                Pattern pattern = Pattern.compile("\\((\\d+)\\)");
                Matcher matcher = pattern.matcher(packet.content().getString());
                if (matcher.find()) {
                    waitingPacket = Integer.parseInt(matcher.group(1));
                    tempVrPlayers.clear();
                    event.cancel();
                } else {
                    return;
                }
            } else if (packet.content().getString().startsWith("  - ")) {
                waitingPacket--;
                Pattern pattern = Pattern.compile("-\\s+(\\w+)\\s+\\(");
                Matcher matcher = pattern.matcher(packet.content().getString());

                if (matcher.find()) {
                    {
                        String name = matcher.group(1);
                        tempVrPlayers.add(name);
                    }
                    event.cancel();
                    if (waitingPacket == 0 && mc.player != null && mc.getNetworkHandler() != null) {
                        List<String> prevVanishedPlayers = vanishedPlayers;
                        vanishedPlayers = new ArrayList<>();
                        for (String name : tempVrPlayers) {
                            if (ignoreSelf.get() && name.equals(mc.player.getName().getString())) continue;
                            if (listMode.get() == ListMode.Blacklist) {
                                if (blacklist.get().contains(name)) {
                                    continue;
                                }
                            } else {
                                if (!(whitelist.get().contains(name) || (includeFriends.get() && Friends.get().get(name) != null))) {
                                    continue;
                                }
                            }
                            if (mc.getNetworkHandler().getPlayerListEntry(name) != null) continue;
                            vanishedPlayers.add(name);
                            if (!prevVanishedPlayers.contains(name)) {
                                showVanishedNotification(name);
                            }
                        }
                        tempVrPlayers.clear();
                        for (String name : prevVanishedPlayers) {
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

                            if (!vanishedPlayers.contains(name)) {
                                if (mc.getNetworkHandler().getPlayerListEntry(name) == null) {
                                    if (leaveNotification.get()) {
                                        showLeaveNotification(name);
                                    }
                                } else {
                                    showUnvanishNotification(name);
                                }
                            }
                        }
                    }
                } else {
                    return;
                }
            }
        }
    }

    public void showUnvanishNotification(String name) {
        info(Text.literal(
            Formatting.RESET + name
                + Formatting.GREEN + " unvanished"
        ));
    }

    public void showVanishedNotification(String name) {
        info(Text.literal(
                Formatting.RESET + name
                    + Formatting.LIGHT_PURPLE + " vanished"
        ));
    }

    public void showLeaveNotification(String name) {
        info(Text.literal(
                Formatting.LIGHT_PURPLE + "vanished"
                + Formatting.RESET + " player " + name
                + Formatting.RED + " left"
        ));
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }
}
