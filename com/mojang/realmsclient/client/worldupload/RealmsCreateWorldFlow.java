package com.mojang.realmsclient.client.worldupload;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSetting;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.util.task.RealmCreationTask;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.RegistryLayer;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class RealmsCreateWorldFlow {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static void createWorld(
      Minecraft minecraft, Screen lastScreen, Screen resetWorldScreen, int slot, RealmsServer server, @Nullable RealmCreationTask realmCreationTask
   ) {
      CreateWorldScreen.openFresh(
         minecraft,
         () -> minecraft.setScreen(lastScreen),
         (createWorldScreen, layeredRegistryAccess, primaryLevelData, path) -> {
            Path path2;
            try {
               path2 = createTemporaryWorldFolder(layeredRegistryAccess, primaryLevelData, path);
            } catch (IOException var14) {
               LOGGER.warn("Failed to create temporary world folder.");
               minecraft.setScreen(new RealmsGenericErrorScreen(Component.translatable("mco.create.world.failed"), resetWorldScreen));
               return true;
            }

            RealmsWorldOptions realmsWorldOptions = RealmsWorldOptions.createFromSettings(
               primaryLevelData.getLevelSettings(), SharedConstants.getCurrentVersion().name()
            );
            RealmsSlot realmsSlot = new RealmsSlot(
               slot, realmsWorldOptions, List.of(RealmsSetting.hardcoreSetting(primaryLevelData.getLevelSettings().hardcore()))
            );
            RealmsWorldUpload realmsWorldUpload = new RealmsWorldUpload(
               path2, realmsSlot, minecraft.getUser(), server.id, RealmsWorldUploadStatusTracker.noOp()
            );
            minecraft.setScreenAndShow(
               new AlertScreen(
                  realmsWorldUpload::cancel, Component.translatable("mco.create.world.reset.title"), Component.empty(), CommonComponents.GUI_CANCEL, false
               )
            );
            if (realmCreationTask != null) {
               realmCreationTask.run();
            }

            realmsWorldUpload.packAndUpload().handleAsync((object, throwable) -> {
               if (throwable != null) {
                  if (throwable instanceof CompletionException completionException) {
                     throwable = completionException.getCause();
                  }

                  if (throwable instanceof RealmsUploadCanceledException) {
                     minecraft.setScreenAndShow(resetWorldScreen);
                  } else {
                     if (throwable instanceof RealmsUploadFailedException realmsUploadFailedException) {
                        LOGGER.warn("Failed to create realms world {}", realmsUploadFailedException.getStatusMessage());
                     } else {
                        LOGGER.warn("Failed to create realms world {}", throwable.getMessage());
                     }

                     minecraft.setScreenAndShow(new RealmsGenericErrorScreen(Component.translatable("mco.create.world.failed"), resetWorldScreen));
                  }
               } else {
                  if (lastScreen instanceof RealmsConfigureWorldScreen realmsConfigureWorldScreen) {
                     realmsConfigureWorldScreen.fetchServerData(server.id);
                  }

                  if (realmCreationTask != null) {
                     RealmsMainScreen.play(server, lastScreen, true);
                  } else {
                     minecraft.setScreenAndShow(lastScreen);
                  }

                  RealmsMainScreen.refreshServerList();
               }

               return null;
            }, minecraft);
            return true;
         }
      );
   }

   private static Path createTemporaryWorldFolder(
      LayeredRegistryAccess<RegistryLayer> registryAccess, PrimaryLevelData levelData, @Nullable Path tempDatapackDir
   ) throws IOException {
      Path path = Files.createTempDirectory("minecraft_realms_world_upload");
      if (tempDatapackDir != null) {
         Files.move(tempDatapackDir, path.resolve("datapacks"));
      }

      CompoundTag compoundTag = levelData.createTag(registryAccess.compositeAccess(), null);
      CompoundTag compoundTag2 = new CompoundTag();
      compoundTag2.put("Data", compoundTag);
      Path path2 = Files.createFile(path.resolve("level.dat"));
      NbtIo.writeCompressed(compoundTag2, path2);
      return path;
   }
}
