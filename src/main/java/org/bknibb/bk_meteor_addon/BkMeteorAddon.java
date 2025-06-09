package org.bknibb.bk_meteor_addon;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.loader.api.FabricLoader;
import org.bknibb.bk_meteor_addon.commands.*;
import org.bknibb.bk_meteor_addon.modules.*;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class BkMeteorAddon extends MeteorAddon {
    public static final String MOD_ID = "bk-meteor-addon";
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("BkMeteorAddon");
    public static BkMeteorAddon INSTNACE;
    public static final boolean UPDATER_ENABLED = false;
    public static final File FOLDER = FabricLoader.getInstance().getGameDir().resolve(BkMeteorAddon.MOD_ID).toFile();

    @Override
    public void onInitialize() {
        INSTNACE = this;
        LOG.info("Initializing Bk Meteor Addon");

        Path modPath = FabricLoader.getInstance().getModContainer(MOD_ID).get().getOrigin().getPaths().getFirst();
        Path modDir = modPath.getParent();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modDir, MOD_ID+"-*.jar")) {
            for (Path path : stream) {
                if (!path.equals(modPath)) {
                    Files.deleteIfExists(path);
                    LOG.info("Deleted old mod file: " + path);
                }
            }
        } catch (Exception e) {
            LOG.error("Error reading mod directory: " + e.getMessage());
        }

        if (!FOLDER.exists()) {
            FOLDER.getParentFile().mkdirs();
            FOLDER.mkdir();
        }

        ConfigModifier.get();

        UpdatableResourcesManager.get();

        //UpdateSystem.checkForUpdates(this);

        // Modules
        Modules.get().add(new PlayerEsp());
        Modules.get().add(new PlayerTracers());
        Modules.get().add(new PlayerLoginLogoutNotifier());
        Modules.get().add(new MineplayRemoveOfflineRobloxPlayers());
        Modules.get().add(new MineplayBetterBreak());
        Modules.get().add(new NetworkLoginLogoutNotifier());
        Modules.get().add(new BadWordFinder());
        Modules.get().add(new VivecraftVanishDetect());

        // Commands
        Commands.add(new LocatePlayerCommand());
        Commands.add(new NetworkOnlineCommand());
        Commands.add(new VivecraftVanishedCommand());
        Commands.add(new MineplayBanPresetsCommand());
        Commands.add(new MineplayKickPresetsCommand());
        Commands.add(new MineplayMutePresetsCommand());
        Commands.add(new MineplayRobloxBanPresetsCommand());
        Commands.add(new MineplayRobloxWarnPresetsCommand());
        Commands.add(new MineplayWarnPresetsCommand());
        Commands.add(new MineplayIpCommand());
        Commands.add(new MineplayBlocksCommand());
        Commands.add(new BkUpdateResourcesCommand());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "org.bknibb.bk_meteor_addon";
    }

    @Override
    public String getCommit() {
        String commit = FabricLoader
                .getInstance()
                .getModContainer(MOD_ID)
                .get().getMetadata()
                .getCustomValue("github:sha")
                .getAsString();
        LOG.info("Bk Meteor Addon version: {}", commit);
        return commit.isEmpty() ? null : commit.trim();
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("Bknibb", "bk-meteor-addon");
    }
}
