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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ModelDiscovery {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Object2ObjectMap<Identifier, ModelDiscovery.ModelWrapper> modelWrappers = new Object2ObjectOpenHashMap();
   private final ModelDiscovery.ModelWrapper missingModel;
   private final Object2ObjectFunction<Identifier, ModelDiscovery.ModelWrapper> uncachedResolver;
   private final ResolvableModel.Resolver resolver;
   private final Queue<ModelDiscovery.ModelWrapper> parentDiscoveryQueue = new ArrayDeque<>();

   public ModelDiscovery(Map<Identifier, UnbakedModel> inputModels, UnbakedModel missingModel) {
      this.missingModel = new ModelDiscovery.ModelWrapper(MissingBlockModel.LOCATION, missingModel, true);
      this.modelWrappers.put(MissingBlockModel.LOCATION, this.missingModel);
      this.uncachedResolver = object -> {
         Identifier identifier = (Identifier)object;
         UnbakedModel unbakedModel = inputModels.get(identifier);
         if (unbakedModel == null) {
            LOGGER.warn("Missing block model: {}", identifier);
            return this.missingModel;
         } else {
            return this.createAndQueueWrapper(identifier, unbakedModel);
         }
      };
      this.resolver = this::getOrCreateModel;
   }

   private static boolean isRoot(UnbakedModel model) {
      return model.parent() == null;
   }

   private ModelDiscovery.ModelWrapper getOrCreateModel(Identifier location) {
      return (ModelDiscovery.ModelWrapper)this.modelWrappers.computeIfAbsent(location, this.uncachedResolver);
   }

   private ModelDiscovery.ModelWrapper createAndQueueWrapper(Identifier id, UnbakedModel model) {
      boolean bl = isRoot(model);
      ModelDiscovery.ModelWrapper modelWrapper = new ModelDiscovery.ModelWrapper(id, model, bl);
      if (!bl) {
         this.parentDiscoveryQueue.add(modelWrapper);
      }

      return modelWrapper;
   }

   public void addRoot(ResolvableModel model) {
      model.resolveDependencies(this.resolver);
   }

   public void addSpecialModel(Identifier id, UnbakedModel model) {
      if (!isRoot(model)) {
         LOGGER.warn("Trying to add non-root special model {}, ignoring", id);
      } else {
         ModelDiscovery.ModelWrapper modelWrapper = (ModelDiscovery.ModelWrapper)this.modelWrappers.put(id, this.createAndQueueWrapper(id, model));
         if (modelWrapper != null) {
            LOGGER.warn("Duplicate special model {}", id);
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
      this.modelWrappers.forEach((identifier, modelWrapper) -> {
         if (modelWrapper.valid) {
            builder.put(identifier, modelWrapper);
         } else {
            LOGGER.warn("Model {} ignored due to cyclic dependency", identifier);
         }
      });
      return builder.build();
   }

   private void discoverDependencies(List<ModelDiscovery.ModelWrapper> wrappers) {
      ModelDiscovery.ModelWrapper modelWrapper;
      while ((modelWrapper = this.parentDiscoveryQueue.poll()) != null) {
         Identifier identifier = Objects.requireNonNull(modelWrapper.wrapped.parent());
         ModelDiscovery.ModelWrapper modelWrapper2 = this.getOrCreateModel(identifier);
         modelWrapper.parent = modelWrapper2;
         if (modelWrapper2.valid) {
            modelWrapper.valid = true;
         } else {
            wrappers.add(modelWrapper);
         }
      }
   }

   private static void propagateValidity(List<ModelDiscovery.ModelWrapper> wrappers) {
      boolean bl = true;

      while (bl) {
         bl = false;
         Iterator<ModelDiscovery.ModelWrapper> iterator = wrappers.iterator();

         while (iterator.hasNext()) {
            ModelDiscovery.ModelWrapper modelWrapper = iterator.next();
            if (Objects.requireNonNull(modelWrapper.parent).valid) {
               modelWrapper.valid = true;
               iterator.remove();
               bl = true;
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   static class ModelWrapper implements ResolvedModel {
      private static final ModelDiscovery.Slot<Boolean> KEY_AMBIENT_OCCLUSION = slot(0);
      private static final ModelDiscovery.Slot<UnbakedModel.GuiLight> KEY_GUI_LIGHT = slot(1);
      private static final ModelDiscovery.Slot<UnbakedGeometry> KEY_GEOMETRY = slot(2);
      private static final ModelDiscovery.Slot<ItemTransforms> KEY_TRANSFORMS = slot(3);
      private static final ModelDiscovery.Slot<TextureSlots> KEY_TEXTURE_SLOTS = slot(4);
      private static final ModelDiscovery.Slot<TextureAtlasSprite> KEY_PARTICLE_SPRITE = slot(5);
      private static final ModelDiscovery.Slot<QuadCollection> KEY_DEFAULT_GEOMETRY = slot(6);
      private static final int SLOT_COUNT = 7;
      private final Identifier id;
      boolean valid;
      @Nullable
      ModelDiscovery.ModelWrapper parent;
      final UnbakedModel wrapped;
      private final AtomicReferenceArray<Object> fixedSlots = new AtomicReferenceArray<>(7);
      private final Map<ModelState, QuadCollection> modelBakeCache = new ConcurrentHashMap<>();

      private static <T> ModelDiscovery.Slot<T> slot(int index) {
         Objects.checkIndex(index, 7);
         return new ModelDiscovery.Slot<>(index);
      }

      ModelWrapper(Identifier id, UnbakedModel wrapped, boolean valid) {
         this.id = id;
         this.wrapped = wrapped;
         this.valid = valid;
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
      private <T> T getSlot(ModelDiscovery.Slot<T> slot) {
         return (T)this.fixedSlots.get(slot.index);
      }

      private <T> T updateSlot(ModelDiscovery.Slot<T> slot, T value) {
         T object = (T)this.fixedSlots.compareAndExchange(slot.index, null, value);
         return object == null ? value : object;
      }

      private <T> T getSimpleProperty(ModelDiscovery.Slot<T> slot, Function<ResolvedModel, T> propertyGetter) {
         T object = this.getSlot(slot);
         return object != null ? object : this.updateSlot(slot, propertyGetter.apply(this));
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
      public TextureAtlasSprite resolveParticleSprite(TextureSlots textureSlots, ModelBaker modelBaker) {
         TextureAtlasSprite textureAtlasSprite = this.getSlot(KEY_PARTICLE_SPRITE);
         return textureAtlasSprite != null
            ? textureAtlasSprite
            : this.updateSlot(KEY_PARTICLE_SPRITE, ResolvedModel.resolveParticleSprite(textureSlots, modelBaker, this));
      }

      private QuadCollection bakeDefaultState(TextureSlots textureSlots, ModelBaker modelBaker, ModelState modelState) {
         QuadCollection quadCollection = this.getSlot(KEY_DEFAULT_GEOMETRY);
         return quadCollection != null
            ? quadCollection
            : this.updateSlot(KEY_DEFAULT_GEOMETRY, this.getTopGeometry().bake(textureSlots, modelBaker, modelState, this));
      }

      @Override
      public QuadCollection bakeTopGeometry(TextureSlots textureSlots, ModelBaker modelBaker, ModelState modelState) {
         return modelState == BlockModelRotation.IDENTITY
            ? this.bakeDefaultState(textureSlots, modelBaker, modelState)
            : this.modelBakeCache.computeIfAbsent(modelState, modelStatex -> {
               UnbakedGeometry unbakedGeometry = this.getTopGeometry();
               return unbakedGeometry.bake(textureSlots, modelBaker, modelStatex, this);
            });
      }
   }

   @Environment(EnvType.CLIENT)
   record Slot<T>(int index) {
   }
}
