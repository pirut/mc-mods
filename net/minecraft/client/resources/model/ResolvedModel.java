package net.minecraft.client.resources.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemDisplayContext;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface ResolvedModel extends ModelDebugName {
   boolean DEFAULT_AMBIENT_OCCLUSION = true;
   UnbakedModel.GuiLight DEFAULT_GUI_LIGHT = UnbakedModel.GuiLight.SIDE;

   UnbakedModel wrapped();

   @Nullable
   ResolvedModel parent();

   static TextureSlots findTopTextureSlots(ResolvedModel model) {
      ResolvedModel resolvedModel = model;

      TextureSlots.Resolver resolver;
      for (resolver = new TextureSlots.Resolver(); resolvedModel != null; resolvedModel = resolvedModel.parent()) {
         resolver.addLast(resolvedModel.wrapped().textureSlots());
      }

      return resolver.resolve(model);
   }

   default TextureSlots getTopTextureSlots() {
      return findTopTextureSlots(this);
   }

   static boolean findTopAmbientOcclusion(ResolvedModel model) {
      while (model != null) {
         Boolean boolean_ = model.wrapped().ambientOcclusion();
         if (boolean_ != null) {
            return boolean_;
         }

         model = model.parent();
      }

      return true;
   }

   default boolean getTopAmbientOcclusion() {
      return findTopAmbientOcclusion(this);
   }

   static UnbakedModel.GuiLight findTopGuiLight(ResolvedModel model) {
      while (model != null) {
         UnbakedModel.GuiLight guiLight = model.wrapped().guiLight();
         if (guiLight != null) {
            return guiLight;
         }

         model = model.parent();
      }

      return DEFAULT_GUI_LIGHT;
   }

   default UnbakedModel.GuiLight getTopGuiLight() {
      return findTopGuiLight(this);
   }

   static UnbakedGeometry findTopGeometry(ResolvedModel model) {
      while (model != null) {
         UnbakedGeometry unbakedGeometry = model.wrapped().geometry();
         if (unbakedGeometry != null) {
            return unbakedGeometry;
         }

         model = model.parent();
      }

      return UnbakedGeometry.EMPTY;
   }

   default UnbakedGeometry getTopGeometry() {
      return findTopGeometry(this);
   }

   default QuadCollection bakeTopGeometry(TextureSlots textureSlots, ModelBaker modelBaker, ModelState modelState) {
      return this.getTopGeometry().bake(textureSlots, modelBaker, modelState, this);
   }

   static TextureAtlasSprite resolveParticleSprite(TextureSlots textureSlots, ModelBaker modelBaker, ModelDebugName debugName) {
      return modelBaker.sprites().resolveSlot(textureSlots, "particle", debugName);
   }

   default TextureAtlasSprite resolveParticleSprite(TextureSlots textureSlots, ModelBaker modelBaker) {
      return resolveParticleSprite(textureSlots, modelBaker, this);
   }

   static ItemTransform findTopTransform(ResolvedModel model, ItemDisplayContext displayContext) {
      while (model != null) {
         ItemTransforms itemTransforms = model.wrapped().transforms();
         if (itemTransforms != null) {
            ItemTransform itemTransform = itemTransforms.getTransform(displayContext);
            if (itemTransform != ItemTransform.NO_TRANSFORM) {
               return itemTransform;
            }
         }

         model = model.parent();
      }

      return ItemTransform.NO_TRANSFORM;
   }

   static ItemTransforms findTopTransforms(ResolvedModel model) {
      ItemTransform itemTransform = findTopTransform(model, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
      ItemTransform itemTransform2 = findTopTransform(model, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
      ItemTransform itemTransform3 = findTopTransform(model, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
      ItemTransform itemTransform4 = findTopTransform(model, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
      ItemTransform itemTransform5 = findTopTransform(model, ItemDisplayContext.HEAD);
      ItemTransform itemTransform6 = findTopTransform(model, ItemDisplayContext.GUI);
      ItemTransform itemTransform7 = findTopTransform(model, ItemDisplayContext.GROUND);
      ItemTransform itemTransform8 = findTopTransform(model, ItemDisplayContext.FIXED);
      ItemTransform itemTransform9 = findTopTransform(model, ItemDisplayContext.ON_SHELF);
      return new ItemTransforms(
         itemTransform, itemTransform2, itemTransform3, itemTransform4, itemTransform5, itemTransform6, itemTransform7, itemTransform8, itemTransform9
      );
   }

   default ItemTransforms getTopTransforms() {
      return findTopTransforms(this);
   }
}
