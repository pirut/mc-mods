package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record ShaderDefines(Map<String, String> values, Set<String> flags) {
   public static final ShaderDefines EMPTY = new ShaderDefines(Map.of(), Set.of());
   public static final Codec<ShaderDefines> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("values", Map.of()).forGetter(ShaderDefines::values),
            Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf).optionalFieldOf("flags", Set.of()).forGetter(ShaderDefines::flags)
         )
         .apply(instance, ShaderDefines::new)
   );

   public static ShaderDefines.Builder builder() {
      return new ShaderDefines.Builder();
   }

   public ShaderDefines withOverrides(ShaderDefines defines) {
      if (this.isEmpty()) {
         return defines;
      } else if (defines.isEmpty()) {
         return this;
      } else {
         com.google.common.collect.ImmutableMap.Builder<String, String> builder = ImmutableMap.builderWithExpectedSize(
            this.values.size() + defines.values.size()
         );
         builder.putAll(this.values);
         builder.putAll(defines.values);
         com.google.common.collect.ImmutableSet.Builder<String> builder2 = ImmutableSet.builderWithExpectedSize(this.flags.size() + defines.flags.size());
         builder2.addAll(this.flags);
         builder2.addAll(defines.flags);
         return new ShaderDefines(builder.buildKeepingLast(), builder2.build());
      }
   }

   public String asSourceDirectives() {
      StringBuilder stringBuilder = new StringBuilder();

      for (Entry<String, String> entry : this.values.entrySet()) {
         String string = entry.getKey();
         String string2 = entry.getValue();
         stringBuilder.append("#define ").append(string).append(" ").append(string2).append('\n');
      }

      for (String string3 : this.flags) {
         stringBuilder.append("#define ").append(string3).append('\n');
      }

      return stringBuilder.toString();
   }

   public boolean isEmpty() {
      return this.values.isEmpty() && this.flags.isEmpty();
   }

   @Environment(EnvType.CLIENT)
   public static class Builder {
      private final com.google.common.collect.ImmutableMap.Builder<String, String> values = ImmutableMap.builder();
      private final com.google.common.collect.ImmutableSet.Builder<String> flags = ImmutableSet.builder();

      Builder() {
      }

      public ShaderDefines.Builder define(String key, String value) {
         if (value.isBlank()) {
            throw new IllegalArgumentException("Cannot define empty string");
         } else {
            this.values.put(key, escapeNewLines(value));
            return this;
         }
      }

      private static String escapeNewLines(String str) {
         return str.replaceAll("\n", "\\\\\n");
      }

      public ShaderDefines.Builder define(String key, float value) {
         this.values.put(key, String.valueOf(value));
         return this;
      }

      public ShaderDefines.Builder define(String key, int value) {
         this.values.put(key, String.valueOf(value));
         return this;
      }

      public ShaderDefines.Builder define(String flag) {
         this.flags.add(flag);
         return this;
      }

      public ShaderDefines build() {
         return new ShaderDefines(this.values.build(), this.flags.build());
      }
   }
}
