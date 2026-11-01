package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.UnbakedModelExtension;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface UnbakedModel extends UnbakedModelExtension {
   String PARTICLE_TEXTURE_REFERENCE = "particle";

   @Nullable
   default Boolean ambientOcclusion() {
      return null;
   }

   @Nullable
   default UnbakedModel.GuiLight guiLight() {
      return null;
   }

   @Nullable
   default ItemTransforms transforms() {
      return null;
   }

   default TextureSlots.Data textureSlots() {
      return TextureSlots.Data.EMPTY;
   }

   @Nullable
   default UnbakedGeometry geometry() {
      return null;
   }

   @Nullable
   default Identifier parent() {
      return null;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum GuiLight {
      FRONT("front"),
      SIDE("side");

      private final String name;

      private GuiLight(String p_387953_) {
         this.name = p_387953_;
      }

      public static UnbakedModel.GuiLight getByName(String p_388510_) {
         for (UnbakedModel.GuiLight unbakedmodel$guilight : values()) {
            if (unbakedmodel$guilight.name.equals(p_388510_)) {
               return unbakedmodel$guilight;
            }
         }

         throw new IllegalArgumentException("Invalid gui light: " + p_388510_);
      }

      public boolean lightLikeBlock() {
         return this == SIDE;
      }

      public String getSerializedName() {
         return this.name;
      }
   }
}
