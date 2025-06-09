package org.bknibb.bk_meteor_addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import org.bknibb.bk_meteor_addon.UpdatableResourcesManager;

public class BkUpdateResourcesCommand extends Command {
    public BkUpdateResourcesCommand() {
        super("bk-update-resources", "Updates the updatable resources of Bk Meteor Addon.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (UpdatableResourcesManager.get().CheckForUpdates(false)) {
                info("Updated Resources.");
            } else {
                info("No updates found.");
            }
            return SINGLE_SUCCESS;
        });
        builder.then(literal("force").executes(context -> {
            if (UpdatableResourcesManager.get().CheckForUpdates(true)) {
                info("Updated Resources.");
            } else {
                info("No updates found.");
            }
            return SINGLE_SUCCESS;
        }));
    }
}
