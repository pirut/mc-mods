package com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.DontObfuscate;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public record GpuBufferSlice(GpuBuffer buffer, long offset, long length) {
   public GpuBufferSlice slice(long p_482333_, long p_482309_) {
      if (p_482333_ >= 0L && p_482309_ >= 0L && p_482333_ + p_482309_ <= this.length) {
         return new GpuBufferSlice(this.buffer, this.offset + p_482333_, p_482309_);
      } else {
         throw new IllegalArgumentException(
            "Offset of "
               + p_482333_
               + " and length "
               + p_482309_
               + " would put new slice outside existing slice's range (of "
               + this.offset
               + ","
               + this.length
               + ")"
         );
      }
   }
}
