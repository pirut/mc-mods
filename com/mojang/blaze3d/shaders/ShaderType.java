package com.mojang.blaze3d.shaders;

import com.mojang.blaze3d.DontObfuscate;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.internal.NonExhaustiveEnum;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
@NonExhaustiveEnum(
   reason = "Further shader types such as Compute, Task, and Mesh may be added"
)
public enum ShaderType {
   VERTEX("vertex", ".vsh"),
   FRAGMENT("fragment", ".fsh");

   private static final ShaderType[] TYPES = values();
   private final String name;
   private final String extension;

   private ShaderType(String p_410582_, String p_409858_) {
      this.name = p_410582_;
      this.extension = p_409858_;
   }

   @Nullable
   public static ShaderType byLocation(Identifier p_468062_) {
      for (ShaderType shadertype : TYPES) {
         if (p_468062_.getPath().endsWith(shadertype.extension)) {
            return shadertype;
         }
      }

      return null;
   }

   public String getName() {
      return this.name;
   }

   public FileToIdConverter idConverter() {
      return new FileToIdConverter("shaders", this.extension);
   }
}
