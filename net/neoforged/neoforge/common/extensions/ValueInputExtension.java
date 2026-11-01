package net.neoforged.neoforge.common.extensions;

import com.mojang.serialization.MapCodec;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

public interface ValueInputExtension {
   private ValueInput self() {
      return (ValueInput)this;
   }

   default Set<String> keySet() {
      return this.self().<CompoundTag>read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseThrow().keySet();
   }

   default void readChild(String key, ValueIOSerializable object) {
      this.self().child(key).ifPresent(object::deserialize);
   }

   default ValueInput rawChildOrEmpty(String key) {
      return this.self().childOrEmpty(key);
   }
}
