package org.bknibb.bk_meteor_addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.command.CommandSource;
import org.bknibb.bk_meteor_addon.modules.VanishDetect;

public class VanishedCommand extends Command {
    public VanishedCommand() {
        super("vanished", "Shows vanished players on the servert, gets settings from and requires VanishDetect.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            VanishDetect module = Modules.get().get(VanishDetect.class);
            if (!module.isActive()) {
                error("VanishDetect is not active.");
                return SINGLE_SUCCESS;
            }
            for (String name : module.vanishedPlayers) {
                module.showVanishedNotification(name);
            }
            return SINGLE_SUCCESS;
        });
    }
}
