package org.bknibb.bk_meteor_addon.update_system;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import net.minecraft.util.Util;

public class UpdateFailedScreen extends WindowScreen {
    private final MeteorAddon addon;
    private final UpdateSystem.ReleaseResponse relaseResponse;
    private final String errorMessage;
    private final String errorTitle;
    public UpdateFailedScreen(GuiTheme theme, MeteorAddon addon, UpdateSystem.ReleaseResponse relaseResponse, String errorTitle, String errorMessage) {
        super(theme, "Update failed for " + addon.name + ".");
        this.addon = addon;
        this.relaseResponse = relaseResponse;
        this.errorTitle = errorTitle;
        this.errorMessage = errorMessage;
    }
    @Override
    public void initWidgets() {
        add(theme.label(errorTitle)).expandX();
        add(theme.label("Please try again later.")).expandX();
        add(theme.label(errorMessage)).expandX();
        add(theme.button("Close")).expandX().widget().action = this::onClose;
        add(theme.button("Open Release Page")).expandX().widget().action = () -> {
            Util.getPlatform().openUri(relaseResponse.html_url);
            onClose();
        };
    }
}
