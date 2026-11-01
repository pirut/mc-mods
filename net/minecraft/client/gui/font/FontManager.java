package net.minecraft.client.gui.font;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.DependencySorter;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class FontManager implements PreparableReloadListener, AutoCloseable {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final String FONTS_PATH = "fonts.json";
   public static final Identifier MISSING_FONT = Identifier.withDefaultNamespace("missing");
   private static final FileToIdConverter FONT_DEFINITIONS = FileToIdConverter.json("font");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   final FontSet missingFontSet;
   private final List<GlyphProvider> providersToClose = new ArrayList<>();
   private final Map<Identifier, FontSet> fontSets = new HashMap<>();
   private final TextureManager textureManager;
   private final FontManager.CachedFontProvider anyGlyphs = new FontManager.CachedFontProvider(false);
   private final FontManager.CachedFontProvider nonFishyGlyphs = new FontManager.CachedFontProvider(true);
   private final AtlasManager atlasManager;
   private final Map<Identifier, AtlasGlyphProvider> atlasProviders = new HashMap<>();
   final PlayerGlyphProvider playerProvider;

   public FontManager(TextureManager p_95005_, AtlasManager p_436601_, PlayerSkinRenderCache p_443263_) {
      this.textureManager = p_95005_;
      this.atlasManager = p_436601_;
      this.missingFontSet = this.createFontSet(MISSING_FONT, List.of(createFallbackProvider()), Set.of());
      this.playerProvider = new PlayerGlyphProvider(p_443263_);
   }

   private FontSet createFontSet(Identifier p_467177_, List<GlyphProvider.Conditional> p_433289_, Set<FontOption> p_434588_) {
      GlyphStitcher glyphstitcher = new GlyphStitcher(this.textureManager, p_467177_);
      FontSet fontset = new FontSet(glyphstitcher);
      fontset.reload(p_433289_, p_434588_);
      return fontset;
   }

   private static GlyphProvider.Conditional createFallbackProvider() {
      return new GlyphProvider.Conditional(new AllMissingGlyphProvider(), FontOption.Filter.ALWAYS_PASS);
   }

   @Override
   public CompletableFuture<Void> reload(
      PreparableReloadListener.SharedState p_435059_, Executor p_284975_, PreparableReloadListener.PreparationBarrier p_285160_, Executor p_285218_
   ) {
      return this.prepare(p_435059_.resourceManager(), p_284975_)
         .thenCompose(p_285160_::wait)
         .thenAcceptAsync(p_372503_ -> this.apply(p_372503_, Profiler.get()), p_285218_);
   }

   private CompletableFuture<FontManager.Preparation> prepare(ResourceManager p_285252_, Executor p_284969_) {
      List<CompletableFuture<FontManager.UnresolvedBuilderBundle>> list = new ArrayList<>();

      for (Entry<Identifier, List<Resource>> entry : FONT_DEFINITIONS.listMatchingResourceStacks(p_285252_).entrySet()) {
         Identifier identifier = FONT_DEFINITIONS.fileToId(entry.getKey());
         list.add(CompletableFuture.supplyAsync(() -> {
            List<Pair<FontManager.BuilderId, GlyphProviderDefinition.Conditional>> list1 = loadResourceStack(entry.getValue(), identifier);
            FontManager.UnresolvedBuilderBundle fontmanager$unresolvedbuilderbundle = new FontManager.UnresolvedBuilderBundle(identifier);

            for (Pair<FontManager.BuilderId, GlyphProviderDefinition.Conditional> pair : list1) {
               FontManager.BuilderId fontmanager$builderid = (FontManager.BuilderId)pair.getFirst();
               FontOption.Filter fontoption$filter = ((GlyphProviderDefinition.Conditional)pair.getSecond()).filter();
               ((GlyphProviderDefinition.Conditional)pair.getSecond()).definition().unpack().ifLeft(p_325476_ -> {
                  CompletableFuture<Optional<GlyphProvider>> completablefuture = this.safeLoad(fontmanager$builderid, p_325476_, p_285252_, p_284969_);
                  fontmanager$unresolvedbuilderbundle.add(fontmanager$builderid, fontoption$filter, completablefuture);
               }).ifRight(p_325470_ -> fontmanager$unresolvedbuilderbundle.add(fontmanager$builderid, fontoption$filter, p_325470_));
            }

            return fontmanager$unresolvedbuilderbundle;
         }, p_284969_));
      }

      return Util.sequence(list)
         .thenCompose(
            p_465473_ -> {
               List<CompletableFuture<Optional<GlyphProvider>>> list1 = p_465473_.stream()
                  .flatMap(FontManager.UnresolvedBuilderBundle::listBuilders)
                  .collect(Util.toMutableList());
               GlyphProvider.Conditional glyphprovider$conditional = createFallbackProvider();
               list1.add(CompletableFuture.completedFuture(Optional.of(glyphprovider$conditional.provider())));
               return Util.sequence(list1)
                  .thenCompose(
                     p_284618_ -> {
                        Map<Identifier, List<GlyphProvider.Conditional>> map = this.resolveProviders(p_465473_);
                        CompletableFuture<?>[] completablefuture = map.values()
                           .stream()
                           .map(p_284585_ -> CompletableFuture.runAsync(() -> this.finalizeProviderLoading(p_284585_, glyphprovider$conditional), p_284969_))
                           .toArray(CompletableFuture[]::new);
                        return CompletableFuture.allOf(completablefuture).thenApply(p_284595_ -> {
                           List<GlyphProvider> list2 = p_284618_.stream().flatMap(Optional::stream).toList();
                           return new FontManager.Preparation(map, list2);
                        });
                     }
                  );
            }
         );
   }

   private CompletableFuture<Optional<GlyphProvider>> safeLoad(
      FontManager.BuilderId p_285113_, GlyphProviderDefinition.Loader p_286561_, ResourceManager p_285424_, Executor p_285371_
   ) {
      return CompletableFuture.supplyAsync(() -> {
         try {
            return Optional.of(p_286561_.load(p_285424_));
         } catch (Exception var4x) {
            LOGGER.warn("Failed to load builder {}, rejecting", p_285113_, var4x);
            return Optional.empty();
         }
      }, p_285371_);
   }

   private Map<Identifier, List<GlyphProvider.Conditional>> resolveProviders(List<FontManager.UnresolvedBuilderBundle> p_285282_) {
      Map<Identifier, List<GlyphProvider.Conditional>> map = new HashMap<>();
      DependencySorter<Identifier, FontManager.UnresolvedBuilderBundle> dependencysorter = new DependencySorter<>();
      p_285282_.forEach(p_465479_ -> dependencysorter.addEntry(p_465479_.fontId, p_465479_));
      dependencysorter.orderByDependencies(
         (p_468546_, p_284621_) -> p_284621_.resolve(map::get).ifPresent(p_284590_ -> map.put(p_468546_, (List<GlyphProvider.Conditional>)p_284590_))
      );
      return map;
   }

   private void finalizeProviderLoading(List<GlyphProvider.Conditional> p_285520_, GlyphProvider.Conditional p_326102_) {
      p_285520_.add(0, p_326102_);
      IntSet intset = new IntOpenHashSet();

      for (GlyphProvider.Conditional glyphprovider$conditional : p_285520_) {
         intset.addAll(glyphprovider$conditional.provider().getSupportedGlyphs());
      }

      intset.forEach(p_438726_ -> {
         if (p_438726_ != 32) {
            for (GlyphProvider.Conditional glyphprovider$conditional1 : Lists.reverse(p_285520_)) {
               if (glyphprovider$conditional1.provider().getGlyph(p_438726_) != null) {
                  break;
               }
            }
         }
      });
   }

   private static Set<FontOption> getFontOptions(Options p_326037_) {
      Set<FontOption> set = EnumSet.noneOf(FontOption.class);
      if (p_326037_.forceUnicodeFont().get()) {
         set.add(FontOption.UNIFORM);
      }

      if (p_326037_.japaneseGlyphVariants().get()) {
         set.add(FontOption.JAPANESE_VARIANTS);
      }

      return set;
   }

   private void apply(FontManager.Preparation p_284939_, ProfilerFiller p_285407_) {
      p_285407_.push("closing");
      this.anyGlyphs.invalidate();
      this.nonFishyGlyphs.invalidate();
      this.fontSets.values().forEach(FontSet::close);
      this.fontSets.clear();
      this.providersToClose.forEach(GlyphProvider::close);
      this.providersToClose.clear();
      Set<FontOption> set = getFontOptions(Minecraft.getInstance().options);
      p_285407_.popPush("reloading");
      p_284939_.fontSets().forEach((p_465470_, p_465471_) -> this.fontSets.put(p_465470_, this.createFontSet(p_465470_, Lists.reverse(p_465471_), set)));
      this.providersToClose.addAll(p_284939_.allProviders);
      p_285407_.pop();
      if (!this.fontSets.containsKey(Minecraft.DEFAULT_FONT)) {
         throw new IllegalStateException("Default font failed to load");
      } else {
         this.atlasProviders.clear();
         this.atlasManager.forEach((p_467578_, p_436486_) -> this.atlasProviders.put(p_467578_, new AtlasGlyphProvider(p_436486_)));
      }
   }

   public void updateOptions(Options p_326271_) {
      Set<FontOption> set = getFontOptions(p_326271_);

      for (FontSet fontset : this.fontSets.values()) {
         fontset.reload(set);
      }
   }

   private static List<Pair<FontManager.BuilderId, GlyphProviderDefinition.Conditional>> loadResourceStack(List<Resource> p_284976_, Identifier p_468889_) {
      List<Pair<FontManager.BuilderId, GlyphProviderDefinition.Conditional>> list = new ArrayList<>();

      for (Resource resource : p_284976_) {
         try (Reader reader = resource.openAsReader()) {
            JsonElement jsonelement = (JsonElement)GSON.fromJson(reader, JsonElement.class);
            FontManager.FontDefinitionFile fontmanager$fontdefinitionfile = (FontManager.FontDefinitionFile)FontManager.FontDefinitionFile.CODEC
               .parse(JsonOps.INSTANCE, jsonelement)
               .getOrThrow(JsonParseException::new);
            List<GlyphProviderDefinition.Conditional> list1 = fontmanager$fontdefinitionfile.providers;

            for (int i = list1.size() - 1; i >= 0; i--) {
               FontManager.BuilderId fontmanager$builderid = new FontManager.BuilderId(p_468889_, resource.sourcePackId(), i);
               list.add(Pair.of(fontmanager$builderid, list1.get(i)));
            }
         } catch (Exception var13) {
            LOGGER.warn("Unable to load font '{}' in {} in resourcepack: '{}'", new Object[]{p_468889_, "fonts.json", resource.sourcePackId(), var13});
         }
      }

      return list;
   }

   public Font createFont() {
      return new Font(this.anyGlyphs);
   }

   public Font createFontFilterFishy() {
      return new Font(this.nonFishyGlyphs);
   }

   FontSet getFontSetRaw(Identifier p_469966_) {
      return this.fontSets.getOrDefault(p_469966_, this.missingFontSet);
   }

   GlyphSource getSpriteFont(FontDescription.AtlasSprite p_436579_) {
      AtlasGlyphProvider atlasglyphprovider = this.atlasProviders.get(p_436579_.atlasId());
      return atlasglyphprovider == null ? this.missingFontSet.source(false) : atlasglyphprovider.sourceForSprite(p_436579_.spriteId());
   }

   @Override
   public void close() {
      this.anyGlyphs.close();
      this.nonFishyGlyphs.close();
      this.fontSets.values().forEach(FontSet::close);
      this.providersToClose.forEach(GlyphProvider::close);
      this.missingFontSet.close();
   }

   @OnlyIn(Dist.CLIENT)
   record BuilderId(Identifier fontId, String pack, int index) {
      @Override
      public String toString() {
         return "(" + this.fontId + ": builder #" + this.index + " from pack " + this.pack + ")";
      }
   }

   @OnlyIn(Dist.CLIENT)
   record BuilderResult(FontManager.BuilderId id, FontOption.Filter filter, Either<CompletableFuture<Optional<GlyphProvider>>, Identifier> result) {
      public Optional<List<GlyphProvider.Conditional>> resolve(Function<Identifier, List<GlyphProvider.Conditional>> p_284942_) {
         return (Optional<List<GlyphProvider.Conditional>>)this.result
            .map(
               p_325492_ -> ((Optional)p_325492_.join()).map(p_325491_ -> List.of(new GlyphProvider.Conditional(p_325491_, this.filter))),
               p_467309_ -> {
                  List<GlyphProvider.Conditional> list = p_284942_.apply(p_467309_);
                  if (list == null) {
                     FontManager.LOGGER
                        .warn(
                           "Can't find font {} referenced by builder {}, either because it's missing, failed to load or is part of loading cycle",
                           p_467309_,
                           this.id
                        );
                     return Optional.empty();
                  } else {
                     return Optional.of(list.stream().map(this::mergeFilters).toList());
                  }
               }
            );
      }

      private GlyphProvider.Conditional mergeFilters(GlyphProvider.Conditional p_326502_) {
         return new GlyphProvider.Conditional(p_326502_.provider(), this.filter.merge(p_326502_.filter()));
      }
   }

   @OnlyIn(Dist.CLIENT)
   class CachedFontProvider implements Font.Provider, AutoCloseable {
      private final boolean nonFishyOnly;
      @Nullable
      private volatile FontManager.CachedFontProvider.CachedEntry lastEntry;
      @Nullable
      private volatile EffectGlyph whiteGlyph;

      CachedFontProvider(boolean p_435857_) {
         this.nonFishyOnly = p_435857_;
      }

      public void invalidate() {
         this.lastEntry = null;
         this.whiteGlyph = null;
      }

      @Override
      public void close() {
         this.invalidate();
      }

      private GlyphSource getGlyphSource(FontDescription p_436666_) {
         return switch (p_436666_) {
            case FontDescription.Resource fontdescription$resource -> FontManager.this.getFontSetRaw(fontdescription$resource.id()).source(this.nonFishyOnly);
            case FontDescription.AtlasSprite fontdescription$atlassprite -> FontManager.this.getSpriteFont(fontdescription$atlassprite);
            case FontDescription.PlayerSprite fontdescription$playersprite -> FontManager.this.playerProvider.sourceForPlayer(fontdescription$playersprite);
            default -> FontManager.this.missingFontSet.source(this.nonFishyOnly);
         };
      }

      @Override
      public GlyphSource glyphs(FontDescription p_436755_) {
         FontManager.CachedFontProvider.CachedEntry fontmanager$cachedfontprovider$cachedentry = this.lastEntry;
         if (fontmanager$cachedfontprovider$cachedentry != null && p_436755_.equals(fontmanager$cachedfontprovider$cachedentry.description)) {
            return fontmanager$cachedfontprovider$cachedentry.source;
         } else {
            GlyphSource glyphsource = this.getGlyphSource(p_436755_);
            this.lastEntry = new FontManager.CachedFontProvider.CachedEntry(p_436755_, glyphsource);
            return glyphsource;
         }
      }

      @Override
      public EffectGlyph effect() {
         EffectGlyph effectglyph = this.whiteGlyph;
         if (effectglyph == null) {
            effectglyph = FontManager.this.getFontSetRaw(FontDescription.DEFAULT.id()).whiteGlyph();
            this.whiteGlyph = effectglyph;
         }

         return effectglyph;
      }

      @OnlyIn(Dist.CLIENT)
      record CachedEntry(FontDescription description, GlyphSource source) {
      }
   }

   @OnlyIn(Dist.CLIENT)
   record FontDefinitionFile(List<GlyphProviderDefinition.Conditional> providers) {
      public static final Codec<FontManager.FontDefinitionFile> CODEC = RecordCodecBuilder.create(
         p_325493_ -> p_325493_.group(
               GlyphProviderDefinition.Conditional.CODEC.listOf().fieldOf("providers").forGetter(FontManager.FontDefinitionFile::providers)
            )
            .apply(p_325493_, FontManager.FontDefinitionFile::new)
      );
   }

   @OnlyIn(Dist.CLIENT)
   record Preparation(Map<Identifier, List<GlyphProvider.Conditional>> fontSets, List<GlyphProvider> allProviders) {
   }

   @OnlyIn(Dist.CLIENT)
   record UnresolvedBuilderBundle(Identifier fontId, List<FontManager.BuilderResult> builders, Set<Identifier> dependencies)
      implements DependencySorter.Entry<Identifier> {
      public UnresolvedBuilderBundle(Identifier p_469719_) {
         this(p_469719_, new ArrayList<>(), new HashSet<>());
      }

      public void add(FontManager.BuilderId p_286837_, FontOption.Filter p_326179_, GlyphProviderDefinition.Reference p_286500_) {
         this.builders.add(new FontManager.BuilderResult(p_286837_, p_326179_, Either.right(p_286500_.id())));
         this.dependencies.add(p_286500_.id());
      }

      public void add(FontManager.BuilderId p_284935_, FontOption.Filter p_326423_, CompletableFuture<Optional<GlyphProvider>> p_284966_) {
         this.builders.add(new FontManager.BuilderResult(p_284935_, p_326423_, Either.left(p_284966_)));
      }

      private Stream<CompletableFuture<Optional<GlyphProvider>>> listBuilders() {
         return this.builders.stream().flatMap(p_285041_ -> p_285041_.result.left().stream());
      }

      public Optional<List<GlyphProvider.Conditional>> resolve(Function<Identifier, List<GlyphProvider.Conditional>> p_285118_) {
         List<GlyphProvider.Conditional> list = new ArrayList<>();

         for (FontManager.BuilderResult fontmanager$builderresult : this.builders) {
            Optional<List<GlyphProvider.Conditional>> optional = fontmanager$builderresult.resolve(p_285118_);
            if (!optional.isPresent()) {
               return Optional.empty();
            }

            list.addAll(optional.get());
         }

         return Optional.of(list);
      }

      @Override
      public void visitRequiredDependencies(Consumer<Identifier> p_285391_) {
         this.dependencies.forEach(p_285391_);
      }

      @Override
      public void visitOptionalDependencies(Consumer<Identifier> p_285405_) {
      }
   }
}
