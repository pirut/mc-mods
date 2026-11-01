package net.neoforged.neoforge.common.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.UnknownNullability;

public interface INBTSerializable<T extends Tag> {
   @UnknownNullability
   T serializeNBT(HolderLookup.Provider var1);

   void deserializeNBT(HolderLookup.Provider var1, T var2);
}
