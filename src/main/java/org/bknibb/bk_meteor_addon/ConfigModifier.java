package org.bknibb.bk_meteor_addon;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;

public class ConfigModifier {
    private static ConfigModifier INSTANCE;

    public final SettingGroup sgBkMeteorAddon = Config.get().settings.createGroup("BkMeteorAddon");

    public final Setting<Boolean> checkForUpdates = sgBkMeteorAddon.add(new BoolSetting.Builder()
            .name("check-for-updates")
            .description(BkMeteorAddon.UPDATER_ENABLED ? "Check for BkMeteorAddon updates." : "Check for BkMeteorAddon updates (disabled in this build).")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> chatBotSender = sgBkMeteorAddon.add(new BoolSetting.Builder()
            .name("chat-bot-sender")
            .description("Replace <sender> with the sender's name in the meteor-rejects chat bot.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> chatBotArgs = sgBkMeteorAddon.add(new BoolSetting.Builder()
        .name("chat-bot-args")
        .description("Replace <args> with the args passed in the chat in the meteor-rejects chat bot (warning changes the check from the end of the message to contained in the message).")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> chatBotMessageDelayOption = sgBkMeteorAddon.add(new BoolSetting.Builder()
        .name("chat-bot-message-delay-option")
        .description("Adds a message delay option to meteor-rejects chat bot (restart required for apply).")
        .defaultValue(true)
        .build()
    );

    public static ConfigModifier get() {
        if (INSTANCE == null) INSTANCE = new ConfigModifier();
        return INSTANCE;
    }
}
