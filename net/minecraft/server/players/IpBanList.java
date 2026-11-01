package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.net.SocketAddress;
import javax.annotation.Nullable;

public class IpBanList extends StoredUserList<String, IpBanListEntry> {
   public IpBanList(File p_11036_) {
      super(p_11036_);
   }

   @Override
   protected StoredUserEntry<String> createEntry(JsonObject p_11038_) {
      return new IpBanListEntry(p_11038_);
   }

   public boolean isBanned(SocketAddress p_11042_) {
      String $$1 = this.getIpFromAddress(p_11042_);
      return this.contains($$1);
   }

   public boolean isBanned(String p_11040_) {
      return this.contains(p_11040_);
   }

   @Nullable
   public IpBanListEntry get(SocketAddress p_11044_) {
      String $$1 = this.getIpFromAddress(p_11044_);
      return this.get($$1);
   }

   private String getIpFromAddress(SocketAddress p_11046_) {
      String $$1 = p_11046_.toString();
      if ($$1.contains("/")) {
         $$1 = $$1.substring($$1.indexOf(47) + 1);
      }

      if ($$1.contains(":")) {
         $$1 = $$1.substring(0, $$1.indexOf(58));
      }

      return $$1;
   }
}
