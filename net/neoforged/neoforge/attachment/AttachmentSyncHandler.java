package net.neoforged.neoforge.attachment;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public interface AttachmentSyncHandler<T> {
   default boolean sendToPlayer(IAttachmentHolder holder, ServerPlayer to) {
      return true;
   }

   void write(RegistryFriendlyByteBuf var1, T var2, boolean var3);

   @Nullable
   T read(IAttachmentHolder var1, RegistryFriendlyByteBuf var2, @Nullable T var3);
}
