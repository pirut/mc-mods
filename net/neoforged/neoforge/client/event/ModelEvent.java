package net.neoforged.neoforge.client.event;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.client.model.standalone.UnbakedStandaloneModel;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class ModelEvent extends Event {
   @Internal
   protected ModelEvent() {
   }

   public static class BakingCompleted extends ModelEvent implements IModBusEvent {
      private final ModelManager modelManager;
      private final ModelBakery.BakingResult bakingResult;
      private final ModelBakery modelBakery;

      @Internal
      public BakingCompleted(ModelManager modelManager, ModelBakery.BakingResult bakingResult, ModelBakery modelBakery) {
         this.modelManager = modelManager;
         this.bakingResult = new ModelBakery.BakingResult(
            bakingResult.missingModels(),
            Collections.unmodifiableMap(bakingResult.blockStateModels()),
            Collections.unmodifiableMap(bakingResult.itemStackModels()),
            Collections.unmodifiableMap(bakingResult.itemProperties()),
            bakingResult.standaloneModels().unmodifiable()
         );
         this.modelBakery = modelBakery;
      }

      public ModelManager getModelManager() {
         return this.modelManager;
      }

      public ModelBakery.BakingResult getBakingResult() {
         return this.bakingResult;
      }

      public ModelBakery getModelBakery() {
         return this.modelBakery;
      }
   }

   public static class ModifyBakingResult extends ModelEvent implements IModBusEvent {
      private final ModelBakery.BakingResult bakingResult;
      private final Function<Identifier, TextureAtlasSprite> textureGetter;
      private final ModelBakery modelBakery;

      @Internal
      public ModifyBakingResult(ModelBakery.BakingResult bakingResult, Function<Identifier, TextureAtlasSprite> textureGetter, ModelBakery modelBakery) {
         this.bakingResult = bakingResult;
         this.textureGetter = textureGetter;
         this.modelBakery = modelBakery;
      }

      public ModelBakery.BakingResult getBakingResult() {
         return this.bakingResult;
      }

      public Function<Identifier, TextureAtlasSprite> getTextureGetter() {
         return this.textureGetter;
      }

      public ModelBakery getModelBakery() {
         return this.modelBakery;
      }
   }

   public static class RegisterLoaders extends ModelEvent implements IModBusEvent {
      private final Map<Identifier, UnbakedModelLoader<?>> loaders;

      @Internal
      public RegisterLoaders(Map<Identifier, UnbakedModelLoader<?>> loaders) {
         this.loaders = loaders;
      }

      public void register(Identifier key, UnbakedModelLoader<?> loader) {
         Preconditions.checkArgument(!this.loaders.containsKey(key), "Unbaked model loader already registered: " + key);
         this.loaders.put(key, loader);
      }
   }

   public static class RegisterStandalone extends ModelEvent implements IModBusEvent {
      private final Map<StandaloneModelKey<?>, UnbakedStandaloneModel<?>> modelMap;

      @Internal
      public RegisterStandalone(Map<StandaloneModelKey<?>, UnbakedStandaloneModel<?>> modelMap) {
         this.modelMap = modelMap;
      }

      public <T> void register(StandaloneModelKey<T> modelKey, UnbakedStandaloneModel<T> baker) {
         this.modelMap.put(modelKey, baker);
      }
   }
}
