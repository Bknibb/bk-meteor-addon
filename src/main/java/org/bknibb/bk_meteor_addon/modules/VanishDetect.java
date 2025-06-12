package org.bknibb.bk_meteor_addon.modules;

import com.mojang.authlib.GameProfile;
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
import meteordevelopment.meteorclient.utils.misc.Version;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.bknibb.bk_meteor_addon.BkMeteorAddon;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VanishDetect extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgVivecraft = settings.createGroup("Vivecraft");
    private final SettingGroup sgInfoUpdates = settings.createGroup("Info Updates");
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");
    private final SettingGroup sgVivecraftServerWhitelist = settings.createGroup("Vivecraft Server Whitelist");
    private final SettingGroup sgInfoUpdatesServerWhitelist = settings.createGroup("Info Updates Server Whitelist");

    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Ignores yourself vanishing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> leaveNotification = sgGeneral.add(new BoolSetting.Builder()
        .name("leave-notification")
        .description("Show a notification when a vanished player leaves.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> vivecraftEnabled = sgVivecraft.add(new BoolSetting.Builder()
        .name("vivecraft-enabled")
        .description("Detects if a player is in vanish mode using /vr list (the server and the player vanishing must have vivecraft).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> scanInterval = sgVivecraft.add(new IntSetting.Builder()
        .name("scan-interval")
        .description("How long to wait in ticks between checking player completions.")
        .range(20, 600)
        .sliderRange(20, 600)
        .defaultValue(100)
        .build()
    );

    public final Setting<Boolean> infoUpdatesEnabled = sgInfoUpdates.add(new BoolSetting.Builder()
        .name("info-updates-enabled")
        .description("Detects if a player is in vanish mode using player info updates (the server needs to provide them).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> cacheThreshold = sgInfoUpdates.add(new DoubleSetting.Builder()
        .name("cache-threshold")
        .description("How long to wait in seconds without recieving a player info update before showing as left.")
        .range(5, 30)
        .sliderRange(5, 30)
        .defaultValue(30)
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

    private final Setting<ListMode> vivecraftServerListMode = sgVivecraftServerWhitelist.add(new EnumSetting.Builder<ListMode>()
        .name("list-mode")
        .description("Selection mode.")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    private final Setting<List<String>> vivecraftServerBlacklist = sgVivecraftServerWhitelist.add(new StringListSetting.Builder()
        .name("blacklist")
        .description("The servers you don't want this to work on.")
        .visible(() -> vivecraftServerListMode.get() == ListMode.Blacklist)
        .build()
    );

    private final Setting<List<String>> vivecraftServerWhitelist = sgVivecraftServerWhitelist.add(new StringListSetting.Builder()
        .name("whitelist")
        .description("The players you want this to work on.")
        .visible(() -> vivecraftServerListMode.get() == ListMode.Whitelist)
        .build()
    );

    private final Setting<ListMode> infoUpdatesServerListMode = sgInfoUpdatesServerWhitelist.add(new EnumSetting.Builder<ListMode>()
        .name("list-mode")
        .description("Selection mode.")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    private final Setting<List<String>> infoUpdatesServerBlacklist = sgInfoUpdatesServerWhitelist.add(new StringListSetting.Builder()
        .name("blacklist")
        .description("The servers you don't want this to work on.")
        .visible(() -> infoUpdatesServerListMode.get() == ListMode.Blacklist)
        .build()
    );

    private final Setting<List<String>> infoUpdatesServerWhitelist = sgInfoUpdatesServerWhitelist.add(new StringListSetting.Builder()
        .name("whitelist")
        .description("The players you want this to work on.")
        .visible(() -> infoUpdatesServerListMode.get() == ListMode.Whitelist)
        .build()
    );

    private int timer;
    private Integer waitingPacket = 0;
    public List<String> vanishedPlayers = new ArrayList<>();
    private final List<String> tempVrPlayers = new ArrayList<>();
    private final Map<String, Instant> infoUpdatesCacheTime = new HashMap<>();
    private final Map<UUID, GameProfile> profileCache = new HashMap<>();

    public VanishDetect() {
        super(BkMeteorAddon.CATEGORY, "vanish-detect", "Uses multiple methods to detect if a player is in vanish mode. This includes /vr list, which requires vivecraft, and the server's player info updates.");
    }

    private boolean VivecraftServerAllowed() {
        if (mc.getCurrentServerEntry() == null) {
            return false;
        }
        if (vivecraftServerListMode.get() == ListMode.Blacklist) {
            return !vivecraftServerBlacklist.get().contains(mc.getCurrentServerEntry().address);
        } else {
            return vivecraftServerWhitelist.get().contains(mc.getCurrentServerEntry().address);
        }
    }

    public boolean InfoUpdatesAllowed() {
        if (mc.getCurrentServerEntry() == null) {
            return false;
        }
        if (infoUpdatesServerListMode.get() == ListMode.Blacklist) {
            return !infoUpdatesServerBlacklist.get().contains(mc.getCurrentServerEntry().address);
        } else {
            return infoUpdatesServerWhitelist.get().contains(mc.getCurrentServerEntry().address);
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
        list.add(theme.button("Copy Vivecraft Server List Settings")).widget().action = () -> {
            NbtCompound tag = new NbtCompound();
            tag.put("serverListMode", vivecraftServerListMode.toTag());
            tag.put("serverBlacklist", vivecraftServerBlacklist.toTag());
            tag.put("serverWhitelist", vivecraftServerWhitelist.toTag());
            NbtUtils.toClipboard(tag);
        };
        list.add(theme.button("Paste Vivecraft Server List Settings")).widget().action = () -> {
            NbtCompound tag = NbtUtils.fromClipboard();
            if (tag == null) return;
            if (tag.contains("serverListMode")) {
                vivecraftServerListMode.fromTag(tag.getCompound("serverListMode").get());
            }
            if (tag.contains("serverBlacklist")) {
                vivecraftServerBlacklist.fromTag(tag.getCompound("serverBlacklist").get());
            }
            if (tag.contains("serverWhitelist")) {
                vivecraftServerWhitelist.fromTag(tag.getCompound("serverWhitelist").get());
            }
        };
        list.add(theme.button("Copy Info Updates Server List Settings")).widget().action = () -> {
            NbtCompound tag = new NbtCompound();
            tag.put("serverListMode", infoUpdatesServerListMode.toTag());
            tag.put("serverBlacklist", infoUpdatesServerBlacklist.toTag());
            tag.put("serverWhitelist", infoUpdatesServerWhitelist.toTag());
            NbtUtils.toClipboard(tag);
        };
        list.add(theme.button("Paste Info Updates Server List Settings")).widget().action = () -> {
            NbtCompound tag = NbtUtils.fromClipboard();
            if (tag == null) return;
            if (tag.contains("serverListMode")) {
                infoUpdatesServerListMode.fromTag(tag.getCompound("serverListMode").get());
            }
            if (tag.contains("serverBlacklist")) {
                infoUpdatesServerBlacklist.fromTag(tag.getCompound("serverBlacklist").get());
            }
            if (tag.contains("serverWhitelist")) {
                infoUpdatesServerWhitelist.fromTag(tag.getCompound("serverWhitelist").get());
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
        if (vivecraftEnabled.get() && VivecraftServerAllowed()) {
            timer++;
            if (timer > scanInterval.get()) {
                timer = 0;
                waitingPacket = 1;
                mc.getNetworkHandler().sendChatCommand("vr list");
            }
        }
        if (infoUpdatesEnabled.get() && InfoUpdatesAllowed()) {
            Instant now = Instant.now();
            List<String> unvanished = new ArrayList<>();
            List<String> cacheExpired = new ArrayList<>();
            for (Map.Entry<String, Instant> entry : infoUpdatesCacheTime.entrySet()) {
                if (mc.getNetworkHandler().getPlayerListEntry(entry.getKey()) != null) {
                    unvanished.add(entry.getKey());
                }
                else if (now.isAfter(entry.getValue().plusMillis((long)(cacheThreshold.get()*1000)))) {
                    cacheExpired.add(entry.getKey());
                }
            }
            for (String name : unvanished) {
                vanishedPlayers.remove(name);
                infoUpdatesCacheTime.remove(name);
                showUnvanishNotification(name);
            }
            for (String name : cacheExpired) {
                vanishedPlayers.remove(name);
                infoUpdatesCacheTime.remove(name);
                showLeaveNotification(name);
            }
        }
    }

    @EventHandler
    private void onGameLeave(GameLeftEvent event) {
        waitingPacket = 0;
        vanishedPlayers.clear();
        tempVrPlayers.clear();
        infoUpdatesCacheTime.clear();
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
                            if (infoUpdatesCacheTime.containsKey(name)) {
                                infoUpdatesCacheTime.put(name, Instant.now());
                            }
                        }
                        for (Map.Entry<String, Instant> entry : infoUpdatesCacheTime.entrySet()) {
                            vanishedPlayers.add(entry.getKey());
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
                                infoUpdatesCacheTime.remove(name);
                            }
                        }
                    }
                } else {
                    return;
                }
            }
        }
    }

    public static GameProfile resolveProfile(UUID uuid) {
        String uuidStr = uuid.toString().replace("-", "");
        Http.Request request = Http.get("https://sessionserver.mojang.com/session/minecraft/profile/" + uuidStr);
        HttpResponse<GameProfileJson> res = request.sendJsonResponse(GameProfileJson.class);
        if (res.statusCode() == Http.SUCCESS) {
            GameProfileJson json = res.body();
            return new GameProfile(uuid, json.name);
        }
        return null;
    }

    private static class GameProfileJson {
        public String id;
        public String name;
    }

    public void infoUpdatesVanished(PlayerListS2CPacket.Entry entry) {
        if (entry == null || entry.profileId() == null) return;
        new Thread(() -> {
            if (!profileCache.containsKey(entry.profileId())) {
                GameProfile profile = resolveProfile(entry.profileId());
                if (profile != null) {
                    profileCache.put(entry.profileId(), profile);
                }
            }
            String name = entry.profileId().toString();
            if (profileCache.containsKey(entry.profileId())) {
                name = profileCache.get(entry.profileId()).getName();
            }
            if (ignoreSelf.get() && name.equals(mc.player.getName().getString())) return;
            if (listMode.get() == ListMode.Blacklist) {
                if (blacklist.get().contains(name)) return;
            } else {
                if (!(whitelist.get().contains(name) || (includeFriends.get() && Friends.get().get(name) != null))) return;
            }
            Instant now = Instant.now();
            infoUpdatesCacheTime.put(name, now);
            if (vanishedPlayers.contains(name)) return;
            vanishedPlayers.add(name);
            showVanishedNotification(name);
        }).start();
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
