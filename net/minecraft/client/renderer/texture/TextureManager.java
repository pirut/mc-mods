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
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class TextureManager implements PreparableReloadListener, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Identifier INTENTIONAL_MISSING_TEXTURE = Identifier.withDefaultNamespace("");
   private final Map<Identifier, AbstractTexture> byPath = new HashMap<>();
   private final Set<TickableTexture> tickableTextures = new HashSet<>();
   private final ResourceManager resourceManager;

   public TextureManager(ResourceManager p_118474_) {
      this.resourceManager = p_118474_;
      NativeImage nativeimage = MissingTextureAtlasSprite.generateMissingImage();
      this.register(MissingTextureAtlasSprite.getLocation(), new DynamicTexture(() -> "(intentionally-)Missing Texture", nativeimage));
   }

   public void registerAndLoad(Identifier p_467786_, ReloadableTexture p_389421_) {
      try {
         p_389421_.apply(this.loadContentsSafe(p_467786_, p_389421_));
      } catch (Throwable var6) {
         CrashReport crashreport = CrashReport.forThrowable(var6, "Uploading texture");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Uploaded texture");
         crashreportcategory.setDetail("Resource location", p_389421_.resourceId());
         crashreportcategory.setDetail("Texture id", p_467786_);
         throw new ReportedException(crashreport);
      }

      this.register(p_467786_, p_389421_);
   }

   private TextureContents loadContentsSafe(Identifier p_469788_, ReloadableTexture p_390396_) {
      try {
         return loadContents(this.resourceManager, p_469788_, p_390396_);
      } catch (Exception var4) {
         LOGGER.error("Failed to load texture {} into slot {}", new Object[]{p_390396_.resourceId(), p_469788_, var4});
         return TextureContents.createMissing();
      }
   }

   public void registerForNextReload(Identifier p_469423_) {
      this.register(p_469423_, new SimpleTexture(p_469423_));
   }

   public void register(Identifier p_469853_, AbstractTexture p_118497_) {
      AbstractTexture abstracttexture = this.byPath.put(p_469853_, p_118497_);
      if (abstracttexture != p_118497_) {
         if (abstracttexture != null) {
            this.safeClose(p_469853_, abstracttexture);
         }

         if (p_118497_ instanceof TickableTexture tickabletexture) {
            this.tickableTextures.add(tickabletexture);
         }
      }
   }

   private void safeClose(Identifier p_467886_, AbstractTexture p_118510_) {
      this.tickableTextures.remove(p_118510_);

      try {
         p_118510_.close();
      } catch (Exception var4) {
         LOGGER.warn("Failed to close texture {}", p_467886_, var4);
      }
   }

   public AbstractTexture getTexture(Identifier p_469657_) {
      AbstractTexture abstracttexture = this.byPath.get(p_469657_);
      if (abstracttexture != null) {
         return abstracttexture;
      } else {
         SimpleTexture simpletexture = new SimpleTexture(p_469657_);
         this.registerAndLoad(p_469657_, simpletexture);
         return simpletexture;
      }
   }

   public void tick() {
      for (TickableTexture tickabletexture : this.tickableTextures) {
         tickabletexture.tick();
      }
   }

   public void release(Identifier p_469041_) {
      AbstractTexture abstracttexture = this.byPath.remove(p_469041_);
      if (abstracttexture != null) {
         this.safeClose(p_469041_, abstracttexture);
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
      PreparableReloadListener.SharedState p_435296_, Executor p_118480_, PreparableReloadListener.PreparationBarrier p_118476_, Executor p_118481_
   ) {
      ResourceManager resourcemanager = p_435296_.resourceManager();
      List<TextureManager.PendingReload> list = new ArrayList<>();
      this.byPath.forEach((p_465732_, p_465733_) -> {
         if (p_465733_ instanceof ReloadableTexture reloadabletexture) {
            list.add(scheduleLoad(resourcemanager, p_465732_, reloadabletexture, p_118480_));
         }
      });
      return CompletableFuture.allOf(list.stream().map(TextureManager.PendingReload::newContents).toArray(CompletableFuture[]::new))
         .thenCompose(p_118476_::wait)
         .thenAcceptAsync(p_389351_ -> {
            AddRealmPopupScreen.updateCarouselImages(this.resourceManager);

            for (TextureManager.PendingReload texturemanager$pendingreload : list) {
               texturemanager$pendingreload.texture.apply(texturemanager$pendingreload.newContents.join());
            }
         }, p_118481_);
   }

   public void dumpAllSheets(Path p_276129_) {
      try {
         Files.createDirectories(p_276129_);
      } catch (IOException var3) {
         LOGGER.error("Failed to create directory {}", p_276129_, var3);
         return;
      }

      this.byPath.forEach((p_465727_, p_465728_) -> {
         if (p_465728_ instanceof Dumpable dumpable) {
            try {
               dumpable.dumpContents(p_465727_, p_276129_);
            } catch (Exception var5) {
               LOGGER.error("Failed to dump texture {}", p_465727_, var5);
            }
         }
      });
   }

   private static TextureContents loadContents(ResourceManager p_389428_, Identifier p_467223_, ReloadableTexture p_389480_) throws IOException {
      try {
         return p_389480_.loadContents(p_389428_);
      } catch (FileNotFoundException var4) {
         if (p_467223_ != INTENTIONAL_MISSING_TEXTURE) {
            LOGGER.warn("Missing resource {} referenced from {}", p_389480_.resourceId(), p_467223_);
         }

         return TextureContents.createMissing();
      }
   }

   private static TextureManager.PendingReload scheduleLoad(ResourceManager p_389696_, Identifier p_469215_, ReloadableTexture p_389691_, Executor p_389430_) {
      return new TextureManager.PendingReload(p_389691_, CompletableFuture.supplyAsync(() -> {
         try {
            return loadContents(p_389696_, p_469215_, p_389691_);
         } catch (IOException var4) {
            throw new UncheckedIOException(var4);
         }
      }, p_389430_));
   }

   @OnlyIn(Dist.CLIENT)
   record PendingReload(ReloadableTexture texture, CompletableFuture<TextureContents> newContents) {
   }
}
