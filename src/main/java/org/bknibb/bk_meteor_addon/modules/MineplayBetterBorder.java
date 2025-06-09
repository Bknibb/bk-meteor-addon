package org.bknibb.bk_meteor_addon.modules;

import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import org.bknibb.bk_meteor_addon.BkMeteorAddon;

public class MineplayBetterBorder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Double> shrinkBy = sgGeneral.add(new DoubleSetting.Builder()
        .name("shrink-by")
        .description("The amount to shrink the border.")
        .defaultValue(0.2)
        .range(0.2, 20)
        .sliderRange(0.2, 2)
        .build()
    );

    public MineplayBetterBorder() {
        super(BkMeteorAddon.CATEGORY, "mineplay-better-border", "Makes the world border have smaller collisions to stop spawn teleporting (for mineplay).");
    }
}
