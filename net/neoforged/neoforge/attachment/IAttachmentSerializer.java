package net.neoforged.neoforge.attachment;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public interface IAttachmentSerializer<T> {
   T read(IAttachmentHolder var1, ValueInput var2);

   boolean write(T var1, ValueOutput var2);
}
