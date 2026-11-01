package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.datamaps.IWithData;
import org.jetbrains.annotations.Nullable;

public interface IHolderExtension<T> extends IWithData<T> {
   default Holder<T> getDelegate() {
      return (Holder<T>)this;
   }

   @Nullable
   default HolderLookup.RegistryLookup<T> unwrapLookup() {
      return null;
   }

   @Nullable
   default ResourceKey<T> getKey() {
      return ((Holder)this).unwrapKey().orElse(null);
   }
}
