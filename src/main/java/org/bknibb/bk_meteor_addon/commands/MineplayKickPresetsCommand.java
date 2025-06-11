package org.bknibb.bk_meteor_addon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.StringIdentifiable;

public class MineplayKickPresetsCommand extends Command {
    public MineplayKickPresetsCommand() {
        super("mp-kick", "Will kick a player using mineplay admin kick presets (requires /kick).");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        var argument = argument("player", PlayerArgumentType.create());
        for (KickPreset preset : KickPreset.values()) {
            argument = argument.then(literal(preset.name()).executes(context -> {
                if (mc.getNetworkHandler() == null) return SINGLE_SUCCESS;
                PlayerEntity player = PlayerArgumentType.get(context);
                mc.getNetworkHandler().sendChatCommand("kick " + player.getName().getString() + " " + "Please stop " + preset.asString() + ", if you continue, you will be banned - Kicked Warn");
                return SINGLE_SUCCESS;
            }).then(literal("-s").executes(context -> {
                if (mc.getNetworkHandler() == null) return SINGLE_SUCCESS;
                PlayerEntity player = PlayerArgumentType.get(context);
                mc.getNetworkHandler().sendChatCommand("kick " + player.getName().getString() + " " + "Please stop " + preset.asString() + ", if you continue, you will be banned - Kicked Warn -s");
                return SINGLE_SUCCESS;
            })));
        }
        argument = argument.then(argument("text", StringArgumentType.greedyString()).executes(context -> {
            if (mc.getNetworkHandler() == null) return SINGLE_SUCCESS;
            PlayerEntity player = PlayerArgumentType.get(context);
            String text = StringArgumentType.getString(context, "text");
            String command = "kick " + player.getName().getString() + " " + "Please stop " + text.replace(" -s", "") + ", if you continue, you will be banned - Kicked Warn";
            if (text.endsWith(" -s")) {
                command += " -s";
            }
            mc.getNetworkHandler().sendChatCommand(command);
            return SINGLE_SUCCESS;
        }));
        builder.then(argument);
    }

    private enum KickPreset implements StringIdentifiable {
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
