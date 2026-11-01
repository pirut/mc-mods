package net.minecraft.client.resources.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectFunction;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.ResolvedModelExtension;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ModelDiscovery {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Object2ObjectMap<Identifier, ModelDiscovery.ModelWrapper> modelWrappers = new Object2ObjectOpenHashMap();
   private final ModelDiscovery.ModelWrapper missingModel;
   private final Object2ObjectFunction<Identifier, ModelDiscovery.ModelWrapper> uncachedResolver;
   private final ResolvableModel.Resolver resolver;
   private final Queue<ModelDiscovery.ModelWrapper> parentDiscoveryQueue = new ArrayDeque<>();

   public ModelDiscovery(Map<Identifier, UnbakedModel> p_360750_, UnbakedModel p_365355_) {
      this.missingModel = new ModelDiscovery.ModelWrapper(MissingBlockModel.LOCATION, p_365355_, true);
      this.modelWrappers.put(MissingBlockModel.LOCATION, this.missingModel);
      this.uncachedResolver = p_465779_ -> {
         Identifier identifier = (Identifier)p_465779_;
         UnbakedModel unbakedmodel = p_360750_.get(identifier);
         if (unbakedmodel == null) {
            LOGGER.warn("Missing block model: {}", identifier);
            return this.missingModel;
         } else {
            return this.createAndQueueWrapper(identifier, unbakedmodel);
         }
      };
      this.resolver = this::getOrCreateModel;
   }

   private static boolean isRoot(UnbakedModel p_405616_) {
      return p_405616_.parent() == null;
   }

   private ModelDiscovery.ModelWrapper getOrCreateModel(Identifier p_467971_) {
      ModelDiscovery.ModelWrapper wrapper = (ModelDiscovery.ModelWrapper)this.modelWrappers.get(p_467971_);
      if (wrapper == null) {
         wrapper = (ModelDiscovery.ModelWrapper)this.uncachedResolver.get(p_467971_);
         this.modelWrappers.put(p_467971_, wrapper);
      }

      return wrapper;
   }

   private ModelDiscovery.ModelWrapper createAndQueueWrapper(Identifier p_468169_, UnbakedModel p_404997_) {
      boolean flag = isRoot(p_404997_);
      ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper = new ModelDiscovery.ModelWrapper(p_468169_, p_404997_, flag);
      if (!flag) {
         this.parentDiscoveryQueue.add(modeldiscovery$modelwrapper);
      }

      p_404997_.resolveDependencies(this.resolver);
      return modeldiscovery$modelwrapper;
   }

   public void addRoot(ResolvableModel p_388596_) {
      p_388596_.resolveDependencies(this.resolver);
   }

   public void addSpecialModel(Identifier p_469822_, UnbakedModel p_405251_) {
      if (!isRoot(p_405251_)) {
         LOGGER.warn("Trying to add non-root special model {}, ignoring", p_469822_);
      } else {
         ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper = (ModelDiscovery.ModelWrapper)this.modelWrappers
            .put(p_469822_, this.createAndQueueWrapper(p_469822_, p_405251_));
         if (modeldiscovery$modelwrapper != null) {
            LOGGER.warn("Duplicate special model {}", p_469822_);
         }
      }
   }

   public ResolvedModel missingModel() {
      return this.missingModel;
   }

   public Map<Identifier, ResolvedModel> resolve() {
      List<ModelDiscovery.ModelWrapper> list = new ArrayList<>();
      this.discoverDependencies(list);
      propagateValidity(list);
      Builder<Identifier, ResolvedModel> builder = ImmutableMap.builder();
      this.modelWrappers.forEach((p_469258_, p_404133_) -> {
         if (p_404133_.valid) {
            builder.put(p_469258_, p_404133_);
         } else {
            LOGGER.warn("Model {} ignored due to cyclic dependency", p_469258_);
         }
      });
      return builder.build();
   }

   private void discoverDependencies(List<ModelDiscovery.ModelWrapper> p_405689_) {
      ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper;
      while ((modeldiscovery$modelwrapper = this.parentDiscoveryQueue.poll()) != null) {
         Identifier identifier = Objects.requireNonNull(modeldiscovery$modelwrapper.wrapped.parent());
         ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper1 = this.getOrCreateModel(identifier);
         modeldiscovery$modelwrapper.parent = modeldiscovery$modelwrapper1;
         if (modeldiscovery$modelwrapper1.valid) {
            modeldiscovery$modelwrapper.valid = true;
         } else {
            p_405689_.add(modeldiscovery$modelwrapper);
         }
      }
   }

   private static void propagateValidity(List<ModelDiscovery.ModelWrapper> p_405440_) {
      boolean flag = true;

      while (flag) {
         flag = false;
         Iterator<ModelDiscovery.ModelWrapper> iterator = p_405440_.iterator();

         while (iterator.hasNext()) {
            ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper = iterator.next();
            if (Objects.requireNonNull(modeldiscovery$modelwrapper.parent).valid) {
               modeldiscovery$modelwrapper.valid = true;
               iterator.remove();
               flag = true;
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class ModelWrapper implements ResolvedModel {
      private static final ModelDiscovery.Slot<Boolean> KEY_AMBIENT_OCCLUSION = slot(0);
      private static final ModelDiscovery.Slot<UnbakedModel.GuiLight> KEY_GUI_LIGHT = slot(1);
      private static final ModelDiscovery.Slot<UnbakedGeometry> KEY_GEOMETRY = slot(2);
      private static final ModelDiscovery.Slot<ItemTransforms> KEY_TRANSFORMS = slot(3);
      private static final ModelDiscovery.Slot<TextureSlots> KEY_TEXTURE_SLOTS = slot(4);
      private static final ModelDiscovery.Slot<TextureAtlasSprite> KEY_PARTICLE_SPRITE = slot(5);
      private static final ModelDiscovery.Slot<QuadCollection> KEY_DEFAULT_GEOMETRY = slot(6);
      private static final ModelDiscovery.Slot<ContextMap> KEY_ADDITIONAL_PROPERTIES = slot(7);
      private static final int SLOT_COUNT = 8;
      private final Identifier id;
      boolean valid;
      @Nullable
      ModelDiscovery.ModelWrapper parent;
      final UnbakedModel wrapped;
      private final AtomicReferenceArray<Object> fixedSlots = new AtomicReferenceArray<>(8);
      private final Map<ModelState, QuadCollection> modelBakeCache = new ConcurrentHashMap<>();

      private static <T> ModelDiscovery.Slot<T> slot(int p_405746_) {
         Objects.checkIndex(p_405746_, 8);
         return new ModelDiscovery.Slot<>(p_405746_);
      }

      ModelWrapper(Identifier p_469735_, UnbakedModel p_404712_, boolean p_404729_) {
         this.id = p_469735_;
         this.wrapped = p_404712_;
         this.valid = p_404729_;
      }

      @Override
      public UnbakedModel wrapped() {
         return this.wrapped;
      }

      @Nullable
      @Override
      public ResolvedModel parent() {
         return this.parent;
      }

      @Override
      public String debugName() {
         return this.id.toString();
      }

      @Nullable
      private <T> T getSlot(ModelDiscovery.Slot<T> p_405631_) {
         return (T)this.fixedSlots.get(p_405631_.index);
      }

      private <T> T updateSlot(ModelDiscovery.Slot<T> p_404819_, T p_404859_) {
         T t = (T)this.fixedSlots.compareAndExchange(p_404819_.index, null, p_404859_);
         return t == null ? p_404859_ : t;
      }

      private <T> T getSimpleProperty(ModelDiscovery.Slot<T> p_405509_, Function<ResolvedModel, T> p_405257_) {
         T t = this.getSlot(p_405509_);
         return t != null ? t : this.updateSlot(p_405509_, p_405257_.apply(this));
      }

      @Override
      public boolean getTopAmbientOcclusion() {
         return this.getSimpleProperty(KEY_AMBIENT_OCCLUSION, ResolvedModel::findTopAmbientOcclusion);
      }

      @Override
      public UnbakedModel.GuiLight getTopGuiLight() {
         return this.getSimpleProperty(KEY_GUI_LIGHT, ResolvedModel::findTopGuiLight);
      }

      @Override
      public ItemTransforms getTopTransforms() {
         return this.getSimpleProperty(KEY_TRANSFORMS, ResolvedModel::findTopTransforms);
      }

      @Override
      public UnbakedGeometry getTopGeometry() {
         return this.getSimpleProperty(KEY_GEOMETRY, ResolvedModel::findTopGeometry);
      }

      @Override
      public TextureSlots getTopTextureSlots() {
         return this.getSimpleProperty(KEY_TEXTURE_SLOTS, ResolvedModel::findTopTextureSlots);
      }

      @Override
      public TextureAtlasSprite resolveParticleSprite(TextureSlots p_405019_, ModelBaker p_405134_) {
         TextureAtlasSprite textureatlassprite = this.getSlot(KEY_PARTICLE_SPRITE);
         return textureatlassprite != null
            ? textureatlassprite
            : this.updateSlot(KEY_PARTICLE_SPRITE, ResolvedModel.resolveParticleSprite(p_405019_, p_405134_, this));
      }

      private QuadCollection bakeDefaultState(TextureSlots p_405546_, ModelBaker p_404641_, ModelState p_405363_) {
         QuadCollection quadcollection = this.getSlot(KEY_DEFAULT_GEOMETRY);
         return quadcollection != null
            ? quadcollection
            : this.updateSlot(KEY_DEFAULT_GEOMETRY, this.getTopGeometry().bake(p_405546_, p_404641_, p_405363_, this, this.getTopAdditionalProperties()));
      }

      @Override
      public QuadCollection bakeTopGeometry(TextureSlots p_405587_, ModelBaker p_405166_, ModelState p_405646_) {
         return p_405646_ == BlockModelRotation.IDENTITY
            ? this.bakeDefaultState(p_405587_, p_405166_, p_405646_)
            : this.modelBakeCache.computeIfAbsent(p_405646_, p_405723_ -> {
               UnbakedGeometry unbakedgeometry = this.getTopGeometry();
               return unbakedgeometry.bake(p_405587_, p_405166_, p_405723_, this, this.getTopAdditionalProperties());
            });
      }

      @Override
      public ContextMap getTopAdditionalProperties() {
         return this.getSimpleProperty(KEY_ADDITIONAL_PROPERTIES, ResolvedModelExtension::findTopAdditionalProperties);
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Slot<T>(int index) {
   }
}
