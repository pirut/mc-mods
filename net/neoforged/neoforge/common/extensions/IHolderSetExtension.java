package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;

public interface IHolderSetExtension<T> {
   default void addInvalidationListener(Runnable runnable) {
   }

   default IHolderSetExtension.SerializationType serializationType() {
      return this instanceof HolderSet.ListBacked<T> listBacked
         ? (IHolderSetExtension.SerializationType)listBacked.unwrap()
            .map(
               tag -> IHolderSetExtension.SerializationType.STRING,
               list -> list.size() == 1
                  ? (IHolderSetExtension.SerializationType)((Holder)list.get(0))
                     .unwrap()
                     .map(
                        key -> key == null ? IHolderSetExtension.SerializationType.OBJECT : IHolderSetExtension.SerializationType.STRING,
                        value -> IHolderSetExtension.SerializationType.OBJECT
                     )
                  : IHolderSetExtension.SerializationType.LIST
            )
         : IHolderSetExtension.SerializationType.UNKNOWN;
   }

   public static enum SerializationType {
      UNKNOWN,
      STRING,
      LIST,
      OBJECT;
   }
}
