package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.net.SocketAddress;
import net.minecraft.server.notifications.NotificationService;
import org.jspecify.annotations.Nullable;

public class IpBanList extends StoredUserList<String, IpBanListEntry> {
   public IpBanList(File file, NotificationService notificationService) {
      super(file, notificationService);
   }

   @Override
   protected StoredUserEntry<String> createEntry(JsonObject entryData) {
      return new IpBanListEntry(entryData);
   }

   public boolean isBanned(SocketAddress address) {
      String string = this.getIpFromAddress(address);
      return this.contains(string);
   }

   public boolean isBanned(String address) {
      return this.contains(address);
   }

   @Nullable
   public IpBanListEntry get(SocketAddress address) {
      String string = this.getIpFromAddress(address);
      return this.get(string);
   }

   private String getIpFromAddress(SocketAddress address) {
      String string = address.toString();
      if (string.contains("/")) {
         string = string.substring(string.indexOf(47) + 1);
      }

      if (string.contains(":")) {
         string = string.substring(0, string.indexOf(58));
      }

      return string;
   }

   public boolean add(IpBanListEntry ipBanListEntry) {
      if (super.add(ipBanListEntry)) {
         if (ipBanListEntry.getUser() != null) {
            this.notificationService.ipBanned(ipBanListEntry);
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean remove(String string) {
      if (super.remove(string)) {
         this.notificationService.ipUnbanned(string);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void clear() {
      for (IpBanListEntry ipBanListEntry : this.getEntries()) {
         if (ipBanListEntry.getUser() != null) {
            this.notificationService.ipUnbanned(ipBanListEntry.getUser());
         }
      }

      super.clear();
   }
}
