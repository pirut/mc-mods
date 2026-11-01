package net.neoforged.neoforge.attachment;

import net.minecraft.core.HolderLookup;
import org.jetbrains.annotations.Nullable;

public interface IAttachmentCopyHandler<T> {
   @Nullable
   T copy(T var1, IAttachmentHolder var2, HolderLookup.Provider var3);
}
