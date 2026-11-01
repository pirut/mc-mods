package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ShaderDefines(Map<String, String> values, Set<String> flags) {
   public static final ShaderDefines EMPTY = new ShaderDefines(Map.of(), Set.of());
   public static final Codec<ShaderDefines> CODEC = RecordCodecBuilder.create(
      p_366546_ -> p_366546_.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("values", Map.of()).forGetter(ShaderDefines::values),
            Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf).optionalFieldOf("flags", Set.of()).forGetter(ShaderDefines::flags)
         )
         .apply(p_366546_, ShaderDefines::new)
   );

   public static ShaderDefines.Builder builder() {
      return new ShaderDefines.Builder();
   }

   public ShaderDefines withOverrides(ShaderDefines p_366677_) {
      if (this.isEmpty()) {
         return p_366677_;
      } else if (p_366677_.isEmpty()) {
         return this;
      } else {
         com.google.common.collect.ImmutableMap.Builder<String, String> builder = ImmutableMap.builderWithExpectedSize(
            this.values.size() + p_366677_.values.size()
         );
         builder.putAll(this.values);
         builder.putAll(p_366677_.values);
         com.google.common.collect.ImmutableSet.Builder<String> builder1 = ImmutableSet.builderWithExpectedSize(this.flags.size() + p_366677_.flags.size());
         builder1.addAll(this.flags);
         builder1.addAll(p_366677_.flags);
         return new ShaderDefines(builder.buildKeepingLast(), builder1.build());
      }
   }

   public String asSourceDirectives() {
      StringBuilder stringbuilder = new StringBuilder();

      for (Entry<String, String> entry : this.values.entrySet()) {
         String s = entry.getKey();
         String s1 = entry.getValue();
         stringbuilder.append("#define ").append(s).append(" ").append(s1).append('\n');
      }

      for (String s2 : this.flags) {
         stringbuilder.append("#define ").append(s2).append('\n');
      }

      return stringbuilder.toString();
   }

   public boolean isEmpty() {
      return this.values.isEmpty() && this.flags.isEmpty();
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      private final com.google.common.collect.ImmutableMap.Builder<String, String> values = ImmutableMap.builder();
      private final com.google.common.collect.ImmutableSet.Builder<String> flags = ImmutableSet.builder();

      Builder() {
      }

      public ShaderDefines.Builder define(String p_366680_, String p_366593_) {
         if (p_366593_.isBlank()) {
            throw new IllegalArgumentException("Cannot define empty string");
         } else {
            this.values.put(p_366680_, escapeNewLines(p_366593_));
            return this;
         }
      }

      private static String escapeNewLines(String p_366812_) {
         return p_366812_.replaceAll("\n", "\\\\\n");
      }

      public ShaderDefines.Builder define(String p_366704_, float p_366833_) {
         this.values.put(p_366704_, String.valueOf(p_366833_));
         return this;
      }

      public ShaderDefines.Builder define(String p_404977_, int p_404735_) {
         this.values.put(p_404977_, String.valueOf(p_404735_));
         return this;
      }

      public ShaderDefines.Builder define(String p_366554_) {
         this.flags.add(p_366554_);
         return this;
      }

      public ShaderDefines build() {
         return new ShaderDefines(this.values.build(), this.flags.build());
      }
   }
}
