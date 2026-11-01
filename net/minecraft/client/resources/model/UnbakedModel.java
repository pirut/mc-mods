package net.minecraft.client.resources.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface UnbakedModel {
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

   @Environment(EnvType.CLIENT)
   public static enum GuiLight {
      FRONT("front"),
      SIDE("side");

      private final String name;

      private GuiLight(final String name) {
         this.name = name;
      }

      public static UnbakedModel.GuiLight getByName(String name) {
         for (UnbakedModel.GuiLight guiLight : values()) {
            if (guiLight.name.equals(name)) {
               return guiLight;
            }
         }

         throw new IllegalArgumentException("Invalid gui light: " + name);
      }

      public boolean lightLikeBlock() {
         return this == SIDE;
      }
   }
}
