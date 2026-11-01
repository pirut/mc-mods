package com.mojang.blaze3d.textures;

import com.mojang.blaze3d.DontObfuscate;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public abstract class GpuTexture implements AutoCloseable {
   public static final int USAGE_COPY_DST = 1;
   public static final int USAGE_COPY_SRC = 2;
   public static final int USAGE_TEXTURE_BINDING = 4;
   public static final int USAGE_RENDER_ATTACHMENT = 8;
   public static final int USAGE_CUBEMAP_COMPATIBLE = 16;
   public static final int RESERVED_USAGE_BITS = -16777216;
   private final TextureFormat format;
   private final int width;
   private final int height;
   private final int depthOrLayers;
   private final int mipLevels;
   @GpuTexture.Usage
   private final int usage;
   private final String label;

   public GpuTexture(@GpuTexture.Usage int p_404771_, String p_405873_, TextureFormat p_405456_, int p_405638_, int p_404958_, int p_419943_, int p_423664_) {
      this.usage = p_404771_;
      this.label = p_405873_;
      this.format = p_405456_;
      this.width = p_405638_;
      this.height = p_404958_;
      this.depthOrLayers = p_419943_;
      this.mipLevels = p_423664_;
   }

   public int getWidth(int p_405560_) {
      return this.width >> p_405560_;
   }

   public int getHeight(int p_404653_) {
      return this.height >> p_404653_;
   }

   public int getDepthOrLayers() {
      return this.depthOrLayers;
   }

   public int getMipLevels() {
      return this.mipLevels;
   }

   public TextureFormat getFormat() {
      return this.format;
   }

   @GpuTexture.Usage
   public int usage() {
      return this.usage;
   }

   public String getLabel() {
      return this.label;
   }

   @Override
   public abstract void close();

   public abstract boolean isClosed();

   @Retention(RetentionPolicy.CLASS)
   @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.TYPE_USE})
   @OnlyIn(Dist.CLIENT)
   public @interface Usage {
   }
}
