package net.neoforged.neoforge.attachment;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public interface IAttachmentSerializer<S extends Tag, T> {
   T read(IAttachmentHolder var1, S var2, HolderLookup.Provider var3);

   @Nullable
   S write(T var1, HolderLookup.Provider var2);
}
