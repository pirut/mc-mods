package com.mojang.blaze3d.systems;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class SamplerCache {
   private final GpuSampler[] samplers = new GpuSampler[32];

   public void initialize() {
      GpuDevice gpuDevice = RenderSystem.getDevice();
      if (AddressMode.values().length == 2 && FilterMode.values().length == 2) {
         for (AddressMode addressMode : AddressMode.values()) {
            for (AddressMode addressMode2 : AddressMode.values()) {
               for (FilterMode filterMode : FilterMode.values()) {
                  for (FilterMode filterMode2 : FilterMode.values()) {
                     for (boolean bl : new boolean[]{true, false}) {
                        this.samplers[encode(addressMode, addressMode2, filterMode, filterMode2, bl)] = gpuDevice.createSampler(
                           addressMode, addressMode2, filterMode, filterMode2, 1, bl ? OptionalDouble.empty() : OptionalDouble.of(0.0)
                        );
                     }
                  }
               }
            }
         }
      } else {
         throw new IllegalStateException("AddressMode and FilterMode enum sizes must be 2 - if you expanded them, please update SamplerCache");
      }
   }

   public GpuSampler getSampler(AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilter, FilterMode magFilter, boolean useMipmaps) {
      return this.samplers[encode(addressModeU, addressModeV, minFilter, magFilter, useMipmaps)];
   }

   public GpuSampler getClampToEdge(FilterMode filter) {
      return this.getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, filter, filter, false);
   }

   public GpuSampler getClampToEdge(FilterMode filter, boolean useMipmaps) {
      return this.getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, filter, filter, useMipmaps);
   }

   public GpuSampler getRepeat(FilterMode filter) {
      return this.getSampler(AddressMode.REPEAT, AddressMode.REPEAT, filter, filter, false);
   }

   public GpuSampler getRepeat(FilterMode filter, boolean useMipmaps) {
      return this.getSampler(AddressMode.REPEAT, AddressMode.REPEAT, filter, filter, useMipmaps);
   }

   public void close() {
      for (GpuSampler gpuSampler : this.samplers) {
         gpuSampler.close();
      }
   }

   @VisibleForTesting
   static int encode(AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilter, FilterMode magFilter, boolean useMipmaps) {
      int i = 0;
      i |= addressModeU.ordinal() & 1;
      i |= (addressModeV.ordinal() & 1) << 1;
      i |= (minFilter.ordinal() & 1) << 2;
      i |= (magFilter.ordinal() & 1) << 3;
      if (useMipmaps) {
         i |= 16;
      }

      return i;
   }
}
