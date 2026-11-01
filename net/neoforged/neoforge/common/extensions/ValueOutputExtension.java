package net.neoforged.neoforge.common.extensions;

import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

public interface ValueOutputExtension {
   private ValueOutput self() {
      return (ValueOutput)this;
   }

   default void store(CompoundTag tag) {
      ValueOutput self = this.self();

      for (Entry<String, Tag> entry : tag.entrySet()) {
         self.store(entry.getKey(), ExtraCodecs.NBT, entry.getValue());
      }
   }

   default void putChild(String key, ValueIOSerializable child) {
      child.serialize(this.self().child(key));
   }
}
