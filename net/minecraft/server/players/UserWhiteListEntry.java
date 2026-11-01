package net.minecraft.server.players;

import com.google.gson.JsonObject;

public class UserWhiteListEntry extends StoredUserEntry<NameAndId> {
   public UserWhiteListEntry(NameAndId p_435511_) {
      super(p_435511_);
   }

   public UserWhiteListEntry(JsonObject p_11460_) {
      super(NameAndId.fromJson(p_11460_));
   }

   @Override
   protected void serialize(JsonObject p_11464_) {
      if (this.getUser() != null) {
         this.getUser().appendTo(p_11464_);
      }
   }
}
