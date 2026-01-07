package org.bknibb.bk_meteor_addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.commands.SharedSuggestionProvider;

public class MineplayBlocksCommand extends Command {
    public MineplayBlocksCommand() {
        super("mp-blocks", "Will tell players how to get blocks.");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            if (mc.getConnection() == null) return SINGLE_SUCCESS;
            mc.getConnection().sendChat("Use /blocks or /b to open the blocks menu!");
            return SINGLE_SUCCESS;
        });
    }
}
