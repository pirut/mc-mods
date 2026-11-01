package net.minecraft.client.gui.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record TextureSetup(
   @Nullable GpuTextureView texure0,
   @Nullable GpuTextureView texure1,
   @Nullable GpuTextureView texure2,
   @Nullable GpuSampler sampler0,
   @Nullable GpuSampler sampler1,
   @Nullable GpuSampler sampler2
) {
   private static final TextureSetup NO_TEXTURE_SETUP = new TextureSetup(null, null, null, null, null, null);
   private static int sortKeySeed;

   public static TextureSetup singleTexture(GpuTextureView p_423536_, GpuSampler p_455416_) {
      return new TextureSetup(p_423536_, null, null, p_455416_, null, null);
   }

   public static TextureSetup singleTextureWithLightmap(GpuTextureView p_423561_, GpuSampler p_456190_) {
      return new TextureSetup(
         p_423561_,
         null,
         Minecraft.getInstance().gameRenderer.lightTexture().getTextureView(),
         p_456190_,
         null,
         RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
      );
   }

   public static TextureSetup doubleTexture(GpuTextureView p_423619_, GpuSampler p_454859_, GpuTextureView p_423588_, GpuSampler p_454988_) {
      return new TextureSetup(p_423619_, p_423588_, null, p_454859_, p_454988_, null);
   }

   public static TextureSetup noTexture() {
      return NO_TEXTURE_SETUP;
   }

   public int getSortKey() {
      return SharedConstants.DEBUG_SHUFFLE_UI_RENDERING_ORDER ? this.hashCode() * (sortKeySeed + 1) : this.hashCode();
   }

   public static void updateSortKeySeed() {
      sortKeySeed = Math.round(100000.0F * (float)Math.random());
   }
}
