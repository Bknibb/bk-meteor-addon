package org.bknibb.bk_meteor_addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.bknibb.bk_meteor_addon.modules.NetworkLoginLogoutNotifier;

public class NetworkOnlineCommand extends Command {
    public NetworkOnlineCommand() {
        super("network-online", "Shows online players on the network, gets settings from and requires NetworkLoginLogoutNotifier (for mineplay, also may work on other server networks).");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            NetworkLoginLogoutNotifier module = Modules.get().get(NetworkLoginLogoutNotifier.class);
            if (!module.isActive()) {
                error("NetworkLoginLogoutNotifier is not active.");
                return SINGLE_SUCCESS;
            }
            for (String name : module.onlinePlayers) {
                showOnlineNotification(name);
            }
            return SINGLE_SUCCESS;
        });
    }

    private void showOnlineNotification(String name) {
        if (Modules.get().get(NetworkLoginLogoutNotifier.class).simpleNotifications.get()) {
            if (mc.player == null) return;
            mc.player.displayClientMessage(Component.literal(
                ChatFormatting.GRAY + "["
                    + ChatFormatting.LIGHT_PURPLE + "Network"
                    + ChatFormatting.GRAY + "] "
                    + ChatFormatting.GRAY + "["
                    + ChatFormatting.GREEN + "Online"
                    + ChatFormatting.GRAY + "] "
                    + ChatFormatting.RESET + name
            ), false);
        } else {
            ChatUtils.sendMsg(Component.literal(
                name
                    + " is "
                    + ChatFormatting.GREEN + "online"
                    + ChatFormatting.RESET + " on the network."
            ));
        }
    }
}
