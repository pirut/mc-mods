package net.neoforged.neoforge.common;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class EffectCure {
   private static final Map<String, EffectCure> CURES = new ConcurrentHashMap<>();
   public static Codec<EffectCure> CODEC = Codec.STRING.xmap(EffectCure::get, EffectCure::name);
   public static final StreamCodec<ByteBuf, EffectCure> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(EffectCure::get, EffectCure::name);
   private final String name;

   public static Collection<EffectCure> getAllCures() {
      return Collections.unmodifiableCollection(CURES.values());
   }

   public static EffectCure get(String name) {
      return CURES.computeIfAbsent(name, EffectCure::new);
   }

   public String name() {
      return this.name;
   }

   @Override
   public String toString() {
      return "EffectCure[" + this.name + "]";
   }

   private EffectCure(String name) {
      this.name = name;
   }
}
