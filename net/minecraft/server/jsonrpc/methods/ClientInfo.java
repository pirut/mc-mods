package net.minecraft.server.jsonrpc.methods;

public record ClientInfo(Integer connectionId) {
   public static ClientInfo of(Integer p_442771_) {
      return new ClientInfo(p_442771_);
   }
}
