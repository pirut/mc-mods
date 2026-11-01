package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public final class MissingTextureAtlasSprite {
   private static final int MISSING_IMAGE_WIDTH = 16;
   private static final int MISSING_IMAGE_HEIGHT = 16;
   private static final String MISSING_TEXTURE_NAME = "missingno";
   private static final Identifier MISSING_TEXTURE_LOCATION = Identifier.withDefaultNamespace("missingno");

   public static NativeImage generateMissingImage() {
      return generateMissingImage(16, 16);
   }

   public static NativeImage generateMissingImage(int width, int height) {
      NativeImage nativeImage = new NativeImage(width, height, false);
      int i = -524040;

      for (int j = 0; j < height; j++) {
         for (int k = 0; k < width; k++) {
            if (j < height / 2 ^ k < width / 2) {
               nativeImage.setPixel(k, j, -524040);
            } else {
               nativeImage.setPixel(k, j, -16777216);
            }
         }
      }

      return nativeImage;
   }

   public static SpriteContents create() {
      NativeImage nativeImage = generateMissingImage(16, 16);
      return new SpriteContents(MISSING_TEXTURE_LOCATION, new FrameSize(16, 16), nativeImage);
   }

   public static Identifier getLocation() {
      return MISSING_TEXTURE_LOCATION;
   }
}
