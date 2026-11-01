package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.gui.screens.AddRealmPopupScreen;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class TextureManager implements PreparableReloadListener, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Identifier INTENTIONAL_MISSING_TEXTURE = Identifier.withDefaultNamespace("");
   private final Map<Identifier, AbstractTexture> byPath = new HashMap<>();
   private final Set<TickableTexture> tickableTextures = new HashSet<>();
   private final ResourceManager resourceManager;

   public TextureManager(ResourceManager resourceManager) {
      this.resourceManager = resourceManager;
      NativeImage nativeImage = MissingTextureAtlasSprite.generateMissingImage();
      this.register(MissingTextureAtlasSprite.getLocation(), new DynamicTexture(() -> "(intentionally-)Missing Texture", nativeImage));
   }

   public void registerAndLoad(Identifier textureId, ReloadableTexture texture) {
      try {
         texture.apply(this.loadContentsSafe(textureId, texture));
      } catch (Throwable var6) {
         CrashReport crashReport = CrashReport.forThrowable(var6, "Uploading texture");
         CrashReportCategory crashReportCategory = crashReport.addCategory("Uploaded texture");
         crashReportCategory.setDetail("Resource location", texture.resourceId());
         crashReportCategory.setDetail("Texture id", textureId);
         throw new ReportedException(crashReport);
      }

      this.register(textureId, texture);
   }

   private TextureContents loadContentsSafe(Identifier textureId, ReloadableTexture texture) {
      try {
         return loadContents(this.resourceManager, textureId, texture);
      } catch (Exception var4) {
         LOGGER.error("Failed to load texture {} into slot {}", new Object[]{texture.resourceId(), textureId, var4});
         return TextureContents.createMissing();
      }
   }

   public void registerForNextReload(Identifier textureId) {
      this.register(textureId, new SimpleTexture(textureId));
   }

   public void register(Identifier path, AbstractTexture texture) {
      AbstractTexture abstractTexture = this.byPath.put(path, texture);
      if (abstractTexture != texture) {
         if (abstractTexture != null) {
            this.safeClose(path, abstractTexture);
         }

         if (texture instanceof TickableTexture tickableTexture) {
            this.tickableTextures.add(tickableTexture);
         }
      }
   }

   private void safeClose(Identifier path, AbstractTexture texture) {
      this.tickableTextures.remove(texture);

      try {
         texture.close();
      } catch (Exception var4) {
         LOGGER.warn("Failed to close texture {}", path, var4);
      }
   }

   public AbstractTexture getTexture(Identifier path) {
      AbstractTexture abstractTexture = this.byPath.get(path);
      if (abstractTexture != null) {
         return abstractTexture;
      } else {
         SimpleTexture simpleTexture = new SimpleTexture(path);
         this.registerAndLoad(path, simpleTexture);
         return simpleTexture;
      }
   }

   public void tick() {
      for (TickableTexture tickableTexture : this.tickableTextures) {
         tickableTexture.tick();
      }
   }

   public void release(Identifier path) {
      AbstractTexture abstractTexture = this.byPath.remove(path);
      if (abstractTexture != null) {
         this.safeClose(path, abstractTexture);
      }
   }

   @Override
   public void close() {
      this.byPath.forEach(this::safeClose);
      this.byPath.clear();
      this.tickableTextures.clear();
   }

   @Override
   public CompletableFuture<Void> reload(
      PreparableReloadListener.SharedState sharedState, Executor executor, PreparableReloadListener.PreparationBarrier preparationBarrier, Executor executor2
   ) {
      ResourceManager resourceManager = sharedState.resourceManager();
      List<TextureManager.PendingReload> list = new ArrayList<>();
      this.byPath.forEach((identifier, abstractTexture) -> {
         if (abstractTexture instanceof ReloadableTexture reloadableTexture) {
            list.add(scheduleLoad(resourceManager, identifier, reloadableTexture, executor));
         }
      });
      return CompletableFuture.allOf(list.stream().map(TextureManager.PendingReload::newContents).toArray(CompletableFuture[]::new))
         .thenCompose(preparationBarrier::wait)
         .thenAcceptAsync(void_ -> {
            AddRealmPopupScreen.updateCarouselImages(this.resourceManager);

            for (TextureManager.PendingReload pendingReload : list) {
               pendingReload.texture.apply(pendingReload.newContents.join());
            }
         }, executor2);
   }

   public void dumpAllSheets(Path path) {
      try {
         Files.createDirectories(path);
      } catch (IOException var3) {
         LOGGER.error("Failed to create directory {}", path, var3);
         return;
      }

      this.byPath.forEach((identifier, abstractTexture) -> {
         if (abstractTexture instanceof Dumpable dumpable) {
            try {
               dumpable.dumpContents(identifier, path);
            } catch (Exception var5) {
               LOGGER.error("Failed to dump texture {}", identifier, var5);
            }
         }
      });
   }

   private static TextureContents loadContents(ResourceManager resourceManager, Identifier textureId, ReloadableTexture texture) throws IOException {
      try {
         return texture.loadContents(resourceManager);
      } catch (FileNotFoundException var4) {
         if (textureId != INTENTIONAL_MISSING_TEXTURE) {
            LOGGER.warn("Missing resource {} referenced from {}", texture.resourceId(), textureId);
         }

         return TextureContents.createMissing();
      }
   }

   private static TextureManager.PendingReload scheduleLoad(ResourceManager resourceManager, Identifier textureId, ReloadableTexture texture, Executor executor) {
      return new TextureManager.PendingReload(texture, CompletableFuture.supplyAsync(() -> {
         try {
            return loadContents(resourceManager, textureId, texture);
         } catch (IOException var4) {
            throw new UncheckedIOException(var4);
         }
      }, executor));
   }

   @Environment(EnvType.CLIENT)
   record PendingReload(ReloadableTexture texture, CompletableFuture<TextureContents> newContents) {
   }
}
