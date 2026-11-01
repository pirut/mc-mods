package net.minecraft.client.resources;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

@Environment(EnvType.CLIENT)
public class LegacyStuffWrapper {
   @Deprecated
   public static int[] getPixels(ResourceManager manager, Identifier location) throws IOException {
      int[] var4;
      try (
         InputStream inputStream = manager.open(location);
         NativeImage nativeImage = NativeImage.read(inputStream);
      ) {
         var4 = nativeImage.makePixelArray();
      }

      return var4;
   }
}
