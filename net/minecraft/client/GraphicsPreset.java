package net.minecraft.client;

import com.mojang.blaze3d.GraphicsWorkarounds;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public enum GraphicsPreset implements StringRepresentable {
   FAST("fast", "options.graphics.fast"),
   FANCY("fancy", "options.graphics.fancy"),
   FABULOUS("fabulous", "options.graphics.fabulous"),
   CUSTOM("custom", "options.graphics.custom");

   private final String serializedName;
   private final String key;
   public static final Codec<GraphicsPreset> CODEC = StringRepresentable.fromEnum(GraphicsPreset::values);

   private GraphicsPreset(String p_454952_, String p_455702_) {
      this.serializedName = p_454952_;
      this.key = p_455702_;
   }

   @Override
   public String getSerializedName() {
      return this.serializedName;
   }

   public String getKey() {
      return this.key;
   }

   public void apply(Minecraft p_455289_) {
      OptionsSubScreen optionssubscreen = p_455289_.screen instanceof OptionsSubScreen ? (OptionsSubScreen)p_455289_.screen : null;
      GpuDevice gpudevice = RenderSystem.getDevice();
      switch (this) {
         case FAST:
            int k = 8;
            this.set(optionssubscreen, p_455289_.options.biomeBlendRadius(), 1);
            this.set(optionssubscreen, p_455289_.options.renderDistance(), 8);
            this.set(optionssubscreen, p_455289_.options.prioritizeChunkUpdates(), PrioritizeChunkUpdates.NONE);
            this.set(optionssubscreen, p_455289_.options.simulationDistance(), 6);
            this.set(optionssubscreen, p_455289_.options.ambientOcclusion(), false);
            this.set(optionssubscreen, p_455289_.options.cloudStatus(), CloudStatus.FAST);
            this.set(optionssubscreen, p_455289_.options.particles(), ParticleStatus.DECREASED);
            this.set(optionssubscreen, p_455289_.options.mipmapLevels(), 2);
            this.set(optionssubscreen, p_455289_.options.entityShadows(), false);
            this.set(optionssubscreen, p_455289_.options.entityDistanceScaling(), 0.75);
            this.set(optionssubscreen, p_455289_.options.menuBackgroundBlurriness(), 2);
            this.set(optionssubscreen, p_455289_.options.cloudRange(), 32);
            this.set(optionssubscreen, p_455289_.options.cutoutLeaves(), false);
            this.set(optionssubscreen, p_455289_.options.improvedTransparency(), false);
            this.set(optionssubscreen, p_455289_.options.weatherRadius(), 5);
            this.set(optionssubscreen, p_455289_.options.maxAnisotropyBit(), 1);
            this.set(optionssubscreen, p_455289_.options.textureFiltering(), TextureFilteringMethod.NONE);
            break;
         case FANCY:
            int j = 16;
            this.set(optionssubscreen, p_455289_.options.biomeBlendRadius(), 2);
            this.set(optionssubscreen, p_455289_.options.renderDistance(), 16);
            this.set(optionssubscreen, p_455289_.options.prioritizeChunkUpdates(), PrioritizeChunkUpdates.PLAYER_AFFECTED);
            this.set(optionssubscreen, p_455289_.options.simulationDistance(), 12);
            this.set(optionssubscreen, p_455289_.options.ambientOcclusion(), true);
            this.set(optionssubscreen, p_455289_.options.cloudStatus(), CloudStatus.FANCY);
            this.set(optionssubscreen, p_455289_.options.particles(), ParticleStatus.ALL);
            this.set(optionssubscreen, p_455289_.options.mipmapLevels(), 4);
            this.set(optionssubscreen, p_455289_.options.entityShadows(), true);
            this.set(optionssubscreen, p_455289_.options.entityDistanceScaling(), 1.0);
            this.set(optionssubscreen, p_455289_.options.menuBackgroundBlurriness(), 5);
            this.set(optionssubscreen, p_455289_.options.cloudRange(), 64);
            this.set(optionssubscreen, p_455289_.options.cutoutLeaves(), true);
            this.set(optionssubscreen, p_455289_.options.improvedTransparency(), false);
            this.set(optionssubscreen, p_455289_.options.weatherRadius(), 10);
            this.set(optionssubscreen, p_455289_.options.maxAnisotropyBit(), 1);
            this.set(optionssubscreen, p_455289_.options.textureFiltering(), TextureFilteringMethod.RGSS);
            break;
         case FABULOUS:
            int i = 32;
            this.set(optionssubscreen, p_455289_.options.biomeBlendRadius(), 2);
            this.set(optionssubscreen, p_455289_.options.renderDistance(), 32);
            this.set(optionssubscreen, p_455289_.options.prioritizeChunkUpdates(), PrioritizeChunkUpdates.PLAYER_AFFECTED);
            this.set(optionssubscreen, p_455289_.options.simulationDistance(), 12);
            this.set(optionssubscreen, p_455289_.options.ambientOcclusion(), true);
            this.set(optionssubscreen, p_455289_.options.cloudStatus(), CloudStatus.FANCY);
            this.set(optionssubscreen, p_455289_.options.particles(), ParticleStatus.ALL);
            this.set(optionssubscreen, p_455289_.options.mipmapLevels(), 4);
            this.set(optionssubscreen, p_455289_.options.entityShadows(), true);
            this.set(optionssubscreen, p_455289_.options.entityDistanceScaling(), 1.25);
            this.set(optionssubscreen, p_455289_.options.menuBackgroundBlurriness(), 5);
            this.set(optionssubscreen, p_455289_.options.cloudRange(), 128);
            this.set(optionssubscreen, p_455289_.options.cutoutLeaves(), true);
            this.set(optionssubscreen, p_455289_.options.improvedTransparency(), Util.getPlatform() != Util.OS.OSX);
            this.set(optionssubscreen, p_455289_.options.weatherRadius(), 10);
            this.set(optionssubscreen, p_455289_.options.maxAnisotropyBit(), 2);
            if (GraphicsWorkarounds.get(gpudevice).isAmd()) {
               this.set(optionssubscreen, p_455289_.options.textureFiltering(), TextureFilteringMethod.RGSS);
            } else {
               this.set(optionssubscreen, p_455289_.options.textureFiltering(), TextureFilteringMethod.ANISOTROPIC);
            }
      }
   }

   <T> void set(@Nullable OptionsSubScreen p_455228_, OptionInstance<T> p_454688_, T p_455444_) {
      if (p_454688_.get() != p_455444_) {
         p_454688_.set(p_455444_);
         if (p_455228_ != null) {
            p_455228_.resetOption(p_454688_);
         }
      }
   }
}
