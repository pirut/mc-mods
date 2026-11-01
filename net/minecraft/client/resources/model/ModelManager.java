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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SpecialBlockModelRenderer;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
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

   public ModelManager(BlockColors blockColors, AtlasManager atlasManager, PlayerSkinRenderCache playerSkinRenderCache) {
      this.blockColors = blockColors;
      this.atlasManager = atlasManager;
      this.playerSkinRenderCache = playerSkinRenderCache;
      this.blockModelShaper = new BlockModelShaper(this);
   }

   public BlockStateModel getMissingBlockStateModel() {
      return this.missingModels.block();
   }

   public ItemModel getItemModel(Identifier modelLocation) {
      return this.bakedItemStackModels.getOrDefault(modelLocation, this.missingModels.item());
   }

   public ClientItem.Properties getItemProperties(Identifier itemId) {
      return this.itemProperties.getOrDefault(itemId, ClientItem.Properties.DEFAULT);
   }

   public BlockModelShaper getBlockModelShaper() {
      return this.blockModelShaper;
   }

   @Override
   public final CompletableFuture<Void> reload(
      PreparableReloadListener.SharedState sharedState, Executor executor, PreparableReloadListener.PreparationBarrier preparationBarrier, Executor executor2
   ) {
      ResourceManager resourceManager = sharedState.resourceManager();
      CompletableFuture<EntityModelSet> completableFuture = CompletableFuture.supplyAsync(EntityModelSet::vanilla, executor);
      CompletableFuture<SpecialBlockModelRenderer> completableFuture2 = completableFuture.thenApplyAsync(
         entityModelSet -> SpecialBlockModelRenderer.vanilla(
            new SpecialModelRenderer.BakingContext.Simple(entityModelSet, this.atlasManager, this.playerSkinRenderCache)
         ),
         executor
      );
      CompletableFuture<Map<Identifier, UnbakedModel>> completableFuture3 = loadBlockModels(resourceManager, executor);
      CompletableFuture<BlockStateModelLoader.LoadedModels> completableFuture4 = BlockStateModelLoader.loadBlockStates(resourceManager, executor);
      CompletableFuture<ClientItemInfoLoader.LoadedClientInfos> completableFuture5 = ClientItemInfoLoader.scheduleLoad(resourceManager, executor);
      CompletableFuture<ModelManager.ResolvedModels> completableFuture6 = CompletableFuture.allOf(completableFuture3, completableFuture4, completableFuture5)
         .thenApplyAsync(void_ -> discoverModelDependencies(completableFuture3.join(), completableFuture4.join(), completableFuture5.join()), executor);
      CompletableFuture<Object2IntMap<BlockState>> completableFuture7 = completableFuture4.thenApplyAsync(
         loadedModels -> buildModelGroups(this.blockColors, loadedModels), executor
      );
      AtlasManager.PendingStitchResults pendingStitchResults = sharedState.get(AtlasManager.PENDING_STITCH);
      CompletableFuture<SpriteLoader.Preparations> completableFuture8 = pendingStitchResults.get(AtlasIds.BLOCKS);
      CompletableFuture<SpriteLoader.Preparations> completableFuture9 = pendingStitchResults.get(AtlasIds.ITEMS);
      return CompletableFuture.allOf(
            completableFuture8,
            completableFuture9,
            completableFuture6,
            completableFuture7,
            completableFuture4,
            completableFuture5,
            completableFuture,
            completableFuture2,
            completableFuture3
         )
         .thenComposeAsync(
            void_ -> {
               SpriteLoader.Preparations preparations = completableFuture8.join();
               SpriteLoader.Preparations preparations2 = completableFuture9.join();
               ModelManager.ResolvedModels resolvedModels = completableFuture6.join();
               Object2IntMap<BlockState> object2IntMap = completableFuture7.join();
               Set<Identifier> set = Sets.difference(completableFuture3.join().keySet(), resolvedModels.models.keySet());
               if (!set.isEmpty()) {
                  LOGGER.debug("Unreferenced models: \n{}", set.stream().sorted().map(identifier -> "\t" + identifier + "\n").collect(Collectors.joining()));
               }

               ModelBakery modelBakery = new ModelBakery(
                  completableFuture.join(),
                  this.atlasManager,
                  this.playerSkinRenderCache,
                  completableFuture4.join().models(),
                  completableFuture5.join().contents(),
                  resolvedModels.models(),
                  resolvedModels.missing()
               );
               return loadModels(preparations, preparations2, modelBakery, object2IntMap, completableFuture.join(), completableFuture2.join(), executor);
            },
            executor
         )
         .thenCompose(preparationBarrier::wait)
         .thenAcceptAsync(this::apply, executor2);
   }

   private static CompletableFuture<Map<Identifier, UnbakedModel>> loadBlockModels(ResourceManager resourceManager, Executor executor) {
      return CompletableFuture.<Map<Identifier, Resource>>supplyAsync(() -> MODEL_LISTER.listMatchingResources(resourceManager), executor)
         .thenCompose(
            map -> {
               List<CompletableFuture<Pair<Identifier, BlockModel>>> list = new ArrayList<>(map.size());

               for (Entry<Identifier, Resource> entry : map.entrySet()) {
                  list.add(CompletableFuture.supplyAsync(() -> {
                     Identifier identifier = MODEL_LISTER.fileToId(entry.getKey());

                     try {
                        Pair var3;
                        try (Reader reader = entry.getValue().openAsReader()) {
                           var3 = Pair.of(identifier, BlockModel.fromStream(reader));
                        }

                        return var3;
                     } catch (Exception var7) {
                        LOGGER.error("Failed to load model {}", entry.getKey(), var7);
                        return null;
                     }
                  }, executor));
               }

               return Util.sequence(list)
                  .thenApply(listx -> listx.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond)));
            }
         );
   }

   private static ModelManager.ResolvedModels discoverModelDependencies(
      Map<Identifier, UnbakedModel> inputModels, BlockStateModelLoader.LoadedModels loadedModels, ClientItemInfoLoader.LoadedClientInfos loadedClientInfos
   ) {
      ModelManager.ResolvedModels var5;
      try (Zone zone = Profiler.get().zone("dependencies")) {
         ModelDiscovery modelDiscovery = new ModelDiscovery(inputModels, MissingBlockModel.missingModel());
         modelDiscovery.addSpecialModel(ItemModelGenerator.GENERATED_ITEM_MODEL_ID, new ItemModelGenerator());
         loadedModels.models().values().forEach(modelDiscovery::addRoot);
         loadedClientInfos.contents().values().forEach(clientItem -> modelDiscovery.addRoot(clientItem.model()));
         var5 = new ModelManager.ResolvedModels(modelDiscovery.missingModel(), modelDiscovery.resolve());
      }

      return var5;
   }

   private static CompletableFuture<ModelManager.ReloadState> loadModels(
      SpriteLoader.Preparations blocksAtlas,
      SpriteLoader.Preparations itemsAtlas,
      ModelBakery modelBakery,
      Object2IntMap<BlockState> modelGroups,
      EntityModelSet entityModels,
      SpecialBlockModelRenderer specialBlockRenderer,
      Executor executor
   ) {
      final Multimap<String, Material> multimap = Multimaps.synchronizedMultimap(HashMultimap.create());
      final Multimap<String, String> multimap2 = Multimaps.synchronizedMultimap(HashMultimap.create());
      return modelBakery.bakeModels(new SpriteGetter() {
            private final TextureAtlasSprite blockMissing = blocksAtlas.missing();
            private final TextureAtlasSprite itemMissing = itemsAtlas.missing();

            @Override
            public TextureAtlasSprite get(Material material, ModelDebugName debugName) {
               Identifier identifier = material.atlasLocation();
               boolean bl = identifier.equals(ModelManager.BLOCK_OR_ITEM);
               boolean bl2 = identifier.equals(TextureAtlas.LOCATION_ITEMS);
               boolean bl3 = identifier.equals(TextureAtlas.LOCATION_BLOCKS);
               if (bl || bl2) {
                  TextureAtlasSprite textureAtlasSprite = itemsAtlas.getSprite(material.texture());
                  if (textureAtlasSprite != null) {
                     return textureAtlasSprite;
                  }
               }

               if (bl || bl3) {
                  TextureAtlasSprite textureAtlasSprite = blocksAtlas.getSprite(material.texture());
                  if (textureAtlasSprite != null) {
                     return textureAtlasSprite;
                  }
               }

               multimap.put(debugName.debugName(), material);
               return bl2 ? this.itemMissing : this.blockMissing;
            }

            @Override
            public TextureAtlasSprite reportMissingReference(String name, ModelDebugName debugName) {
               multimap2.put(debugName.debugName(), name);
               return this.blockMissing;
            }
         }, executor)
         .thenApply(
            bakingResult -> {
               multimap.asMap()
                  .forEach(
                     (string, collection) -> LOGGER.warn(
                        "Missing textures in model {}:\n{}",
                        string,
                        collection.stream()
                           .sorted(Material.COMPARATOR)
                           .map(material -> "    " + material.atlasLocation() + ":" + material.texture())
                           .collect(Collectors.joining("\n"))
                     )
                  );
               multimap2.asMap()
                  .forEach(
                     (string, collection) -> LOGGER.warn(
                        "Missing texture references in model {}:\n{}",
                        string,
                        collection.stream().sorted().map(stringx -> "    " + stringx).collect(Collectors.joining("\n"))
                     )
                  );
               Map<BlockState, BlockStateModel> map = createBlockStateToModelDispatch(bakingResult.blockStateModels(), bakingResult.missingModels().block());
               return new ModelManager.ReloadState(bakingResult, modelGroups, map, entityModels, specialBlockRenderer);
            }
         );
   }

   private static Map<BlockState, BlockStateModel> createBlockStateToModelDispatch(
      Map<BlockState, BlockStateModel> blockStateModels, BlockStateModel missingModel
   ) {
      Object var8;
      try (Zone zone = Profiler.get().zone("block state dispatch")) {
         Map<BlockState, BlockStateModel> map = new IdentityHashMap<>(blockStateModels);

         for (Block block : BuiltInRegistries.BLOCK) {
            block.getStateDefinition().getPossibleStates().forEach(blockState -> {
               if (blockStateModels.putIfAbsent(blockState, missingModel) == null) {
                  LOGGER.warn("Missing model for variant: '{}'", blockState);
               }
            });
         }

         var8 = map;
      }

      return (Map<BlockState, BlockStateModel>)var8;
   }

   private static Object2IntMap<BlockState> buildModelGroups(BlockColors blockColors, BlockStateModelLoader.LoadedModels loadedModels) {
      Object2IntMap var3;
      try (Zone zone = Profiler.get().zone("block groups")) {
         var3 = ModelGroupCollector.build(blockColors, loadedModels);
      }

      return var3;
   }

   private void apply(ModelManager.ReloadState state) {
      ModelBakery.BakingResult bakingResult = state.bakedModels;
      this.bakedItemStackModels = bakingResult.itemStackModels();
      this.itemProperties = bakingResult.itemProperties();
      this.modelGroups = state.modelGroups;
      this.missingModels = bakingResult.missingModels();
      this.blockModelShaper.replaceCache(state.modelCache);
      this.specialBlockModelRenderer = state.specialBlockModelRenderer;
      this.entityModelSet = state.entityModelSet;
   }

   public boolean requiresRender(BlockState oldState, BlockState newState) {
      if (oldState == newState) {
         return false;
      } else {
         int i = this.modelGroups.getInt(oldState);
         if (i != -1) {
            int j = this.modelGroups.getInt(newState);
            if (i == j) {
               FluidState fluidState = oldState.getFluidState();
               FluidState fluidState2 = newState.getFluidState();
               return fluidState != fluidState2;
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

   @Environment(EnvType.CLIENT)
   record ReloadState(
      ModelBakery.BakingResult bakedModels,
      Object2IntMap<BlockState> modelGroups,
      Map<BlockState, BlockStateModel> modelCache,
      EntityModelSet entityModelSet,
      SpecialBlockModelRenderer specialBlockModelRenderer
   ) {
   }

   @Environment(EnvType.CLIENT)
   record ResolvedModels(ResolvedModel missing, Map<Identifier, ResolvedModel> models) {
   }
}
