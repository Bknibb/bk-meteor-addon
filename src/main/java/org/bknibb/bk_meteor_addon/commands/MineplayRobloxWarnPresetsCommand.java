package org.bknibb.bk_meteor_addon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.util.StringIdentifiable;
import org.bknibb.bk_meteor_addon.MineplayUtils;

import java.util.Objects;

public class MineplayRobloxWarnPresetsCommand extends Command {
    public MineplayRobloxWarnPresetsCommand() {
        super("mp-rwarn", "Will warn a roblox player using mineplay admin warn presets.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        var argument = argument("player", StringArgumentType.word()).suggests((context, suggestionsBuilder) -> {
            if (mc.getNetworkHandler() == null) return suggestionsBuilder.buildFuture();
            for (PlayerListEntry player : mc.getNetworkHandler().getPlayerList()) {
                if (mc.player != null && Objects.equals(player.getProfile().getName(), mc.player.getGameProfile().getName())) continue;
                if (player.getProfile().getName() == null) continue;
                if (!MineplayUtils.isRobloxPlayer(player)) continue;
                if (!CommandSource.shouldSuggest(suggestionsBuilder.getRemaining(), player.getProfile().getName())) continue;
                suggestionsBuilder.suggest(player.getProfile().getName());
            }
            return suggestionsBuilder.buildFuture();
        });
        for (RWarnPreset preset : RWarnPreset.values()) {
            argument = argument.then(literal(preset.name()).executes(context -> {
                if (mc.getNetworkHandler() == null) return SINGLE_SUCCESS;
                String player = StringArgumentType.getString(context, "player");
                mc.getNetworkHandler().sendChatMessage(player + " " + "Please stop " + preset.asString() + ", if you continue, you will be banned");
                return SINGLE_SUCCESS;
            }));
        }
        argument = argument.then(argument("text", StringArgumentType.greedyString()).executes(context -> {
            if (mc.getNetworkHandler() == null) return SINGLE_SUCCESS;
            String player = StringArgumentType.getString(context, "player");
            String text = StringArgumentType.getString(context, "text");
            mc.getNetworkHandler().sendChatMessage(player + " " + "Please stop " + text + ", if you continue, you will be banned");
            return SINGLE_SUCCESS;
        }));
        builder.then(argument);
    }

    private enum RWarnPreset implements StringIdentifiable {
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
