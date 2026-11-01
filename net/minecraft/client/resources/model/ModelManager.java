package net.minecraft.client.resources.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SpecialBlockModelRenderer;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.entity.animation.json.AnimationLoader;
import net.neoforged.neoforge.client.model.UnbakedModelParser;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ModelManager implements PreparableReloadListener {
   public static final Identifier BLOCK_OR_ITEM = Identifier.withDefaultNamespace("block_or_item");
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final FileToIdConverter MODEL_LISTER = FileToIdConverter.json("models");
   private Map<Identifier, ItemModel> bakedItemStackModels = Map.of();
   private Map<Identifier, ClientItem.Properties> itemProperties = Map.of();
   private final AtlasManager atlasManager;
   private final PlayerSkinRenderCache playerSkinRenderCache;
   private final BlockModelShaper blockModelShaper;
   private final BlockColors blockColors;
   private EntityModelSet entityModelSet = EntityModelSet.EMPTY;
   private SpecialBlockModelRenderer specialBlockModelRenderer = SpecialBlockModelRenderer.EMPTY;
   private ModelBakery.MissingModels missingModels;
   private Object2IntMap<BlockState> modelGroups = Object2IntMaps.emptyMap();
   private final AtomicReference<ModelBakery> modelBakery = new AtomicReference<>(null);
   private StandaloneModelLoader.BakedModels bakedStandaloneModels;
   private Set<Identifier> reportedMissingItemModels = new HashSet<>();

   public ModelManager(BlockColors p_119407_, AtlasManager p_434259_, PlayerSkinRenderCache p_440151_) {
      this.blockColors = p_119407_;
      this.atlasManager = p_434259_;
      this.playerSkinRenderCache = p_440151_;
      this.blockModelShaper = new BlockModelShaper(this);
   }

   public BlockStateModel getMissingBlockStateModel() {
      return this.missingModels.block();
   }

   public ItemModel getItemModel(Identifier p_468547_) {
      ItemModel model = this.bakedItemStackModels.get(p_468547_);
      if (model == null) {
         if (this.reportedMissingItemModels.add(p_468547_)) {
            LOGGER.warn("Missing item model for location {}", p_468547_);
         }

         return this.missingModels.item();
      } else {
         return model;
      }
   }

   public ClientItem.Properties getItemProperties(Identifier p_467175_) {
      return this.itemProperties.getOrDefault(p_467175_, ClientItem.Properties.DEFAULT);
   }

   public BlockModelShaper getBlockModelShaper() {
      return this.blockModelShaper;
   }

   @Override
   public final CompletableFuture<Void> reload(
      PreparableReloadListener.SharedState p_433049_, Executor p_250550_, PreparableReloadListener.PreparationBarrier p_249079_, Executor p_249221_
   ) {
      ResourceManager resourcemanager = p_433049_.resourceManager();
      CompletableFuture<EntityModelSet> completablefuture = CompletableFuture.supplyAsync(EntityModelSet::vanilla, p_250550_);
      AnimationLoader.PendingAnimations pendingAnimations = p_433049_.get(AnimationLoader.STATE_KEY);
      CompletableFuture<SpecialBlockModelRenderer> completablefuture1 = completablefuture.thenApplyAsync(
         p_438833_ -> SpecialBlockModelRenderer.vanilla(
            new SpecialModelRenderer.BakingContext.Simple(p_438833_, this.atlasManager, this.playerSkinRenderCache, pendingAnimations)
         ),
         p_250550_
      );
      CompletableFuture<Map<Identifier, UnbakedModel>> completablefuture2 = loadBlockModels(resourcemanager, p_250550_);
      CompletableFuture<BlockStateModelLoader.LoadedModels> completablefuture3 = BlockStateModelLoader.loadBlockStates(resourcemanager, p_250550_);
      CompletableFuture<ClientItemInfoLoader.LoadedClientInfos> completablefuture4 = ClientItemInfoLoader.scheduleLoad(resourcemanager, p_250550_);
      CompletableFuture<StandaloneModelLoader.LoadedModels> standaloneModelsFuture = StandaloneModelLoader.load(p_250550_);
      CompletableFuture<ModelManager.ResolvedModels> completablefuture5 = CompletableFuture.allOf(
            completablefuture2, completablefuture3, completablefuture4, standaloneModelsFuture
         )
         .thenApplyAsync(
            p_404152_ -> discoverModelDependencies(
               completablefuture2.join(), completablefuture3.join(), completablefuture4.join(), standaloneModelsFuture.join()
            ),
            p_250550_
         );
      CompletableFuture<Object2IntMap<BlockState>> completablefuture6 = completablefuture3.thenApplyAsync(
         p_359309_ -> buildModelGroups(this.blockColors, p_359309_), p_250550_
      );
      AtlasManager.PendingStitchResults atlasmanager$pendingstitchresults = p_433049_.get(AtlasManager.PENDING_STITCH);
      CompletableFuture<SpriteLoader.Preparations> completablefuture7 = atlasmanager$pendingstitchresults.get(AtlasIds.BLOCKS);
      CompletableFuture<SpriteLoader.Preparations> completablefuture8 = atlasmanager$pendingstitchresults.get(AtlasIds.ITEMS);
      return CompletableFuture.allOf(
            completablefuture7,
            completablefuture8,
            completablefuture5,
            completablefuture6,
            completablefuture3,
            completablefuture4,
            completablefuture,
            completablefuture1,
            completablefuture2,
            standaloneModelsFuture
         )
         .thenComposeAsync(
            p_465791_ -> {
               SpriteLoader.Preparations spriteloader$preparations = completablefuture7.join();
               SpriteLoader.Preparations spriteloader$preparations1 = completablefuture8.join();
               ModelManager.ResolvedModels modelmanager$resolvedmodels = completablefuture5.join();
               Object2IntMap<BlockState> object2intmap = completablefuture6.join();
               Set<Identifier> set = Sets.difference(completablefuture2.join().keySet(), modelmanager$resolvedmodels.models.keySet());
               if (!set.isEmpty()) {
                  LOGGER.debug("Unreferenced models: \n{}", set.stream().sorted().map(p_467252_ -> "\t" + p_467252_ + "\n").collect(Collectors.joining()));
               }

               ModelBakery modelbakery = new ModelBakery(
                  completablefuture.join(),
                  this.atlasManager,
                  this.playerSkinRenderCache,
                  completablefuture3.join().models(),
                  completablefuture4.join().contents(),
                  modelmanager$resolvedmodels.models(),
                  modelmanager$resolvedmodels.missing(),
                  standaloneModelsFuture.join(),
                  pendingAnimations
               );
               this.modelBakery.set(modelbakery);
               return loadModels(
                  spriteloader$preparations,
                  spriteloader$preparations1,
                  modelbakery,
                  object2intmap,
                  completablefuture.join(),
                  completablefuture1.join(),
                  p_250550_
               );
            },
            p_250550_
         )
         .thenCompose(p_249079_::wait)
         .thenAcceptAsync(this::apply, p_249221_);
   }

   private static CompletableFuture<Map<Identifier, UnbakedModel>> loadBlockModels(ResourceManager p_251361_, Executor p_252189_) {
      return CompletableFuture.<Map<Identifier, Resource>>supplyAsync(() -> MODEL_LISTER.listMatchingResources(p_251361_), p_252189_)
         .thenCompose(
            p_465794_ -> {
               List<CompletableFuture<Pair<Identifier, UnbakedModel>>> list = new ArrayList<>(p_465794_.size());

               for (Entry<Identifier, Resource> entry : p_465794_.entrySet()) {
                  list.add(CompletableFuture.supplyAsync(() -> {
                     Identifier identifier = MODEL_LISTER.fileToId(entry.getKey());

                     try {
                        Pair pair;
                        try (Reader reader = entry.getValue().openAsReader()) {
                           pair = Pair.of(identifier, UnbakedModelParser.parse(reader));
                        }

                        return pair;
                     } catch (Exception var8) {
                        LOGGER.error("Failed to load model {}", entry.getKey(), var8);
                        return null;
                     }
                  }, p_252189_));
               }

               return Util.sequence(list)
                  .thenApply(p_250813_ -> p_250813_.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond)));
            }
         );
   }

   @Deprecated
   private static ModelManager.ResolvedModels discoverModelDependencies(
      Map<Identifier, UnbakedModel> p_363228_, BlockStateModelLoader.LoadedModels p_361624_, ClientItemInfoLoader.LoadedClientInfos p_390496_
   ) {
      return discoverModelDependencies(p_363228_, p_361624_, p_390496_, StandaloneModelLoader.LoadedModels.EMPTY);
   }

   private static ModelManager.ResolvedModels discoverModelDependencies(
      Map<Identifier, UnbakedModel> p_363228_,
      BlockStateModelLoader.LoadedModels p_361624_,
      ClientItemInfoLoader.LoadedClientInfos p_390496_,
      StandaloneModelLoader.LoadedModels standaloneModels
   ) {
      ModelManager.ResolvedModels modelmanager$resolvedmodels;
      try (Zone zone = Profiler.get().zone("dependencies")) {
         ModelDiscovery modeldiscovery = new ModelDiscovery(p_363228_, MissingBlockModel.missingModel());
         modeldiscovery.addSpecialModel(ItemModelGenerator.GENERATED_ITEM_MODEL_ID, new ItemModelGenerator());
         p_361624_.models().values().forEach(modeldiscovery::addRoot);
         p_390496_.contents().values().forEach(p_390109_ -> modeldiscovery.addRoot(p_390109_.model()));
         standaloneModels.models().values().forEach(modeldiscovery::addRoot);
         modelmanager$resolvedmodels = new ModelManager.ResolvedModels(modeldiscovery.missingModel(), modeldiscovery.resolve());
      }

      return modelmanager$resolvedmodels;
   }

   private static CompletableFuture<ModelManager.ReloadState> loadModels(
      final SpriteLoader.Preparations p_434753_,
      final SpriteLoader.Preparations p_468425_,
      ModelBakery p_248945_,
      Object2IntMap<BlockState> p_363498_,
      EntityModelSet p_388110_,
      SpecialBlockModelRenderer p_387466_,
      Executor p_405316_
   ) {
      final Multimap<String, Material> multimap = Multimaps.synchronizedMultimap(HashMultimap.create());
      final Multimap<String, String> multimap1 = Multimaps.synchronizedMultimap(HashMultimap.create());
      return p_248945_.bakeModels(new SpriteGetter() {
            private final TextureAtlasSprite blockMissing = p_434753_.missing();
            private final TextureAtlasSprite itemMissing = p_468425_.missing();

            @Override
            public TextureAtlasSprite get(Material p_388183_, ModelDebugName p_388862_) {
               Identifier identifier = p_388183_.atlasLocation();
               boolean flag = identifier.equals(ModelManager.BLOCK_OR_ITEM);
               boolean flag1 = identifier.equals(TextureAtlas.LOCATION_ITEMS);
               boolean flag2 = identifier.equals(TextureAtlas.LOCATION_BLOCKS);
               if (flag || flag1) {
                  TextureAtlasSprite textureatlassprite = p_468425_.getSprite(p_388183_.texture());
                  if (textureatlassprite != null) {
                     return textureatlassprite;
                  }
               }

               if (flag || flag2) {
                  TextureAtlasSprite textureatlassprite1 = p_434753_.getSprite(p_388183_.texture());
                  if (textureatlassprite1 != null) {
                     return textureatlassprite1;
                  }
               }

               multimap.put(p_388862_.debugName(), p_388183_);
               return flag1 ? this.itemMissing : this.blockMissing;
            }

            @Override
            public TextureAtlasSprite reportMissingReference(String p_387702_, ModelDebugName p_387819_) {
               multimap1.put(p_387819_.debugName(), p_387702_);
               return this.blockMissing;
            }
         }, p_405316_)
         .thenApply(
            p_432333_ -> {
               multimap.asMap()
                  .forEach(
                     (p_387727_, p_252017_) -> LOGGER.warn(
                        "Missing textures in model {}:\n{}",
                        p_387727_,
                        p_252017_.stream()
                           .sorted(Material.COMPARATOR)
                           .map(p_465792_ -> "    " + p_465792_.atlasLocation() + ":" + p_465792_.texture())
                           .collect(Collectors.joining("\n"))
                     )
                  );
               multimap1.asMap()
                  .forEach(
                     (p_386266_, p_386267_) -> LOGGER.warn(
                        "Missing texture references in model {}:\n{}",
                        p_386266_,
                        p_386267_.stream().sorted().map(p_386265_ -> "    " + p_386265_).collect(Collectors.joining("\n"))
                     )
                  );

               try (Zone ignored = Profiler.get().zone("neoforge_modify_baking_result")) {
                  ClientHooks.onModifyBakingResult(p_432333_, p_434753_, p_248945_);
               }

               Map<BlockState, BlockStateModel> map = createBlockStateToModelDispatch(p_432333_.blockStateModels(), p_432333_.missingModels().block());
               return new ModelManager.ReloadState(p_432333_, p_363498_, map, p_388110_, p_387466_);
            }
         );
   }

   private static Map<BlockState, BlockStateModel> createBlockStateToModelDispatch(Map<BlockState, BlockStateModel> p_386989_, BlockStateModel p_405855_) {
      Object object;
      try (Zone zone = Profiler.get().zone("block state dispatch")) {
         Map<BlockState, BlockStateModel> map = new IdentityHashMap<>(p_386989_);

         for (Block block : BuiltInRegistries.BLOCK) {
            block.getStateDefinition().getPossibleStates().forEach(p_404155_ -> {
               if (p_386989_.putIfAbsent(p_404155_, p_405855_) == null) {
                  LOGGER.warn("Missing model for variant: '{}'", p_404155_);
               }
            });
         }

         object = map;
      }

      return (Map<BlockState, BlockStateModel>)object;
   }

   private static Object2IntMap<BlockState> buildModelGroups(BlockColors p_362057_, BlockStateModelLoader.LoadedModels p_362559_) {
      Object2IntMap object2intmap;
      try (Zone zone = Profiler.get().zone("block groups")) {
         object2intmap = ModelGroupCollector.build(p_362057_, p_362559_);
      }

      return object2intmap;
   }

   private void apply(ModelManager.ReloadState p_248996_) {
      ModelBakery.BakingResult modelbakery$bakingresult = p_248996_.bakedModels;
      this.bakedItemStackModels = modelbakery$bakingresult.itemStackModels();
      this.itemProperties = modelbakery$bakingresult.itemProperties();
      this.modelGroups = p_248996_.modelGroups;
      this.missingModels = modelbakery$bakingresult.missingModels();
      this.bakedStandaloneModels = modelbakery$bakingresult.standaloneModels();
      ClientHooks.onModelBake(this, modelbakery$bakingresult, this.modelBakery.get());
      this.reportedMissingItemModels = new HashSet<>();

      for (Item item : BuiltInRegistries.ITEM) {
         Identifier modelId = item.components().get(DataComponents.ITEM_MODEL);
         if (modelId != null && !this.bakedItemStackModels.containsKey(modelId)) {
            this.reportedMissingItemModels.add(modelId);
            LOGGER.warn("No model loaded for default item model ID {} of {}", modelId, item);
         }
      }

      this.blockModelShaper.replaceCache(p_248996_.modelCache);
      this.specialBlockModelRenderer = p_248996_.specialBlockModelRenderer;
      this.entityModelSet = p_248996_.entityModelSet;
   }

   public boolean requiresRender(BlockState p_119416_, BlockState p_119417_) {
      if (p_119416_ == p_119417_) {
         return false;
      } else {
         int i = this.modelGroups.getInt(p_119416_);
         if (i != -1) {
            int j = this.modelGroups.getInt(p_119417_);
            if (i == j) {
               FluidState fluidstate = p_119416_.getFluidState();
               FluidState fluidstate1 = p_119417_.getFluidState();
               return fluidstate != fluidstate1;
            }
         }

         return true;
      }
   }

   public SpecialBlockModelRenderer specialBlockModelRenderer() {
      return this.specialBlockModelRenderer;
   }

   public Supplier<EntityModelSet> entityModels() {
      return () -> this.entityModelSet;
   }

   public ModelBakery getModelBakery() {
      return this.modelBakery.get();
   }

   @Nullable
   public <T> T getStandaloneModel(StandaloneModelKey<T> modelKey) {
      return this.bakedStandaloneModels.get(modelKey);
   }

   @OnlyIn(Dist.CLIENT)
   record ReloadState(
      ModelBakery.BakingResult bakedModels,
      Object2IntMap<BlockState> modelGroups,
      Map<BlockState, BlockStateModel> modelCache,
      EntityModelSet entityModelSet,
      SpecialBlockModelRenderer specialBlockModelRenderer
   ) {
   }

   @OnlyIn(Dist.CLIENT)
   record ResolvedModels(ResolvedModel missing, Map<Identifier, ResolvedModel> models) {
   }
}
