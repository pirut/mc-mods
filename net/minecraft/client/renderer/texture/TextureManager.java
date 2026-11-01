package net.minecraft.client.renderer.texture;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.gui.screens.AddRealmPopupScreen;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class TextureManager implements PreparableReloadListener, Tickable, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final ResourceLocation INTENTIONAL_MISSING_TEXTURE = ResourceLocation.withDefaultNamespace("");
   private final Map<ResourceLocation, AbstractTexture> byPath = Maps.newHashMap();
   private final Set<Tickable> tickableTextures = Sets.newHashSet();
   private final Map<String, Integer> prefixRegister = Maps.newHashMap();
   private final ResourceManager resourceManager;

   public TextureManager(ResourceManager p_118474_) {
      this.resourceManager = p_118474_;
   }

   public void bindForSetup(ResourceLocation p_174785_) {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> this._bind(p_174785_));
      } else {
         this._bind(p_174785_);
      }
   }

   private void _bind(ResourceLocation p_118520_) {
      AbstractTexture $$1 = this.byPath.get(p_118520_);
      if ($$1 == null) {
         $$1 = new SimpleTexture(p_118520_);
         this.register(p_118520_, $$1);
      }

      $$1.bind();
   }

   public void register(ResourceLocation p_118496_, AbstractTexture p_118497_) {
      p_118497_ = this.loadTexture(p_118496_, p_118497_);
      AbstractTexture $$2 = this.byPath.put(p_118496_, p_118497_);
      if ($$2 != p_118497_) {
         if ($$2 != null && $$2 != MissingTextureAtlasSprite.getTexture()) {
            this.safeClose(p_118496_, $$2);
         }

         if (p_118497_ instanceof Tickable) {
            this.tickableTextures.add((Tickable)p_118497_);
         }
      }
   }

   private void safeClose(ResourceLocation p_118509_, AbstractTexture p_118510_) {
      if (p_118510_ != MissingTextureAtlasSprite.getTexture()) {
         this.tickableTextures.remove(p_118510_);

         try {
            p_118510_.close();
         } catch (Exception var4) {
            LOGGER.warn("Failed to close texture {}", p_118509_, var4);
         }
      }

      p_118510_.releaseId();
   }

   private AbstractTexture loadTexture(ResourceLocation p_118516_, AbstractTexture p_118517_) {
      try {
         p_118517_.load(this.resourceManager);
         return p_118517_;
      } catch (IOException var6) {
         if (p_118516_ != INTENTIONAL_MISSING_TEXTURE) {
            LOGGER.warn("Failed to load texture: {}", p_118516_, var6);
         }

         return MissingTextureAtlasSprite.getTexture();
      } catch (Throwable var7) {
         CrashReport $$4 = CrashReport.forThrowable(var7, "Registering texture");
         CrashReportCategory $$5 = $$4.addCategory("Resource location being registered");
         $$5.setDetail("Resource location", p_118516_);
         $$5.setDetail("Texture object class", () -> p_118517_.getClass().getName());
         throw new ReportedException($$4);
      }
   }

   public AbstractTexture getTexture(ResourceLocation p_118507_) {
      AbstractTexture $$1 = this.byPath.get(p_118507_);
      if ($$1 == null) {
         $$1 = new SimpleTexture(p_118507_);
         this.register(p_118507_, $$1);
      }

      return $$1;
   }

   public AbstractTexture getTexture(ResourceLocation p_174787_, AbstractTexture p_174788_) {
      return this.byPath.getOrDefault(p_174787_, p_174788_);
   }

   public ResourceLocation register(String p_118491_, DynamicTexture p_118492_) {
      Integer $$2 = this.prefixRegister.get(p_118491_);
      if ($$2 == null) {
         $$2 = 1;
      } else {
         $$2 = $$2 + 1;
      }

      this.prefixRegister.put(p_118491_, $$2);
      ResourceLocation $$3 = ResourceLocation.withDefaultNamespace(String.format(Locale.ROOT, "dynamic/%s_%d", p_118491_, $$2));
      this.register($$3, p_118492_);
      return $$3;
   }

   public CompletableFuture<Void> preload(ResourceLocation p_118502_, Executor p_118503_) {
      if (!this.byPath.containsKey(p_118502_)) {
         PreloadedTexture $$2 = new PreloadedTexture(this.resourceManager, p_118502_, p_118503_);
         this.byPath.put(p_118502_, $$2);
         return $$2.getFuture().thenRunAsync(() -> this.register(p_118502_, $$2), TextureManager::execute);
      } else {
         return CompletableFuture.completedFuture(null);
      }
   }

   private static void execute(Runnable p_118489_) {
      Minecraft.getInstance().execute(() -> RenderSystem.recordRenderCall(p_118489_::run));
   }

   @Override
   public void tick() {
      for (Tickable $$0 : this.tickableTextures) {
         $$0.tick();
      }
   }

   public void release(ResourceLocation p_118514_) {
      AbstractTexture $$1 = this.byPath.remove(p_118514_);
      if ($$1 != null) {
         this.safeClose(p_118514_, $$1);
      }
   }

   @Override
   public void close() {
      this.byPath.forEach(this::safeClose);
      this.byPath.clear();
      this.tickableTextures.clear();
      this.prefixRegister.clear();
   }

   @Override
   public CompletableFuture<Void> reload(
      PreparableReloadListener.PreparationBarrier p_118476_,
      ResourceManager p_118477_,
      ProfilerFiller p_118478_,
      ProfilerFiller p_118479_,
      Executor p_118480_,
      Executor p_118481_
   ) {
      CompletableFuture<Void> $$6 = new CompletableFuture<>();
      TitleScreen.preloadResources(this, p_118480_).thenCompose(p_118476_::wait).thenAcceptAsync(p_247950_ -> {
         MissingTextureAtlasSprite.getTexture();
         AddRealmPopupScreen.updateCarouselImages(this.resourceManager);
         Iterator<Entry<ResourceLocation, AbstractTexture>> $$4 = this.byPath.entrySet().iterator();

         while ($$4.hasNext()) {
            Entry<ResourceLocation, AbstractTexture> $$5 = $$4.next();
            ResourceLocation $$6x = $$5.getKey();
            AbstractTexture $$7 = $$5.getValue();
            if ($$7 == MissingTextureAtlasSprite.getTexture() && !$$6x.equals(MissingTextureAtlasSprite.getLocation())) {
               $$4.remove();
            } else {
               $$7.reset(this, p_118477_, $$6x, p_118481_);
            }
         }

         Minecraft.getInstance().tell(() -> $$6.complete(null));
      }, p_118505_ -> RenderSystem.recordRenderCall(p_118505_::run));
      return $$6;
   }

   public void dumpAllSheets(Path p_276129_) {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> this._dumpAllSheets(p_276129_));
      } else {
         this._dumpAllSheets(p_276129_);
      }
   }

   private void _dumpAllSheets(Path p_276128_) {
      try {
         Files.createDirectories(p_276128_);
      } catch (IOException var3) {
         LOGGER.error("Failed to create directory {}", p_276128_, var3);
         return;
      }

      this.byPath.forEach((p_276101_, p_276102_) -> {
         if (p_276102_ instanceof Dumpable $$3) {
            try {
               $$3.dumpContents(p_276101_, p_276128_);
            } catch (IOException var5) {
               LOGGER.error("Failed to dump texture {}", p_276101_, var5);
            }
         }
      });
   }
}
