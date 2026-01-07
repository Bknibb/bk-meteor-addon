package org.bknibb.bk_meteor_addon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import java.util.Objects;

public class MineplayWarnPresetsCommand extends Command {
    public MineplayWarnPresetsCommand() {
        super("mp-warn", "Will warn a player using mineplay admin warn presets (requires /warn).");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        var argument = argument("player", StringArgumentType.word()).suggests((context, suggestionsBuilder) -> {
            if (mc.level == null) {
                return suggestionsBuilder.buildFuture();
            }
            if (mc.getConnection() == null) {
                for (Player player : mc.level.players()) {
                    if (player == mc.player) continue;
                    if (player.getName() == null) continue;
                    if (!SharedSuggestionProvider.matchesSubStr(suggestionsBuilder.getRemaining(), player.getName().getString())) continue;
                    suggestionsBuilder.suggest(player.getName().getString());
                }
                return suggestionsBuilder.buildFuture();
            }
            for (PlayerInfo player : mc.getConnection().getOnlinePlayers()) {
                if (mc.player != null && Objects.equals(player.getProfile().name(), mc.player.getGameProfile().name())) continue;
                if (player.getProfile().name() == null) continue;
                if (!SharedSuggestionProvider.matchesSubStr(suggestionsBuilder.getRemaining(), player.getProfile().name())) continue;
                suggestionsBuilder.suggest(player.getProfile().name());
            }
            return suggestionsBuilder.buildFuture();
        });
        for (WarnPreset preset : WarnPreset.values()) {
            argument = argument.then(literal(preset.name()).executes(context -> {
                if (mc.getConnection() == null) return SINGLE_SUCCESS;
                String player = StringArgumentType.getString(context, "player");
                mc.getConnection().sendCommand("warn " + player + " " + "Please stop " + preset.getSerializedName() + ", if you continue, you will be banned");
                return SINGLE_SUCCESS;
            }).then(literal("-s").executes(context -> {
                if (mc.getConnection() == null) return SINGLE_SUCCESS;
                String player = StringArgumentType.getString(context, "player");
                mc.getConnection().sendCommand("warn " + player + " " + "Please stop " + preset.getSerializedName() + ", if you continue, you will be banned -s");
                return SINGLE_SUCCESS;
            })));
        }
        argument = argument.then(argument("text", StringArgumentType.greedyString()).executes(context -> {
            if (mc.getConnection() == null) return SINGLE_SUCCESS;
            String player = StringArgumentType.getString(context, "player");
            String text = StringArgumentType.getString(context, "text");
            String command = "warn " + player + " " + "Please stop " + text.replace(" -s", "") + ", if you continue, you will be banned";
            if (text.endsWith(" -s")) {
                command += " -s";
            }
            mc.getConnection().sendCommand(command);
            return SINGLE_SUCCESS;
        }));
        builder.then(argument);
    }

    private enum WarnPreset implements StringRepresentable {
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
