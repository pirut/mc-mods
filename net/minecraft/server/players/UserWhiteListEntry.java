package net.minecraft.server.players;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.util.UUID;

public class UserWhiteListEntry extends StoredUserEntry<GameProfile> {
   public UserWhiteListEntry(GameProfile p_11462_) {
      super(p_11462_);
   }

   public UserWhiteListEntry(JsonObject p_11460_) {
      super(createGameProfile(p_11460_));
   }

   @Override
   protected void serialize(JsonObject p_11464_) {
      if (this.getUser() != null) {
         p_11464_.addProperty("uuid", this.getUser().getId() == null ? "" : this.getUser().getId().toString());
         p_11464_.addProperty("name", this.getUser().getName());
      }
   }

   private static GameProfile createGameProfile(JsonObject p_11466_) {
      if (p_11466_.has("uuid") && p_11466_.has("name")) {
         String $$1 = p_11466_.get("uuid").getAsString();

         UUID $$2;
         try {
            $$2 = UUID.fromString($$1);
         } catch (Throwable var4) {
            return null;
         }

         return new GameProfile($$2, p_11466_.get("name").getAsString());
      } else {
         return null;
      }
   }
}
