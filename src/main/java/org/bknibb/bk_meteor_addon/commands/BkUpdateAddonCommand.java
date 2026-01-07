package org.bknibb.bk_meteor_addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.commands.SharedSuggestionProvider;
import org.bknibb.bk_meteor_addon.BkMeteorAddon;
import org.bknibb.bk_meteor_addon.update_system.UpdateSystem;

public class BkUpdateAddonCommand extends Command {
    public BkUpdateAddonCommand() {
        super("bk-update-addon", "Updates Bk Meteor Addon.");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            if (UpdateSystem.checkForUpdates(BkMeteorAddon.INSTNACE)) {
                info("Update Found.");
            } else {
                info("No updates found.");
            }
            return SINGLE_SUCCESS;
        });
    }
}
