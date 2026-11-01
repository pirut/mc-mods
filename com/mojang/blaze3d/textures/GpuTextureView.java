package com.mojang.blaze3d.textures;

import com.mojang.blaze3d.DontObfuscate;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public abstract class GpuTextureView implements AutoCloseable {
   private final GpuTexture texture;
   private final int baseMipLevel;
   private final int mipLevels;

   public GpuTextureView(GpuTexture p_423547_, int p_423447_, int p_423646_) {
      this.texture = p_423547_;
      this.baseMipLevel = p_423447_;
      this.mipLevels = p_423646_;
   }

   @Override
   public abstract void close();

   public GpuTexture texture() {
      return this.texture;
   }

   public int baseMipLevel() {
      return this.baseMipLevel;
   }

   public int mipLevels() {
      return this.mipLevels;
   }

   public int getWidth(int p_423434_) {
      return this.texture.getWidth(p_423434_ + this.baseMipLevel);
   }

   public int getHeight(int p_423457_) {
      return this.texture.getHeight(p_423457_ + this.baseMipLevel);
   }

   public abstract boolean isClosed();
}
