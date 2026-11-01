package net.neoforged.neoforge.registries.holdersets;

import net.minecraft.core.HolderSet;
import net.neoforged.neoforge.common.extensions.IHolderSetExtension;

public interface ICustomHolderSet<T> extends HolderSet<T> {
   HolderSetType type();

   @Override
   default IHolderSetExtension.SerializationType serializationType() {
      return IHolderSetExtension.SerializationType.OBJECT;
   }
}
