package net.neoforged.neoforge.common.util;

import io.netty.buffer.Unpooled;
import java.util.function.Consumer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;

public class FriendlyByteBufUtil {
   private FriendlyByteBufUtil() {
      throw new IllegalStateException("Tried to create utility class!");
   }

   public static byte[] writeCustomData(Consumer<RegistryFriendlyByteBuf> dataWriter, RegistryAccess registryAccess) {
      RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess, ConnectionType.NEOFORGE);

      byte[] var4;
      try {
         dataWriter.accept(buf);
         buf.readerIndex(0);
         byte[] data = new byte[buf.readableBytes()];
         buf.readBytes(data);
         var4 = data;
      } finally {
         buf.release();
      }

      return var4;
   }
}
