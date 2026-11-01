package com.mojang.blaze3d.systems;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import java.util.OptionalDouble;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SamplerCache {
   private final GpuSampler[] samplers = new GpuSampler[32];

   public void initialize() {
      GpuDevice gpudevice = RenderSystem.getDevice();
      if (AddressMode.values().length == 2 && FilterMode.values().length == 2) {
         for (AddressMode addressmode : AddressMode.values()) {
            for (AddressMode addressmode1 : AddressMode.values()) {
               for (FilterMode filtermode : FilterMode.values()) {
                  for (FilterMode filtermode1 : FilterMode.values()) {
                     for (boolean flag : new boolean[]{true, false}) {
                        this.samplers[encode(addressmode, addressmode1, filtermode, filtermode1, flag)] = gpudevice.createSampler(
                           addressmode, addressmode1, filtermode, filtermode1, 1, flag ? OptionalDouble.empty() : OptionalDouble.of(0.0)
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

   public GpuSampler getSampler(AddressMode p_455241_, AddressMode p_455761_, FilterMode p_455768_, FilterMode p_455239_, boolean p_467394_) {
      return this.samplers[encode(p_455241_, p_455761_, p_455768_, p_455239_, p_467394_)];
   }

   public GpuSampler getClampToEdge(FilterMode p_455304_) {
      return this.getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, p_455304_, p_455304_, false);
   }

   public GpuSampler getClampToEdge(FilterMode p_468789_, boolean p_469996_) {
      return this.getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, p_468789_, p_468789_, p_469996_);
   }

   public GpuSampler getRepeat(FilterMode p_455369_) {
      return this.getSampler(AddressMode.REPEAT, AddressMode.REPEAT, p_455369_, p_455369_, false);
   }

   public GpuSampler getRepeat(FilterMode p_469540_, boolean p_468831_) {
      return this.getSampler(AddressMode.REPEAT, AddressMode.REPEAT, p_469540_, p_469540_, p_468831_);
   }

   public void close() {
      for (GpuSampler gpusampler : this.samplers) {
         gpusampler.close();
      }
   }

   @VisibleForTesting
   static int encode(AddressMode p_456031_, AddressMode p_455665_, FilterMode p_454767_, FilterMode p_455989_, boolean p_470012_) {
      int i = 0;
      i |= p_456031_.ordinal() & 1;
      i |= (p_455665_.ordinal() & 1) << 1;
      i |= (p_454767_.ordinal() & 1) << 2;
      i |= (p_455989_.ordinal() & 1) << 3;
      if (p_470012_) {
         i |= 16;
      }

      return i;
   }
}
