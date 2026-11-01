package com.mojang.realmsclient.util;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsTextureManager {
   private static final Map<String, RealmsTextureManager.RealmsTexture> TEXTURES = Maps.newHashMap();
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation TEMPLATE_ICON_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/presets/isles.png");

   public static ResourceLocation worldTemplate(String p_270945_, @Nullable String p_270612_) {
      return p_270612_ == null ? TEMPLATE_ICON_LOCATION : getTexture(p_270945_, p_270612_);
   }

   private static ResourceLocation getTexture(String p_90197_, String p_90198_) {
      RealmsTextureManager.RealmsTexture $$2 = TEXTURES.get(p_90197_);
      if ($$2 != null && $$2.image().equals(p_90198_)) {
         return $$2.textureId;
      } else {
         NativeImage $$3 = loadImage(p_90198_);
         if ($$3 == null) {
            ResourceLocation $$4 = MissingTextureAtlasSprite.getLocation();
            TEXTURES.put(p_90197_, new RealmsTextureManager.RealmsTexture(p_90198_, $$4));
            return $$4;
         } else {
            ResourceLocation $$5 = ResourceLocation.fromNamespaceAndPath("realms", "dynamic/" + p_90197_);
            Minecraft.getInstance().getTextureManager().register($$5, new DynamicTexture($$3));
            TEXTURES.put(p_90197_, new RealmsTextureManager.RealmsTexture(p_90198_, $$5));
            return $$5;
         }
      }
   }

   @Nullable
   private static NativeImage loadImage(String p_270725_) {
      byte[] $$1 = Base64.getDecoder().decode(p_270725_);
      ByteBuffer $$2 = MemoryUtil.memAlloc($$1.length);

      try {
         return NativeImage.read($$2.put($$1).flip());
      } catch (IOException var7) {
         LOGGER.warn("Failed to load world image: {}", p_270725_, var7);
      } finally {
         MemoryUtil.memFree($$2);
      }

      return null;
   }

   @OnlyIn(Dist.CLIENT)
   public record RealmsTexture(String image, ResourceLocation textureId) {
   }
}
