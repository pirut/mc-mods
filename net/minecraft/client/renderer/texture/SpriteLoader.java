package net.minecraft.client.renderer.texture;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class SpriteLoader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Identifier location;
   private final int maxSupportedTextureSize;

   public SpriteLoader(Identifier location, int maxSupportedTextureSize) {
      this.location = location;
      this.maxSupportedTextureSize = maxSupportedTextureSize;
   }

   public static SpriteLoader create(TextureAtlas atlas) {
      return new SpriteLoader(atlas.location(), atlas.maxSupportedTextureSize());
   }

   private SpriteLoader.Preparations stitch(List<SpriteContents> contents, int mipLevel, Executor executor) {
      SpriteLoader.Preparations var19;
      try (Zone zone = Profiler.get().zone(() -> "stitch " + this.location)) {
         int i = this.maxSupportedTextureSize;
         int j = Integer.MAX_VALUE;
         int k = 1 << mipLevel;

         for (SpriteContents spriteContents : contents) {
            j = Math.min(j, Math.min(spriteContents.width(), spriteContents.height()));
            int l = Math.min(Integer.lowestOneBit(spriteContents.width()), Integer.lowestOneBit(spriteContents.height()));
            if (l < k) {
               LOGGER.warn(
                  "Texture {} with size {}x{} limits mip level from {} to {}",
                  new Object[]{spriteContents.name(), spriteContents.width(), spriteContents.height(), Mth.log2(k), Mth.log2(l)}
               );
               k = l;
            }
         }

         int m = Math.min(j, k);
         int n = Mth.log2(m);
         int l;
         if (n < mipLevel) {
            LOGGER.warn("{}: dropping miplevel from {} to {}, because of minimum power of two: {}", new Object[]{this.location, mipLevel, n, m});
            l = n;
         } else {
            l = mipLevel;
         }

         Options options = Minecraft.getInstance().options;
         int o = l != 0 && options.textureFiltering().get() == TextureFilteringMethod.ANISOTROPIC ? options.maxAnisotropyBit().get() : 0;
         Stitcher<SpriteContents> stitcher = new Stitcher<>(i, i, l, o);

         for (SpriteContents spriteContents2 : contents) {
            stitcher.registerSprite(spriteContents2);
         }

         try {
            stitcher.stitch();
         } catch (StitcherException var21) {
            CrashReport crashReport = CrashReport.forThrowable(var21, "Stitching");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Stitcher");
            crashReportCategory.setDetail(
               "Sprites",
               var21.getAllSprites()
                  .stream()
                  .map(entry -> String.format(Locale.ROOT, "%s[%dx%d]", entry.name(), entry.width(), entry.height()))
                  .collect(Collectors.joining(","))
            );
            crashReportCategory.setDetail("Max Texture Size", i);
            throw new ReportedException(crashReport);
         }

         int p = stitcher.getWidth();
         int q = stitcher.getHeight();
         Map<Identifier, TextureAtlasSprite> map = this.getStitchedSprites(stitcher, p, q);
         TextureAtlasSprite textureAtlasSprite = map.get(MissingTextureAtlasSprite.getLocation());
         CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(
            () -> map.values().forEach(textureAtlasSpritex -> textureAtlasSpritex.contents().increaseMipLevel(l)), executor
         );
         var19 = new SpriteLoader.Preparations(p, q, l, textureAtlasSprite, map, completableFuture);
      }

      return var19;
   }

   private static CompletableFuture<List<SpriteContents>> runSpriteSuppliers(
      SpriteResourceLoader spriteResourceLoader, List<SpriteSource.Loader> factories, Executor executor
   ) {
      List<CompletableFuture<SpriteContents>> list = factories.stream()
         .map(loader -> CompletableFuture.supplyAsync(() -> loader.get(spriteResourceLoader), executor))
         .toList();
      return Util.sequence(list).thenApply(listx -> listx.stream().filter(Objects::nonNull).toList());
   }

   public CompletableFuture<SpriteLoader.Preparations> loadAndStitch(
      ResourceManager resourceManager, Identifier location, int mipLevel, Executor executor, Set<MetadataSectionType<?>> sectionTypes
   ) {
      SpriteResourceLoader spriteResourceLoader = SpriteResourceLoader.create(sectionTypes);
      return CompletableFuture.<List<SpriteSource.Loader>>supplyAsync(() -> SpriteSourceList.load(resourceManager, location).list(resourceManager), executor)
         .thenCompose(list -> runSpriteSuppliers(spriteResourceLoader, (List<SpriteSource.Loader>)list, executor))
         .thenApply(list -> this.stitch((List<SpriteContents>)list, mipLevel, executor));
   }

   private Map<Identifier, TextureAtlasSprite> getStitchedSprites(Stitcher<SpriteContents> stitcher, int x, int y) {
      Map<Identifier, TextureAtlasSprite> map = new HashMap<>();
      stitcher.gatherSprites((spriteContents, k, l, m) -> map.put(spriteContents.name(), new TextureAtlasSprite(this.location, spriteContents, x, y, k, l, m)));
      return map;
   }

   @Environment(EnvType.CLIENT)
   public record Preparations(
      int width, int height, int mipLevel, TextureAtlasSprite missing, Map<Identifier, TextureAtlasSprite> regions, CompletableFuture<Void> readyForUpload
   ) {
      @Nullable
      public TextureAtlasSprite getSprite(Identifier name) {
         return this.regions.get(name);
      }
   }
}
