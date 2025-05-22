package org.bknibb.bk_meteor_addon.modules;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPBlockData;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.other.JsonDateDeserializer;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.SignBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.resource.Resource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.ArrayListDeque;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import org.bknibb.bk_meteor_addon.BkMeteorAddon;
import org.bknibb.bk_meteor_addon.MineplayUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BadWordFinder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");

    private final Setting<BadWordList> badWordList = sgGeneral.add(new EnumSetting.Builder<BadWordList>()
        .name("bad-word-list")
        .description("Which bad word list to use.")
        .defaultValue(BadWordList.ModeratelyStrict)
        .onChanged(state -> refreshSigns())
        .build()
    );

    private final Setting<Boolean> extraRegexChecks = sgGeneral.add(new BoolSetting.Builder()
        .name("extra-regex-checks")
        .description("Use extra regex checks to find more variants of bad words.")
        .defaultValue(true)
        .onChanged(state -> refreshSigns())
        .build()
    );

    private final Setting<Boolean> extraNormalizing = sgGeneral.add(new BoolSetting.Builder()
        .name("extra-normalizing")
        .description("Use extra normalizing on messages to detect characters that look like other characters.")
        .defaultValue(true)
        .onChanged(state -> refreshSigns())
        .build()
    );

    private final Setting<Boolean> checkChatMessages = sgGeneral.add(new BoolSetting.Builder()
        .name("check-chat-messages")
        .description("Check for bad words in chat messages.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> checkSigns = sgGeneral.add(new BoolSetting.Builder()
        .name("check-signs")
        .description("Check for bad words in signs.")
        .defaultValue(true)
        .onChanged(state -> {if (state) refreshSigns();})
        .build()
    );

    private final Setting<Boolean> breakSigns = sgGeneral.add(new BoolSetting.Builder()
        .name("break-signs")
        .description("Breaks the signs when found (mineplay only).")
        .defaultValue(false)
        .visible(checkSigns::get)
        .onChanged(state -> {if (state) refreshSigns();})
        .build()
    );

    private final Setting<Boolean> eraseSigns = sgGeneral.add(new BoolSetting.Builder()
        .name("erase-signs")
        .description("Erases the signs when found (doesn't work with break signs) (mineplay only).")
        .defaultValue(false)
        .visible(checkSigns::get)
        .onChanged(state -> {if (state) refreshSigns();})
        .build()
    );

    private final Setting<ESPBlockData> signBlockConfig = sgGeneral.add(new GenericSetting.Builder<ESPBlockData>()
        .name("sign-block-config")
        .description("Sign block config.")
        .defaultValue(
            new ESPBlockData(
                ShapeMode.Lines,
                new SettingColor(255, 200, 0),
                new SettingColor(255, 200, 0, 25),
                true,
                new SettingColor(255, 200, 0, 125)
            )
        )
        .build()
    );

    private final Setting<Boolean> tracers = sgGeneral.add(new BoolSetting.Builder()
        .name("tracers")
        .description("Render tracer lines.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ListMode> listMode = sgWhitelist.add(new EnumSetting.Builder<ListMode>()
        .name("list-mode")
        .description("Selection mode.")
        .defaultValue(ListMode.Blacklist)
        .onChanged(state -> refreshSigns())
        .build()
    );

    private final Setting<List<String>> blacklist = sgWhitelist.add(new StringListSetting.Builder()
        .name("blacklist")
        .description("The players you don't want to see.")
        .visible(() -> listMode.get() == ListMode.Blacklist)
        .onChanged(state -> refreshSigns())
        .build()
    );

    private final Setting<Boolean> includeDefaultBadWordList = sgWhitelist.add(new BoolSetting.Builder()
        .name("include-default-bad-word-list")
        .description("Include the default bad word list in the whitelist.")
        .defaultValue(true)
        .visible(() -> listMode.get() == ListMode.Whitelist)
        .onChanged(state -> refreshSigns())
        .build()
    );

    private final Setting<List<String>> whitelist = sgWhitelist.add(new StringListSetting.Builder()
        .name("whitelist")
        .description("The players you want to see.")
        .visible(() -> listMode.get() == ListMode.Whitelist)
        .onChanged(state -> refreshSigns())
        .build()
    );

    private final ArrayListDeque<String> messageQueue = new ArrayListDeque<>();
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Date.class, new JsonDateDeserializer())
        .create();

    public BadWordFinder() {
        super(BkMeteorAddon.CATEGORY, "bad-word-finder", "Finds bad words in chat messages and nearby signs.");
    }

    @EventHandler
    private void onMessageRecieve(ReceiveMessageEvent event) {
        if (!checkChatMessages.get()) return;
        Text message = event.getMessage();
        if (message.getString().contains("[Meteor]")) return;
        EXECUTOR.submit(() -> {
            String badWord = getBadWord(message.getString().replaceAll("§[0-9a-fk-or]", ""));
            if (badWord != null) {
                messageQueue.addLast(Formatting.RESET + "Bad word " + Formatting.RED + badWord + Formatting.RESET + " found in message");
            }
        });
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        while (!messageQueue.isEmpty()) {
            String message = messageQueue.removeFirst();
            info(message);
        }
    }

    @EventHandler
    private void render(Render3DEvent event) {
        if (!checkSigns.get()) return;
        ESPBlockData signBlockData = signBlockConfig.get();
        if (mc.world == null) return;
        for (Map.Entry<BlockPos, BadSign> entry : badSigns.entrySet()) {
            BlockPos pos = entry.getKey();
            //BadSign badSign = entry.getValue();
            BlockState state = mc.world.getBlockState(pos);
            if (state == null || !state.hasBlockEntity() || !(state.getBlock() instanceof SignBlock)) {
                badSigns.remove(pos);
                return;
            }
            double x = pos.getX();
            double y = pos.getY();
            double z = pos.getZ();
            double x1 = x;
            double y1 = y;
            double z1 = z;
            double x2 = x + 1;
            double y2 = y + 1;
            double z2 = z + 1;
            VoxelShape shape = state.getOutlineShape(mc.world, pos);
            if (!shape.isEmpty()) {
                x1 = x + shape.getMin(Direction.Axis.X);
                y1 = y + shape.getMin(Direction.Axis.Y);
                z1 = z + shape.getMin(Direction.Axis.Z);
                x2 = x + shape.getMax(Direction.Axis.X);
                y2 = y + shape.getMax(Direction.Axis.Y);
                z2 = z + shape.getMax(Direction.Axis.Z);
            }
            ShapeMode shapeMode = signBlockData.shapeMode;
            Color lineColor = signBlockData.lineColor;
            Color sideColor = signBlockData.sideColor;
            event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor, lineColor, shapeMode, 0);
            if (tracers.get() && signBlockData.tracer) {
                event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, x + 0.5, y + 0.5, z + 0.5, signBlockData.tracerColor);
            }
        }
    }

    private List<String> strictBadWords;
    private List<String> moderatelyStrictBadWords;
    private List<String> lessStrictBadWords;
    private Map<Character, Character> confusables;
    private static ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onActivate() {
        refreshSigns();
    }

    @Override
    public void onDeactivate() {
        badSigns.clear();
    }

    @EventHandler
    public void onLeaveGame(GameLeftEvent event) {
        badSigns.clear();
        EXECUTOR.shutdownNow();
//        try {
//            if (!EXECUTOR.awaitTermination(1, TimeUnit.SECONDS)) {
//                System.err.println("Executor did not terminate in time.");
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        EXECUTOR = Executors.newSingleThreadExecutor();
    }

    public List<String> getBadWordsList() {
        if (badWordList.get() == BadWordList.Strict) {
            if (strictBadWords == null) {
                Identifier id = Identifier.of("bk-meteor-addon", "strict.json");
                Resource recource = mc.getResourceManager().getResource(id).orElse(null);
                if (recource == null) {
                    BkMeteorAddon.LOG.error("Failed to load bad words list: " + id);
                    info("Failed to load bad words list.");
                    toggle();
                    return new ArrayList<>();
                }
                try (InputStream stream = recource.getInputStream()) {
                    strictBadWords = GSON.fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new TypeToken<List<String>>() {}.getType());
                } catch (Exception e) {
                    BkMeteorAddon.LOG.error("Failed to load bad words list: " + e.getMessage());
                    info("Failed to load bad words list.");
                    toggle();
                    return new ArrayList<>();
                }
                moderatelyStrictBadWords = null;
                lessStrictBadWords = null;
            }
            return strictBadWords;
        } else if (badWordList.get() == BadWordList.ModeratelyStrict) {
            if (moderatelyStrictBadWords == null) {
                Identifier id = Identifier.of("bk-meteor-addon", "moderately-strict.json");
                Resource recource = mc.getResourceManager().getResource(id).orElse(null);
                if (recource == null) {
                    BkMeteorAddon.LOG.error("Failed to load bad words list: " + id);
                    info("Failed to load bad words list.");
                    toggle();
                    return new ArrayList<>();
                }
                try (InputStream stream = recource.getInputStream()) {
                    moderatelyStrictBadWords = GSON.fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new TypeToken<List<String>>() {}.getType());
                } catch (Exception e) {
                    BkMeteorAddon.LOG.error("Failed to load bad words list: " + e.getMessage());
                    info("Failed to load bad words list.");
                    toggle();
                    return new ArrayList<>();
                }
                strictBadWords = null;
                lessStrictBadWords = null;
            }
            return moderatelyStrictBadWords;
        } else if (badWordList.get() == BadWordList.LessStrict) {
            if (lessStrictBadWords == null) {
                Identifier id = Identifier.of("bk-meteor-addon", "less-strict.json");
                Resource recource = mc.getResourceManager().getResource(id).orElse(null);
                if (recource == null) {
                    BkMeteorAddon.LOG.error("Failed to load bad words list: " + id);
                    info("Failed to load bad words list.");
                    toggle();
                    return new ArrayList<>();
                }
                try (InputStream stream = recource.getInputStream()) {
                    lessStrictBadWords = GSON.fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new TypeToken<List<String>>() {}.getType());
                } catch (Exception e) {
                    BkMeteorAddon.LOG.error("Failed to load bad words list: " + e.getMessage());
                    info("Failed to load bad words list.");
                    toggle();
                    return new ArrayList<>();
                }
                strictBadWords = null;
                moderatelyStrictBadWords = null;
            }
            return lessStrictBadWords;
        }
        return new ArrayList<>();
    }

    public Map<Character, Character> getConfusables() {
        if (confusables == null) {
            Identifier id = Identifier.of("bk-meteor-addon", "confusables.json");
            Resource recource = mc.getResourceManager().getResource(id).orElse(null);
            if (recource == null) {
                BkMeteorAddon.LOG.error("Failed to load confusables list: " + id);
                info("Failed to load confusables list.");
                toggle();
                return new HashMap<>();
            }
            try (InputStream stream = recource.getInputStream()) {
                confusables = GSON.fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new TypeToken<Map<Character, Character>>() {}.getType());
            } catch (Exception e) {
                BkMeteorAddon.LOG.error("Failed to load confusables list: " + e.getMessage());
                info("Failed to load confusables list.");
                toggle();
                return new HashMap<>();
            }
        }
        return confusables;
    }

    private final String suffixPattern =
        "(?:" +
        "o" +                                // o
        "|i[\\W_]*n[\\W_]*g" +               // ing
        "|s" +                               // s
        "|e[\\W_]*r[\\W_]*s" +               // ers
        "|e[\\W_]*r" +                       // er
        "|e[\\W_]*d" +                       // ed
        ")?";

    private final String suffixPatternNoSpace =
        "(?:" +
        "o" +                                // o
        "|i[^\\w\\s_]*n[^\\w\\s_]*g" +       // ing
        "|s" +                               // s
        "|e[^\\w\\s_]*r[^\\w\\s_]*s" +       // ers
        "|e[^\\w\\s_]*r" +                   // er
        "|e[^\\w\\s_]*d" +                   // ed
        ")?";

    private boolean doCheckWord(String word, String message) {
        String regex = "(?i)(?<=^|\\W)" + Pattern.quote(word.replace("<nospaces>", "")) + "(?=\\W|$)";
        if (extraRegexChecks.get()) {
            String interleaved;
            if (word.startsWith("<nospaces>")) {
                interleaved = String.join("[^\\w\\s_]*", Arrays.stream(word.replace("<nospaces>", "").split("")).map(Pattern::quote).toArray(String[]::new));
            } else {
                interleaved = String.join("[\\W_]*", Arrays.stream(word.split("")).map(Pattern::quote).toArray(String[]::new));
            }
            String currentSuffixPattern = suffixPattern;
            if (word.endsWith("o") || word.endsWith("ing") || word.endsWith("s") || word.endsWith("ers") || word.endsWith("er") || word.endsWith("ed")) {
                currentSuffixPattern = "";
            } else if (word.startsWith("<nospaces>")) {
                currentSuffixPattern = suffixPatternNoSpace;
            }
            if (word.startsWith("<nospaces>")) {
                regex = "(?i)(?<=^|\\W)" + interleaved + "[^\\w\\s_]*" + currentSuffixPattern + "(?=\\W|$)";
            } else {
                regex = "(?i)(?<=^|\\W)" + interleaved + "[\\W_]*" + currentSuffixPattern + "(?=\\W|$)";
            }
        }
        Pattern pattern = Pattern.compile(regex);
        if (extraNormalizing.get()) {
            Map<Character, Character> confusables = getConfusables();
            StringBuilder sb = new StringBuilder();
            for (char c : message.toCharArray()) {
                sb.append(confusables.getOrDefault(c, c));
            }
            message = sb.toString();
        }
        Matcher matcher = pattern.matcher(message);
        return matcher.find();
    }

    public boolean containsBadWord(String message) {
        if (!isActive()) return false;
        if (listMode.get() == ListMode.Whitelist) {
            if (includeDefaultBadWordList.get()) {
                for (String word : getBadWordsList()) {
                    if (doCheckWord(word, message)) {
                        return true;
                    }
                }
            }
            for (String word : whitelist.get()) {
                if (doCheckWord(word, message)) {
                    return true;
                }
            }
        } else {
            for (String word : getBadWordsList()) {
                if (blacklist.get().contains(word)) continue;
                if (doCheckWord(word, message)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getBadWord(String message) {
        if (!isActive()) return null;
        if (listMode.get() == ListMode.Whitelist) {
            if (includeDefaultBadWordList.get()) {
                for (String word : getBadWordsList()) {
                    if (doCheckWord(word, message)) {
                        return word.replace("<nospaces>", "");
                    }
                }
            }
            for (String word : whitelist.get()) {
                if (doCheckWord(word, message)) {
                    return word.replace("<nospaces>", "");
                }
            }
        } else {
            for (String word : getBadWordsList()) {
                if (blacklist.get().contains(word)) continue;
                if (doCheckWord(word, message)) {
                    return word.replace("<nospaces>", "");
                }
            }
        }
        return null;
    }

    private final Map<BlockPos, BadSign> badSigns = new ConcurrentHashMap<>();

    private void doBadWordCheck(Text[] texts, BlockPos pos, boolean back) {
        boolean hasBadWord = false;
        String badWord = null;
        for (Text text : texts) {
            badWord = getBadWord(text.getString().replaceAll("§[0-9a-fk-or]", ""));
            if (badWord != null) {
                hasBadWord = true;
                break;
            }
        }
        if (hasBadWord) {
            info(Formatting.RESET + "Bad word " + Formatting.RED + badWord + Formatting.RESET + " found in sign at " + pos.toShortString());
            if (MineplayUtils.isOnMineplay() && mc.getNetworkHandler() != null) {
                if (breakSigns.get()) {
                    if (mc.player != null) {
                        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.getX(), pos.getY(), pos.getZ(), false, mc.player.horizontalCollision));
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
                        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, mc.player.horizontalCollision));
                        badSigns.remove(pos);
                        return;
                    }
                } else if (eraseSigns.get()) {
                    mc.getNetworkHandler().sendPacket(new UpdateSignC2SPacket(pos, !back, "", "", "", ""));
                    if (badSigns.containsKey(pos)) {
                        BadSign badSign = badSigns.get(pos);
                        if (back) {
                            badSign.backBad = false;
                        } else {
                            badSign.frontBad = false;
                        }
                        if (!badSign.frontBad && !badSign.backBad) {
                            badSigns.remove(pos);
                        }
                    }
                    return;
                }
            }
            if (badSigns.containsKey(pos)) {
                BadSign badSign = badSigns.get(pos);
                if (back) {
                    badSign.backBad = true;
                } else {
                    badSign.frontBad = true;
                }
            } else {
                BadSign badSign = new BadSign(!back, back);
                badSigns.put(pos, badSign);
            }
        } else {
            if (badSigns.containsKey(pos)) {
                BadSign badSign = badSigns.get(pos);
                if (back) {
                    badSign.backBad = false;
                } else {
                    badSign.frontBad = false;
                }
                if (!badSign.frontBad && !badSign.backBad) {
                    badSigns.remove(pos);
                }
            }
        }
    }

    public void badWordCheck(Text[] texts, BlockPos pos, boolean back) {
        if (!isActive() || !checkSigns.get()) return;
        EXECUTOR.submit(() -> doBadWordCheck(texts, pos, back));
    }

    public static void BadWordCheck(Text[] texts, BlockPos pos, boolean back) {
        Modules.get().get(BadWordFinder.class).badWordCheck(texts, pos, back);
    }

    public void refreshSigns(/*boolean clear*/) {
        //if (clear) badSigns.clear();
        if (mc.world == null) return;
        for (BlockEntity block : Utils.blockEntities()) {
            if (block instanceof SignBlockEntity sign) {
                BlockPos pos = sign.getPos();
                Text[] textsFront = sign.getFrontText().getMessages(false);
                Text[] textsBack = sign.getBackText().getMessages(false);
                badWordCheck(textsFront, pos, false);
                badWordCheck(textsBack, pos, true);
            }
        }
    }


    private static class BadSign {
        public boolean frontBad;
        public boolean backBad;
        public BadSign(boolean frontBad, boolean backBad) {
            this.frontBad = frontBad;
            this.backBad = backBad;
        }
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }

    public enum BadWordList {
        Strict,
        ModeratelyStrict,
        LessStrict
    }
}
