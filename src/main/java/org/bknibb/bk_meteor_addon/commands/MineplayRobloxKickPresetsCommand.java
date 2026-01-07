package org.bknibb.bk_meteor_addon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.util.StringRepresentable;
import org.bknibb.bk_meteor_addon.MineplayUtils;

import java.util.Objects;

public class MineplayRobloxKickPresetsCommand extends Command {
    public MineplayRobloxKickPresetsCommand() {
        super("mp-rkick", "Will kick a roblox player using mineplay admin kick presets.");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        var argument = argument("player", StringArgumentType.word()).suggests((context, suggestionsBuilder) -> {
            if (mc.getConnection() == null) return suggestionsBuilder.buildFuture();
            for (PlayerInfo player : mc.getConnection().getOnlinePlayers()) {
                if (mc.player != null && Objects.equals(player.getProfile().name(), mc.player.getGameProfile().name())) continue;
                if (player.getProfile().name() == null) continue;
                if (!MineplayUtils.isRobloxPlayer(player)) continue;
                if (!SharedSuggestionProvider.matchesSubStr(suggestionsBuilder.getRemaining(), player.getProfile().name())) continue;
                suggestionsBuilder.suggest(player.getProfile().name());
            }
            return suggestionsBuilder.buildFuture();
        });
        for (RKickPreset preset : RKickPreset.values()) {
            argument = argument.then(literal(preset.name()).executes(context -> {
                if (mc.getConnection() == null) return SINGLE_SUCCESS;
                String player = StringArgumentType.getString(context, "player");
                mc.getConnection().sendCommand("rkick " + player + " " + "\"Please stop " + preset.getSerializedName() + ", if you continue, you will be banned - Kicked Warn\"");
                return SINGLE_SUCCESS;
            }));
        }
        argument = argument.then(argument("text", StringArgumentType.greedyString()).executes(context -> {
            if (mc.getConnection() == null) return SINGLE_SUCCESS;
            String player = StringArgumentType.getString(context, "player");
            String text = StringArgumentType.getString(context, "text");
            mc.getConnection().sendCommand("rkick " + player + " " + "\"Please stop " + text + ", if you continue, you will be banned - Kicked Warn\"");
            return SINGLE_SUCCESS;
        }));
        builder.then(argument);
    }

    private enum RKickPreset implements StringRepresentable {
        Griefing,
        InappropriateBuilds,
        ActingInappropriately,
        BeingRacist,
        BeingHomophobic,
        Spamming;

        @Override
        public String getSerializedName() {
            if (this == Griefing) {
                return "Griefing";
            } else if (this == InappropriateBuilds) {
                return "Building Inappropriately";
            } else if (this == ActingInappropriately) {
                return "Acting Inappropriately";
            } else if (this == BeingRacist) {
                return "Acting Racist";
            } else if (this == BeingHomophobic) {
                return "Acting Homophobic";
            } else if (this == Spamming) {
                return "Spamming";
            }
            return null;
        }
    }
}
