package net.minecraft.client.resources.model;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.MissingItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.thread.ParallelMapTransform;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.entity.animation.json.AnimationLoader;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader;
import org.joml.Vector3fc;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ModelBakery {
   public static final Material FIRE_0 = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("fire_0");
   public static final Material FIRE_1 = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("fire_1");
   public static final Material LAVA_STILL = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("lava_still");
   public static final Material LAVA_FLOW = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("lava_flow");
   public static final Material WATER_STILL = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("water_still");
   public static final Material WATER_FLOW = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("water_flow");
   public static final Material WATER_OVERLAY = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("water_overlay");
   public static final Material BANNER_BASE = new Material(Sheets.BANNER_SHEET, Identifier.withDefaultNamespace("entity/banner_base"));
   public static final Material SHIELD_BASE = new Material(Sheets.SHIELD_SHEET, Identifier.withDefaultNamespace("entity/shield_base"));
   public static final Material NO_PATTERN_SHIELD = new Material(Sheets.SHIELD_SHEET, Identifier.withDefaultNamespace("entity/shield_base_nopattern"));
   public static final int DESTROY_STAGE_COUNT = 10;
   public static final List<Identifier> DESTROY_STAGES = IntStream.range(0, 10)
      .mapToObj(p_465777_ -> Identifier.withDefaultNamespace("block/destroy_stage_" + p_465777_))
      .collect(Collectors.toList());
   public static final List<Identifier> BREAKING_LOCATIONS = DESTROY_STAGES.stream()
      .map(p_465776_ -> p_465776_.withPath(p_349911_ -> "textures/" + p_349911_ + ".png"))
      .collect(Collectors.toList());
   public static final List<RenderType> DESTROY_TYPES = BREAKING_LOCATIONS.stream().map(RenderTypes::crumbling).collect(Collectors.toList());
   static final Logger LOGGER = LogUtils.getLogger();
   private final EntityModelSet entityModelSet;
   private final MaterialSet materials;
   private final PlayerSkinRenderCache playerSkinRenderCache;
   private final Map<BlockState, BlockStateModel.UnbakedRoot> unbakedBlockStateModels;
   private final Map<Identifier, ClientItem> clientInfos;
   final Map<Identifier, ResolvedModel> resolvedModels;
   final ResolvedModel missingModel;
   private final StandaloneModelLoader.LoadedModels standaloneModels;
   private final AnimationLoader.PendingAnimations pendingAnimations;

   @Deprecated
   public ModelBakery(
      EntityModelSet p_388903_,
      MaterialSet p_434703_,
      PlayerSkinRenderCache p_440085_,
      Map<BlockState, BlockStateModel.UnbakedRoot> p_251087_,
      Map<Identifier, ClientItem> p_250416_,
      Map<Identifier, ResolvedModel> p_388404_,
      ResolvedModel p_404940_
   ) {
      this(
         p_388903_,
         p_434703_,
         p_440085_,
         p_251087_,
         p_250416_,
         p_388404_,
         p_404940_,
         StandaloneModelLoader.LoadedModels.EMPTY,
         AnimationLoader.PendingAnimations.EMPTY
      );
   }

   public ModelBakery(
      EntityModelSet p_388903_,
      MaterialSet p_434703_,
      PlayerSkinRenderCache p_440085_,
      Map<BlockState, BlockStateModel.UnbakedRoot> p_251087_,
      Map<Identifier, ClientItem> p_250416_,
      Map<Identifier, ResolvedModel> p_388404_,
      ResolvedModel p_404940_,
      StandaloneModelLoader.LoadedModels standaloneModels,
      AnimationLoader.PendingAnimations pendingAnimations
   ) {
      this.entityModelSet = p_388903_;
      this.materials = p_434703_;
      this.playerSkinRenderCache = p_440085_;
      this.unbakedBlockStateModels = p_251087_;
      this.clientInfos = p_250416_;
      this.resolvedModels = p_388404_;
      this.missingModel = p_404940_;
      this.standaloneModels = standaloneModels;
      this.pendingAnimations = pendingAnimations;
   }

   public CompletableFuture<ModelBakery.BakingResult> bakeModels(SpriteGetter p_404922_, Executor p_405407_) {
      ModelBakery.PartCacheImpl modelbakery$partcacheimpl = new ModelBakery.PartCacheImpl();
      ModelBakery.MissingModels modelbakery$missingmodels = ModelBakery.MissingModels.bake(this.missingModel, p_404922_, modelbakery$partcacheimpl);
      ModelBakery.ModelBakerImpl modelbakery$modelbakerimpl = new ModelBakery.ModelBakerImpl(p_404922_, modelbakery$partcacheimpl, modelbakery$missingmodels);
      CompletableFuture<Map<BlockState, BlockStateModel>> completablefuture = ParallelMapTransform.schedule(
         this.unbakedBlockStateModels, (p_409109_, p_409110_) -> {
            try {
               return p_409110_.bake(p_409109_, modelbakery$modelbakerimpl);
            } catch (Exception var4x) {
               LOGGER.warn("Unable to bake model: '{}': {}", p_409109_, var4x);
               return null;
            }
         }, p_405407_
      );
      CompletableFuture<Map<Identifier, ItemModel>> completablefuture1 = ParallelMapTransform.schedule(
         this.clientInfos,
         (p_468197_, p_438822_) -> {
            try {
               return p_438822_.model()
                  .bake(
                     new ItemModel.BakingContext(
                        modelbakery$modelbakerimpl,
                        this.entityModelSet,
                        this.materials,
                        this.playerSkinRenderCache,
                        modelbakery$missingmodels.item,
                        p_438822_.registrySwapper(),
                        this.pendingAnimations
                     )
                  );
            } catch (Exception var6x) {
               LOGGER.warn("Unable to bake item model: '{}'", p_468197_, var6x);
               return null;
            }
         },
         p_405407_
      );
      CompletableFuture<StandaloneModelLoader.BakedModels> standaloneModelsFuture = StandaloneModelLoader.bake(
         this.standaloneModels, modelbakery$modelbakerimpl, p_405407_
      );
      Map<Identifier, ClientItem.Properties> map = new HashMap<>(this.clientInfos.size());
      this.clientInfos.forEach((p_467624_, p_404117_) -> {
         ClientItem.Properties clientitem$properties = p_404117_.properties();
         if (!clientitem$properties.equals(ClientItem.Properties.DEFAULT)) {
            map.put(p_467624_, clientitem$properties);
         }
      });
      completablefuture = completablefuture.thenCombine(standaloneModelsFuture, (stateModels, standaloneModels) -> stateModels);
      return completablefuture.thenCombine(
         completablefuture1,
         (p_404127_, p_404128_) -> new ModelBakery.BakingResult(
            modelbakery$missingmodels, (Map<BlockState, BlockStateModel>)p_404127_, (Map<Identifier, ItemModel>)p_404128_, map, standaloneModelsFuture.join()
         )
      );
   }

   @OnlyIn(Dist.CLIENT)
   public record BakingResult(
      ModelBakery.MissingModels missingModels,
      Map<BlockState, BlockStateModel> blockStateModels,
      Map<Identifier, ItemModel> itemStackModels,
      Map<Identifier, ClientItem.Properties> itemProperties,
      StandaloneModelLoader.BakedModels standaloneModels
   ) {
      @Deprecated
      public BakingResult(
         ModelBakery.MissingModels missingModels,
         Map<BlockState, BlockStateModel> blockStateModels,
         Map<Identifier, ItemModel> itemStackModels,
         Map<Identifier, ClientItem.Properties> itemProperties
      ) {
         this(missingModels, blockStateModels, itemStackModels, itemProperties, StandaloneModelLoader.BakedModels.EMPTY);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record MissingModels(BlockModelPart blockPart, BlockStateModel block, ItemModel item) {
      public static ModelBakery.MissingModels bake(ResolvedModel p_405089_, final SpriteGetter p_405847_, final ModelBaker.PartCache p_470602_) {
         ModelBaker modelbaker = new ModelBaker() {
            @Override
            public ResolvedModel getModel(Identifier p_466843_) {
               throw new IllegalStateException("Missing model can't have dependencies, but asked for " + p_466843_);
            }

            @Override
            public BlockModelPart missingBlockModelPart() {
               throw new IllegalStateException();
            }

            @Override
            public <T> T compute(ModelBaker.SharedOperationKey<T> p_410289_) {
               return p_410289_.compute(this);
            }

            @Override
            public SpriteGetter sprites() {
               return p_405847_;
            }

            @Override
            public ModelBaker.PartCache parts() {
               return p_470602_;
            }
         };
         TextureSlots textureslots = p_405089_.getTopTextureSlots();
         boolean flag = p_405089_.getTopAmbientOcclusion();
         boolean flag1 = p_405089_.getTopGuiLight().lightLikeBlock();
         ItemTransforms itemtransforms = p_405089_.getTopTransforms();
         QuadCollection quadcollection = p_405089_.bakeTopGeometry(textureslots, modelbaker, BlockModelRotation.IDENTITY);
         TextureAtlasSprite textureatlassprite = p_405089_.resolveParticleSprite(textureslots, modelbaker);
         SimpleModelWrapper simplemodelwrapper = new SimpleModelWrapper(quadcollection, flag, textureatlassprite);
         BlockStateModel blockstatemodel = new SingleVariant(simplemodelwrapper);
         ItemModel itemmodel = new MissingItemModel(quadcollection.getAll(), new ModelRenderProperties(flag1, textureatlassprite, itemtransforms));
         return new ModelBakery.MissingModels(simplemodelwrapper, blockstatemodel, itemmodel);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class ModelBakerImpl implements ModelBaker {
      private final SpriteGetter sprites;
      private final ModelBaker.PartCache parts;
      private final ModelBakery.MissingModels missingModels;
      private final Map<ModelBaker.SharedOperationKey<Object>, Object> operationCache = new ConcurrentHashMap<>();
      private final Function<ModelBaker.SharedOperationKey<Object>, Object> cacheComputeFunction = p_410129_ -> p_410129_.compute(this);

      ModelBakerImpl(SpriteGetter p_405481_, ModelBaker.PartCache p_470712_, ModelBakery.MissingModels p_470605_) {
         this.sprites = p_405481_;
         this.parts = p_470712_;
         this.missingModels = p_470605_;
      }

      @Override
      public BlockModelPart missingBlockModelPart() {
         return this.missingModels.blockPart;
      }

      @Override
      public SpriteGetter sprites() {
         return this.sprites;
      }

      @Override
      public ModelBaker.PartCache parts() {
         return this.parts;
      }

      @Override
      public ResolvedModel getModel(Identifier p_467266_) {
         ResolvedModel resolvedmodel = ModelBakery.this.resolvedModels.get(p_467266_);
         if (resolvedmodel == null) {
            ModelBakery.LOGGER.warn("Requested a model that was not discovered previously: {}", p_467266_);
            return ModelBakery.this.missingModel;
         } else {
            return resolvedmodel;
         }
      }

      @Override
      public <T> T compute(ModelBaker.SharedOperationKey<T> p_409780_) {
         return (T)this.operationCache.computeIfAbsent(p_409780_, this.cacheComputeFunction);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class PartCacheImpl implements ModelBaker.PartCache {
      private final Interner<Vector3fc> vectors = Interners.newStrongInterner();
      private final Interner<BakedNormals> normals = Interners.newStrongInterner();
      private final Interner<BakedColors> colors = Interners.newStrongInterner();

      @Override
      public Vector3fc vector(Vector3fc p_470814_) {
         return (Vector3fc)this.vectors.intern(p_470814_);
      }

      @Override
      public BakedNormals normals(BakedNormals normals) {
         return (BakedNormals)this.normals.intern(normals);
      }

      @Override
      public BakedColors colors(BakedColors colors) {
         return (BakedColors)this.colors.intern(colors);
      }
   }
}
