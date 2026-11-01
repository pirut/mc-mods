package net.neoforged.neoforge.client.event;

import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class ModelEvent extends Event {
   @Internal
   protected ModelEvent() {
   }

   public static class BakingCompleted extends ModelEvent implements IModBusEvent {
      private final ModelManager modelManager;
      private final Map<ModelResourceLocation, BakedModel> models;
      private final ModelBakery modelBakery;

      @Internal
      public BakingCompleted(ModelManager modelManager, Map<ModelResourceLocation, BakedModel> models, ModelBakery modelBakery) {
         this.modelManager = modelManager;
         this.models = models;
         this.modelBakery = modelBakery;
      }

      public ModelManager getModelManager() {
         return this.modelManager;
      }

      public Map<ModelResourceLocation, BakedModel> getModels() {
         return this.models;
      }

      public ModelBakery getModelBakery() {
         return this.modelBakery;
      }
   }

   public static class ModifyBakingResult extends ModelEvent implements IModBusEvent {
      private final Map<ModelResourceLocation, BakedModel> models;
      private final Function<Material, TextureAtlasSprite> textureGetter;
      private final ModelBakery modelBakery;

      @Internal
      public ModifyBakingResult(Map<ModelResourceLocation, BakedModel> models, Function<Material, TextureAtlasSprite> textureGetter, ModelBakery modelBakery) {
         this.models = models;
         this.textureGetter = textureGetter;
         this.modelBakery = modelBakery;
      }

      public Map<ModelResourceLocation, BakedModel> getModels() {
         return this.models;
      }

      public Function<Material, TextureAtlasSprite> getTextureGetter() {
         return this.textureGetter;
      }

      public ModelBakery getModelBakery() {
         return this.modelBakery;
      }
   }

   public static class RegisterAdditional extends ModelEvent implements IModBusEvent {
      private final Set<ModelResourceLocation> models;

      @Internal
      public RegisterAdditional(Set<ModelResourceLocation> models) {
         this.models = models;
      }

      public void register(ModelResourceLocation model) {
         Preconditions.checkArgument(model.getVariant().equals("standalone"), "Side-loaded models must use the 'standalone' variant");
         this.models.add(model);
      }
   }

   public static class RegisterGeometryLoaders extends ModelEvent implements IModBusEvent {
      private final Map<ResourceLocation, IGeometryLoader<?>> loaders;

      @Internal
      public RegisterGeometryLoaders(Map<ResourceLocation, IGeometryLoader<?>> loaders) {
         this.loaders = loaders;
      }

      public void register(ResourceLocation key, IGeometryLoader<?> loader) {
         Preconditions.checkArgument(!this.loaders.containsKey(key), "Geometry loader already registered: " + key);
         this.loaders.put(key, loader);
      }
   }
}
