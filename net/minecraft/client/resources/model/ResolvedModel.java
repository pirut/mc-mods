package net.minecraft.client.resources.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.ResolvedModelExtension;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface ResolvedModel extends ModelDebugName, ResolvedModelExtension {
   boolean DEFAULT_AMBIENT_OCCLUSION = true;
   UnbakedModel.GuiLight DEFAULT_GUI_LIGHT = UnbakedModel.GuiLight.SIDE;

   UnbakedModel wrapped();

   @Nullable
   ResolvedModel parent();

   static TextureSlots findTopTextureSlots(ResolvedModel p_405472_) {
      ResolvedModel resolvedmodel = p_405472_;

      TextureSlots.Resolver textureslots$resolver;
      for (textureslots$resolver = new TextureSlots.Resolver(); resolvedmodel != null; resolvedmodel = resolvedmodel.parent()) {
         textureslots$resolver.addLast(resolvedmodel.wrapped().textureSlots());
      }

      return textureslots$resolver.resolve(p_405472_);
   }

   default TextureSlots getTopTextureSlots() {
      return findTopTextureSlots(this);
   }

   static boolean findTopAmbientOcclusion(ResolvedModel p_404692_) {
      while (p_404692_ != null) {
         Boolean obool = p_404692_.wrapped().ambientOcclusion();
         if (obool != null) {
            return obool;
         }

         p_404692_ = p_404692_.parent();
      }

      return true;
   }

   default boolean getTopAmbientOcclusion() {
      return findTopAmbientOcclusion(this);
   }

   static UnbakedModel.GuiLight findTopGuiLight(ResolvedModel p_405791_) {
      while (p_405791_ != null) {
         UnbakedModel.GuiLight unbakedmodel$guilight = p_405791_.wrapped().guiLight();
         if (unbakedmodel$guilight != null) {
            return unbakedmodel$guilight;
         }

         p_405791_ = p_405791_.parent();
      }

      return DEFAULT_GUI_LIGHT;
   }

   default UnbakedModel.GuiLight getTopGuiLight() {
      return findTopGuiLight(this);
   }

   static UnbakedGeometry findTopGeometry(ResolvedModel p_405270_) {
      while (p_405270_ != null) {
         UnbakedGeometry unbakedgeometry = p_405270_.wrapped().geometry();
         if (unbakedgeometry != null) {
            return unbakedgeometry;
         }

         p_405270_ = p_405270_.parent();
      }

      return UnbakedGeometry.EMPTY;
   }

   default UnbakedGeometry getTopGeometry() {
      return findTopGeometry(this);
   }

   default QuadCollection bakeTopGeometry(TextureSlots p_405784_, ModelBaker p_405450_, ModelState p_405844_) {
      return this.getTopGeometry().bake(p_405784_, p_405450_, p_405844_, this, this.getTopAdditionalProperties());
   }

   static TextureAtlasSprite resolveParticleSprite(TextureSlots p_404698_, ModelBaker p_405533_, ModelDebugName p_405824_) {
      return p_405533_.sprites().resolveSlot(p_404698_, "particle", p_405824_);
   }

   default TextureAtlasSprite resolveParticleSprite(TextureSlots p_405126_, ModelBaker p_404926_) {
      return resolveParticleSprite(p_405126_, p_404926_, this);
   }

   static ItemTransform findTopTransform(ResolvedModel p_404657_, ItemDisplayContext p_405330_) {
      while (p_404657_ != null) {
         ItemTransforms itemtransforms = p_404657_.wrapped().transforms();
         if (itemtransforms != null) {
            ItemTransform itemtransform = itemtransforms.getTransform(p_405330_);
            if (itemtransform != ItemTransform.NO_TRANSFORM) {
               return itemtransform;
            }
         }

         p_404657_ = p_404657_.parent();
      }

      return ItemTransform.NO_TRANSFORM;
   }

   static ItemTransforms findTopTransforms(ResolvedModel p_404681_) {
      ItemTransform itemtransform = findTopTransform(p_404681_, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
      ItemTransform itemtransform1 = findTopTransform(p_404681_, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
      ItemTransform itemtransform2 = findTopTransform(p_404681_, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
      ItemTransform itemtransform3 = findTopTransform(p_404681_, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
      ItemTransform itemtransform4 = findTopTransform(p_404681_, ItemDisplayContext.HEAD);
      ItemTransform itemtransform5 = findTopTransform(p_404681_, ItemDisplayContext.GUI);
      ItemTransform itemtransform6 = findTopTransform(p_404681_, ItemDisplayContext.GROUND);
      ItemTransform itemtransform7 = findTopTransform(p_404681_, ItemDisplayContext.FIXED);
      ItemTransform itemtransform8 = findTopTransform(p_404681_, ItemDisplayContext.ON_SHELF);
      Builder<ItemDisplayContext, ItemTransform> moddedTransforms = ImmutableMap.builder();

      for (ItemDisplayContext context : ItemDisplayContext.values()) {
         if (context.isModded()) {
            ItemTransform transform = findTopTransform(p_404681_, context);
            if (transform != ItemTransform.NO_TRANSFORM) {
               moddedTransforms.put(context, transform);
            }
         }
      }

      return new ItemTransforms(
         itemtransform,
         itemtransform1,
         itemtransform2,
         itemtransform3,
         itemtransform4,
         itemtransform5,
         itemtransform6,
         itemtransform7,
         itemtransform8,
         moddedTransforms.build()
      );
   }

   default ItemTransforms getTopTransforms() {
      return findTopTransforms(this);
   }
}
