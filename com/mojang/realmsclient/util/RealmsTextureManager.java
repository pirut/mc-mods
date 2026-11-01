package com.mojang.realmsclient.util;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class RealmsTextureManager {
   private static final Map<String, RealmsTextureManager.RealmsTexture> TEXTURES = Maps.newHashMap();
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Identifier TEMPLATE_ICON_LOCATION = Identifier.withDefaultNamespace("textures/gui/presets/isles.png");

   public static Identifier worldTemplate(String key, @Nullable String image) {
      return image == null ? TEMPLATE_ICON_LOCATION : getTexture(key, image);
   }

   private static Identifier getTexture(String key, String image) {
      RealmsTextureManager.RealmsTexture realmsTexture = TEXTURES.get(key);
      if (realmsTexture != null && realmsTexture.image().equals(image)) {
         return realmsTexture.textureId;
      } else {
         NativeImage nativeImage = loadImage(image);
         if (nativeImage == null) {
            Identifier identifier = MissingTextureAtlasSprite.getLocation();
            TEXTURES.put(key, new RealmsTextureManager.RealmsTexture(image, identifier));
            return identifier;
         } else {
            Identifier identifier = Identifier.fromNamespaceAndPath("realms", "dynamic/" + key);
            Minecraft.getInstance().getTextureManager().register(identifier, new DynamicTexture(identifier::toString, nativeImage));
            TEXTURES.put(key, new RealmsTextureManager.RealmsTexture(image, identifier));
            return identifier;
         }
      }
   }

   @Nullable
   private static NativeImage loadImage(String base64Image) {
      byte[] bs = Base64.getDecoder().decode(base64Image);
      ByteBuffer byteBuffer = MemoryUtil.memAlloc(bs.length);

      try {
         return NativeImage.read(byteBuffer.put(bs).flip());
      } catch (IOException var7) {
         LOGGER.warn("Failed to load world image: {}", base64Image, var7);
      } finally {
         MemoryUtil.memFree(byteBuffer);
      }

      return null;
   }

   @Environment(EnvType.CLIENT)
   public record RealmsTexture(String image, Identifier textureId) {
   }
}
