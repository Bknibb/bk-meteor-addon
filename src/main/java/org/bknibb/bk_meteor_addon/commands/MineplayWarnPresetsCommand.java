package org.bknibb.bk_meteor_addon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.StringIdentifiable;

import java.util.Objects;

public class MineplayWarnPresetsCommand extends Command {
    public MineplayWarnPresetsCommand() {
        super("mp-warn", "Will warn a player using mineplay admin warn presets (requires /warn).");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        var argument = argument("player", StringArgumentType.word()).suggests((context, suggestionsBuilder) -> {
            if (mc.world == null) {
                return suggestionsBuilder.buildFuture();
            }
            if (mc.getNetworkHandler() == null) {
                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (player == mc.player) continue;
                    if (player.getName() == null) continue;
                    if (!CommandSource.shouldSuggest(suggestionsBuilder.getRemaining(), player.getName().getString())) continue;
                    suggestionsBuilder.suggest(player.getName().getString());
                }
                return suggestionsBuilder.buildFuture();
            }
            for (PlayerListEntry player : mc.getNetworkHandler().getPlayerList()) {
                if (mc.player != null && Objects.equals(player.getProfile().getName(), mc.player.getGameProfile().getName())) continue;
                if (player.getProfile().getName() == null) continue;
                if (!CommandSource.shouldSuggest(suggestionsBuilder.getRemaining(), player.getProfile().getName())) continue;
                suggestionsBuilder.suggest(player.getProfile().getName());
            }
            return suggestionsBuilder.buildFuture();
        });
        for (WarnPreset preset : WarnPreset.values()) {
            argument = argument.then(literal(preset.name()).executes(context -> {
                if (mc.getNetworkHandler() == null) return SINGLE_SUCCESS;
                String player = StringArgumentType.getString(context, "player");
                mc.getNetworkHandler().sendChatCommand("warn " + player + " " + "Please stop " + preset.asString() + ", if you continue, you will be banned");
                return SINGLE_SUCCESS;
            }).then(literal("-s").executes(context -> {
                if (mc.getNetworkHandler() == null) return SINGLE_SUCCESS;
                String player = StringArgumentType.getString(context, "player");
                mc.getNetworkHandler().sendChatCommand("warn " + player + " " + "Please stop " + preset.asString() + ", if you continue, you will be banned -s");
                return SINGLE_SUCCESS;
            })));
        }
        argument = argument.then(argument("text", StringArgumentType.greedyString()).executes(context -> {
            if (mc.getNetworkHandler() == null) return SINGLE_SUCCESS;
            String player = StringArgumentType.getString(context, "player");
            String text = StringArgumentType.getString(context, "text");
            String command = "warn " + player + " " + "Please stop " + text.replace(" -s", "") + ", if you continue, you will be banned";
            if (text.endsWith(" -s")) {
                command += " -s";
            }
            mc.getNetworkHandler().sendChatCommand(command);
            return SINGLE_SUCCESS;
        }));
        builder.then(argument);
    }

    private enum WarnPreset implements StringIdentifiable {
        Griefing,
        InappropriateBuilds,
        ActingInappropriately,
        BeingRacist,
        BeingHomophobic,
        Spamming;

        @Override
        public String asString() {
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
