package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import net.minecraft.core.RegistryAccess;
import net.neoforged.neoforge.network.connection.ConnectionType;

public class RegistryFriendlyByteBuf extends FriendlyByteBuf {
   private final RegistryAccess registryAccess;
   private final ConnectionType connectionType;

   @Deprecated
   public RegistryFriendlyByteBuf(ByteBuf p_320951_, RegistryAccess p_319803_) {
      super(p_320951_);
      this.registryAccess = p_319803_;
      this.connectionType = ConnectionType.OTHER;
   }

   public RegistryFriendlyByteBuf(ByteBuf p_320951_, RegistryAccess p_319803_, ConnectionType connectionType) {
      super(p_320951_);
      this.registryAccess = p_319803_;
      this.connectionType = connectionType;
   }

   public ConnectionType getConnectionType() {
      return this.connectionType;
   }

   public RegistryAccess registryAccess() {
      return this.registryAccess;
   }

   public static Function<ByteBuf, RegistryFriendlyByteBuf> decorator(RegistryAccess p_320166_, ConnectionType connectionType) {
      return p_320793_ -> new RegistryFriendlyByteBuf(p_320793_, p_320166_, connectionType);
   }

   @Deprecated
   public static Function<ByteBuf, RegistryFriendlyByteBuf> decorator(RegistryAccess p_320166_) {
      return p_320793_ -> new RegistryFriendlyByteBuf(p_320793_, p_320166_);
   }
}
