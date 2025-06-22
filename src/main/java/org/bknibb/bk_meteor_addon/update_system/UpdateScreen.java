package org.bknibb.bk_meteor_addon.update_system;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import org.bknibb.bk_meteor_addon.BkMeteorAddon;
import org.bknibb.bk_meteor_addon.ConfigModifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class UpdateScreen extends WindowScreen {
    private final MeteorAddon addon;
    private final UpdateSystem.ReleaseResponse relaseResponse;
    public UpdateScreen(GuiTheme theme, MeteorAddon addon, UpdateSystem.ReleaseResponse relaseResponse) {
        super(theme, "A new version of " + addon.name + " is available.");
        this.addon = addon;
        this.relaseResponse = relaseResponse;
    }

    @Override
    public void initWidgets() {
        WHorizontalList l = add(theme.horizontalList()).expandX().widget();
        String date = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(relaseResponse.published_at));
        l.add(theme.label(addon.name + " " + relaseResponse.name + " from " + date + " is available")).expandX();
        l.add(theme.button("Don't Show Again")).widget().action = () -> {
            ConfigModifier.get().checkForUpdates.set(false);
            close();
        };
        l.add(theme.button("Download and Exit")).widget().action = () -> {
            Http.Request request = Http.get(relaseResponse.assets.getFirst().browser_download_url);
            request.exceptionHandler(e -> {
                close();
                UpdateSystem.LOG.warn("Failed to download update: " + e.getMessage());
                //MinecraftClient.getInstance().getToastManager().add(new MeteorToast(null, "Update Failed", "Failed to download update: " + e.getMessage()));
                MinecraftClient.getInstance().setScreen(new UpdateFailedScreen(theme, addon, relaseResponse, "Failed to download update", e.getMessage()));
            });
            addon.getRepo().authenticate(request);
            HttpResponse<InputStream> res = request.sendInputStreamResponse();
            switch (res.statusCode()) {
                case Http.UNAUTHORIZED -> {
                    String message = "Invalid authentication token for repository '%s'".formatted(addon.getRepo().getOwnerName());
                    MinecraftClient.getInstance().getToastManager().add(new MeteorToast.Builder("GitHub: Unauthorized").icon(Items.BARRIER).text(message).build());
                    UpdateSystem.LOG.warn(message);
                    if (System.getenv("meteor.github.authorization") == null) {
                        UpdateSystem.LOG.info("Consider setting an authorization " +
                            "token with the 'meteor.github.authorization' environment variable.");
                        UpdateSystem.LOG.info("See: https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens");
                    }
                    close();
                    //MinecraftClient.getInstance().getToastManager().add(new MeteorToast(null, "Update Failed", "Failed to download update: Invalid authentication token"));
                    MinecraftClient.getInstance().setScreen(new UpdateFailedScreen(theme, addon, relaseResponse, "Failed to download update", "Invalid authentication token"));
                    return;
                }
                case Http.FORBIDDEN -> {
                    UpdateSystem.LOG.warn("Failed to download update: Rate-limited by GitHub.");
                    close();
                    //MinecraftClient.getInstance().getToastManager().add(new MeteorToast(null, "Update Failed", "Failed to download update: Rate-limited by GitHub"));
                    MinecraftClient.getInstance().setScreen(new UpdateFailedScreen(theme, addon, relaseResponse, "Failed to download update", "Rate-limited by GitHub"));
                    return;
                }
                case Http.NOT_FOUND -> {
                    UpdateSystem.LOG.warn("Failed to download update: GitHub repository '{}' not found.", addon.getRepo().getOwnerName());
                    close();
                    //MinecraftClient.getInstance().getToastManager().add(new MeteorToast(null, "Update Failed", "Failed to download update: GitHub repository not found"));
                    MinecraftClient.getInstance().setScreen(new UpdateFailedScreen(theme, addon, relaseResponse, "Failed to download update", "GitHub repository not found"));
                    return;
                }
                case Http.SUCCESS -> {
                    Path modPath = FabricLoader.getInstance().getModContainer(BkMeteorAddon.MOD_ID).get().getOrigin().getPaths().getFirst();
                    Path target = modPath.getParent().resolve(relaseResponse.assets.getFirst().name);
                    try (OutputStream os = Files.newOutputStream(target); InputStream is = res.body()) {
                        is.transferTo(os);
                    } catch (IOException e) {
                        close();
                        UpdateSystem.LOG.warn("Failed to download update: " + e.getMessage());
                        //MinecraftClient.getInstance().getToastManager().add(new MeteorToast(null, "Update Failed", "Failed to download update: " + e.getMessage()));
                        MinecraftClient.getInstance().setScreen(new UpdateFailedScreen(theme, addon, relaseResponse, "Failed to download update", e.getMessage()));
                        return;
                    }
//                    if (!modPath.equals(target)) {
//                        //try {
//                            //Files.delete(modPath);
//                            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                                try {
//                                    Files.delete(modPath);
//                                } catch (IOException e) {
//                                    UpdateSystem.LOG.warn("Failed to delete old mod file: " + e.getMessage());
//                                }
//                            }));
////                        } catch (IOException e) {
////                            close();
////                            UpdateSystem.LOG.warn("Failed to delete old mod file: " + e.getMessage());
////                            //MinecraftClient.getInstance().getToastManager().add(new MeteorToast(null, "Failed To Delete Old File", "Failed to delete old mod file: " + e.getMessage()));
////                            MinecraftClient.getInstance().setScreen(new UpdateFailedScreen(theme, addon, relaseResponse, "Failed to delete old mod file",  e.getMessage()));
////                            return;
////                        }
//                    }
                    if (!modPath.equals(target)) {
                        modPath.toFile().deleteOnExit();
                    }
                }
                default -> {
                    close();
                    UpdateSystem.LOG.warn("Failed to download update: " + res.statusCode());
                    //MinecraftClient.getInstance().getToastManager().add(new MeteorToast(null, "Update Failed", "Failed to download update: " + res.statusCode()));
                    MinecraftClient.getInstance().setScreen(new UpdateFailedScreen(theme, addon, relaseResponse, "Failed to download update", String.valueOf(res.statusCode())));
                    return;
                }
            }
            UpdateSystem.LOG.info("Update success, exiting...");
            MinecraftClient.getInstance().scheduleStop();
        };
        add(theme.horizontalSeparator()).padVertical(theme.scale(8)).expandX();
        add(theme.label(relaseResponse.body)).expandX();
    }
}
