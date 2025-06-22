package org.bknibb.bk_meteor_addon.update_system;

import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import org.bknibb.bk_meteor_addon.BkMeteorAddon;
import org.slf4j.Logger;

import java.net.http.HttpResponse;
import java.util.List;

public class UpdateSystem {
    private static final CheckMode checkMode = CheckMode.Version;
    public static final Logger LOG = BkMeteorAddon.LOG;
    public static boolean checkForUpdates(MeteorAddon addon) {
        if (!BkMeteorAddon.UPDATER_ENABLED) {
            LOG.info("Update system is disabled.");
            return false;
        }
        if (checkMode == CheckMode.Commit && addon.getCommit() == null) return false;
        // Check for updates
        GithubRepo repo = addon.getRepo();
        if (repo == null) {
            LOG.warn("Could not check for updates.");
            return false;
        }
        Http.Request request = Http.get("https://api.github.com/repos/%s/releases/latest".formatted(repo.getOwnerName()));
        request.exceptionHandler(e -> LOG.warn("Could not check for updates: " + e.getMessage()));
        repo.authenticate(request);
        HttpResponse<ReleaseResponse> res = request.sendJsonResponse(ReleaseResponse.class);
        switch (res.statusCode()) {
            case Http.UNAUTHORIZED -> {
                String message = "Invalid authentication token for repository '%s'".formatted(repo.getOwnerName());
                MinecraftClient.getInstance().getToastManager().add(new MeteorToast.Builder("GitHub: Unauthorized").icon(Items.BARRIER).text(message).build());
                LOG.warn(message);
                if (System.getenv("meteor.github.authorization") == null) {
                    LOG.info("Consider setting an authorization " +
                        "token with the 'meteor.github.authorization' environment variable.");
                    LOG.info("See: https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens");
                }
            }
            case Http.FORBIDDEN -> LOG.warn("Could not fetch updates: Rate-limited by GitHub.");
            case Http.NOT_FOUND -> LOG.warn("Could not fetch updates: GitHub repository '{}' not found.", repo.getOwnerName());
            case Http.SUCCESS -> {
                if (checkMode == CheckMode.Commit) {
                    Http.Request requestCommit = Http.get("https://api.github.com/repos/%s/commits/tag/%s".formatted(repo.getOwnerName(), res.body().tag_name));
                    requestCommit.exceptionHandler(e -> LOG.warn("Could not check for updates: " + e.getMessage()));
                    repo.authenticate(requestCommit);
                    HttpResponse<CommitResponse> resCommit = requestCommit.sendJsonResponse(CommitResponse.class);
                    if (!addon.getCommit().equals(resCommit.body().sha)) {
                        LOG.info("A new version of Bk Meteor Addon is available: " + res.body().tag_name);
                        MinecraftClient.getInstance().setScreen(new UpdateScreen(GuiThemes.get(), addon, res.body()));
                        return true;
                    } else {
                        LOG.info("You are using the latest version of Bk Meteor Addon");
                    }
                } else {
                    Version newVersion;
                    try {
                        newVersion = Version.parse(res.body().tag_name.substring(1));
                    } catch (VersionParsingException e) {
                        LOG.warn("Could not parse version: " + res.body().tag_name);
                        return false;
                    }
                    Version version = FabricLoader
                        .getInstance()
                        .getModContainer(BkMeteorAddon.MOD_ID)
                        .get()
                        .getMetadata()
                        .getVersion();
                    if (version.compareTo(newVersion) < 0) {
                        LOG.info("A new version of Bk Meteor Addon is available: " + res.body().tag_name);
                        MinecraftClient.getInstance().setScreen(new UpdateScreen(GuiThemes.get(), addon, res.body()));
                        return true;
                    } else {
                        LOG.info("You are using the latest version of Bk Meteor Addon");
                    }
                }

            }
        }
        return false;
    }
    private enum CheckMode {
        Commit,
        Version
    }
    public static class ReleaseResponse {
        public String tag_name;
        public String html_url;
        public String body;
        public String name;
        public String published_at;
        public List<Asset> assets;
    }
    public static class Asset {
        public String name;
        public String browser_download_url;
        public String content_type;
        public String size;
        public String download_count;
        public String updated_at;
    }
    private static class CommitResponse {
        public String sha;
    }
}
