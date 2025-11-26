package org.bknibb.bk_meteor_addon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.StringIdentifiable;

import java.util.Objects;

public class MineplayBanPresetsCommand extends Command {
    public MineplayBanPresetsCommand() {
        super("mp-ban", "Will ban a player using mineplay admin ban presets (requires /ban).");
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
                if (mc.player != null && Objects.equals(player.getProfile().name(), mc.player.getGameProfile().name())) continue;
                if (player.getProfile().name() == null) continue;
                if (!CommandSource.shouldSuggest(suggestionsBuilder.getRemaining(), player.getProfile().name())) continue;
                suggestionsBuilder.suggest(player.getProfile().name());
            }
            return suggestionsBuilder.buildFuture();
        });
        for (BanPreset preset : BanPreset.values()) {
            argument = argument.then(literal(preset.name()).executes(context -> {
                if (mc.getNetworkHandler() == null) return SINGLE_SUCCESS;
                String player = StringArgumentType.getString(context, "player");
                mc.getNetworkHandler().sendChatCommand("ban " + player + " " + preset.asString());
                return SINGLE_SUCCESS;
            }).then(literal("-s").executes(context -> {
                if (mc.getNetworkHandler() == null) return SINGLE_SUCCESS;
                String player = StringArgumentType.getString(context, "player");
                mc.getNetworkHandler().sendChatCommand("ban " + player + " " + preset.asString() + " -s");
                return SINGLE_SUCCESS;
            })));
        }
        argument = argument.then(argument("text", StringArgumentType.greedyString()).executes(context -> {
            if (mc.getNetworkHandler() == null) return SINGLE_SUCCESS;
            String player = StringArgumentType.getString(context, "player");
            String text = StringArgumentType.getString(context, "text");
            mc.getNetworkHandler().sendChatCommand("ban " + player + " " + text);
            return SINGLE_SUCCESS;
        }));
        builder.then(argument);
    }

    private enum BanPreset implements StringIdentifiable {
        Griefing,
        InappropriateBehaviour,
        InappropriateBuilds,
        Racism,
        Homophobia,
        HateSpeech;

        @Override
        public String asString() {
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
