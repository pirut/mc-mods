package com.mojang.blaze3d.shaders;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.internal.NonExhaustiveEnum;

@OnlyIn(Dist.CLIENT)
@NonExhaustiveEnum(
   reason = "Further uniform types such as SSBO and StorageTexelBuffer may be added"
)
public enum UniformType {
   UNIFORM_BUFFER("ubo"),
   TEXEL_BUFFER("utb");

   final String name;

   private UniformType(String p_410816_) {
      this.name = p_410816_;
   }
}
