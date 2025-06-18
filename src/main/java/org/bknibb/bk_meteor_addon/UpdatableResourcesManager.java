package org.bknibb.bk_meteor_addon;

import com.google.gson.reflect.TypeToken;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.utils.misc.Version;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class UpdatableResourcesManager {
    private static UpdatableResourcesManager INSTANCE;

    private static final Logger LOG = BkMeteorAddon.LOG;
    private static final GithubRepo REPO = BkMeteorAddon.INSTNACE.getRepo();

    private static final File FOLDER = new File(BkMeteorAddon.FOLDER, "updatable-resources");
    private static final File VERSION_FILE = new File(FOLDER, "version.txt");
    private static final String VERSION_URL = "https://raw.githubusercontent.com/%s/master/updatable-resources/version.txt".formatted(BkMeteorAddon.INSTNACE.getRepo().getOwnerName());

    private static Version VERSION = null;

    //private static final List<UpdateHandler> UPDATE_HANDLERS = new ArrayList<>();
    private static final List<Runnable> UPDATE_HANDLERS = new ArrayList<>();

    public static UpdatableResourcesManager get() {
        if (INSTANCE == null) INSTANCE = new UpdatableResourcesManager();
        return INSTANCE;
    }

    private UpdatableResourcesManager() {
        if (!FOLDER.exists()) {
            FOLDER.mkdir();
        }
        if (VERSION_FILE.exists()) {
            try {
                VERSION = new Version(Files.readString(VERSION_FILE.toPath()));
            } catch (Exception e) {
                throw new RuntimeException("Failed to read version from file: " + VERSION_FILE.getAbsolutePath(), e);
            }
        }
        CheckForUpdates(false);
    }

    private void DownloadFile(String url, String downloadPath) {
        Http.Request request = Http.get(url);
        request.exceptionHandler(e -> LOG.warn("Could not download file (Updatable Resources): " + e.getMessage()));
        REPO.authenticate(request);
        HttpResponse<InputStream> res = request.sendInputStreamResponse();
        if (res.statusCode() == Http.SUCCESS) {
            try (InputStream in = res.body(); OutputStream out = Files.newOutputStream(Path.of(downloadPath))) {
                in.transferTo(out);
            } catch (IOException e) {
                LOG.error("Failed to write file (Updatable Resources): " + downloadPath, e);
            }
        } else {
            LOG.warn("Failed to download file (Updatable Resources): " + url + " - Status code: " + res.statusCode());
        }
    }

    private void DownloadFolder(String path, String downloadPath) {
        Http.Request request = Http.get("https://api.github.com/repos/%s/contents/%s".formatted(REPO.getOwnerName(), path));
        request.exceptionHandler(e -> LOG.warn("Could not download folder (Updatable Resources): " + e.getMessage()));
        REPO.authenticate(request);
        HttpResponse<List<ContentsResponse>> res = request.sendJsonResponse(new TypeToken<List<ContentsResponse>>(){}.getType());
        if (res.statusCode() == Http.SUCCESS) {
            res.body().forEach(content -> {
                if (content.type.equals("file")) {
                    DownloadFile(content.download_url, downloadPath + "/" + content.name);
                } else if (content.type.equals("dir")) {
                    DownloadFolder(path + "/" + content.name, downloadPath + "/" + content.name);
                }
            });
        } else {
            LOG.warn("Failed to download folder (Updatable Resources): " + path + " - Status code: " + res.statusCode());
        }
    }

    private void DoUpdate(Version newVersion) {
        try {
            FileUtils.cleanDirectory(FOLDER);
        } catch (IOException e) {
            LOG.warn("Failed to clean up folder (Updatable Resources): " + FOLDER.getAbsolutePath(), e);
        }
        DownloadFolder("updatable-resources", FOLDER.getAbsolutePath());
        VERSION = newVersion;
        try {
            Files.writeString(VERSION_FILE.toPath(), VERSION.toString());
        } catch (Exception e) {
            LOG.error("Failed to write version to file (Updatable Resources): " + VERSION_FILE.getAbsolutePath(), e);
        }
//        for (UpdateHandler handler : UPDATE_HANDLERS) {
//            handler.run();
//        }
        List<Runnable> handlers = new ArrayList<>(UPDATE_HANDLERS);
        UPDATE_HANDLERS.clear();
        for (Runnable handler : handlers) {
            handler.run();
        }
    }

    public boolean CheckForUpdates(boolean force) {
        Http.Request request = Http.get(VERSION_URL);
        request.exceptionHandler(e -> LOG.warn("Could not check for updates (Updatable Resources): " + e.getMessage()));
        REPO.authenticate(request);
        HttpResponse<String> res = request.sendStringResponse();
        switch (res.statusCode()) {
            case Http.UNAUTHORIZED -> {
                String message = "Invalid authentication token for repository '%s'".formatted(REPO.getOwnerName());
                MinecraftClient.getInstance().getToastManager().add(new MeteorToast(Items.BARRIER, "GitHub: Unauthorized", message));
                LOG.warn(message);
                if (System.getenv("meteor.github.authorization") == null) {
                    LOG.info("Consider setting an authorization " +
                        "token with the 'meteor.github.authorization' environment variable.");
                    LOG.info("See: https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens");
                }
            }
            case Http.FORBIDDEN -> LOG.warn("Could not fetch updates (Updatable Resources): Rate-limited by GitHub.");
            case Http.NOT_FOUND -> LOG.warn("Could not fetch updates (Updatable Resources): GitHub repository '{}' not found.", REPO.getOwnerName());
            case Http.SUCCESS -> {
                Version latestVersion = new Version(res.body().strip());
                if (VERSION == null || force || latestVersion.isHigherThan(VERSION)) {
                    DoUpdate(latestVersion);
                    return true;
                }
            }
        }
        return false;
    }
    public InputStream getResource(String path, /*Consumer<InputStream> onUpdate*/Runnable onUpdate) throws IOException {
        //UPDATE_HANDLERS.add(new UpdateHandler(onUpdate, path));
        UPDATE_HANDLERS.add(onUpdate);
        Path resourcePath = FOLDER.toPath().resolve(path);
        if (!Files.exists(resourcePath)) {
            LOG.warn("(Updatable Resources): Resource not found: " + resourcePath);
            ChatUtils.warningPrefix("BkMeteorAddon-UpdatableResourcesManager", "Resource not found, please try run meteor command \"bk-update-resources force\", Resource: {}", path);
        }
        return Files.newInputStream(resourcePath);
    }
    public static class ContentsResponse {
        public String name;
        public String download_url;
        public String type;
    }
//    public static class UpdateHandler {
//        private final Consumer<InputStream> handler;
//
//        private final String path;
//
//        public UpdateHandler(Consumer<InputStream> handler, String path) {
//            this.handler = handler;
//            this.path = path;
//        }
//
//        public void run() {
//            try (InputStream stream = Files.newInputStream(FOLDER.toPath().resolve(path))) {
//                handler.accept(stream);
//            } catch (IOException e) {
//                LOG.error("Failed to handle update (Updatable Resources): " + e.getMessage(), e);
//            }
//        }
//    }
}
