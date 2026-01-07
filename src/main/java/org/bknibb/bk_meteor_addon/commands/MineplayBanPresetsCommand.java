package org.bknibb.bk_meteor_addon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import java.util.Objects;

public class MineplayBanPresetsCommand extends Command {
    public MineplayBanPresetsCommand() {
        super("mp-ban", "Will ban a player using mineplay admin ban presets (requires /ban).");
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
        for (BanPreset preset : BanPreset.values()) {
            argument = argument.then(literal(preset.name()).executes(context -> {
                if (mc.getConnection() == null) return SINGLE_SUCCESS;
                String player = StringArgumentType.getString(context, "player");
                mc.getConnection().sendCommand("ban " + player + " " + preset.getSerializedName());
                return SINGLE_SUCCESS;
            }).then(literal("-s").executes(context -> {
                if (mc.getConnection() == null) return SINGLE_SUCCESS;
                String player = StringArgumentType.getString(context, "player");
                mc.getConnection().sendCommand("ban " + player + " " + preset.getSerializedName() + " -s");
                return SINGLE_SUCCESS;
            })));
        }
        argument = argument.then(argument("text", StringArgumentType.greedyString()).executes(context -> {
            if (mc.getConnection() == null) return SINGLE_SUCCESS;
            String player = StringArgumentType.getString(context, "player");
            String text = StringArgumentType.getString(context, "text");
            mc.getConnection().sendCommand("ban " + player + " " + text);
            return SINGLE_SUCCESS;
        }));
        builder.then(argument);
    }

    private enum BanPreset implements StringRepresentable {
        Griefing,
        InappropriateBehaviour,
        InappropriateBuilds,
        Racism,
        Homophobia,
        HateSpeech;

        @Override
        public String getSerializedName() {
            if (this == Griefing) {
                return "Griefing";
            } else if (this == InappropriateBuilds) {
                return "InappropriateBuilds";
            } else if (this == InappropriateBehaviour) {
                return "InappropriateBehaviour";
            } else if (this == Racism) {
                return "Racism";
            } else if (this == Homophobia) {
                return "Homophobia";
            } else if (this == HateSpeech) {
                return "Hate Speech";
            }
            return null;
        }
    }
}
